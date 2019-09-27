package com.ztgeo.suqian.filter;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.ztgeo.suqian.common.GlobalConstants;
import com.ztgeo.suqian.common.ZtgeoBizZuulException;
import com.ztgeo.suqian.entity.HttpEntity;
import com.ztgeo.suqian.msg.CodeMsg;
import org.apache.commons.lang.StringUtils;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.stereotype.Component;
import java.util.Objects;


/**
 * 响应记录日志过滤器
 *
 * @author bianyidong
 * @version 2019-6-26
 */
@Component
public class ResponseReceiveBodyFilter extends ZuulFilter {

    private static Logger log = LoggerFactory.getLogger(ResponseReceiveBodyFilter.class);
    @Autowired
    private MongoClient mongoClient;
    @Value("${customAttributes.httpName}")
    private String httpName; // 存储用户发送数据的数据库名

    @Override
    public String filterType() {
        return FilterConstants.POST_TYPE;
    }

    @Override
    public int filterOrder() {
        return 5;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() throws ZuulException {
        log.info("=================进入响应记录日志过滤器,接收返回的数据=====================");
        try {
            RequestContext ctx = RequestContext.getCurrentContext();
            String userID;
            String requserID= ctx.getRequest().getHeader("from_user");

            if(StringUtils.isEmpty(requserID)){
                String ctxFromUser = ctx.get("from_user").toString();
                if(StringUtils.isEmpty(ctxFromUser)){
                    throw new ZtgeoBizZuulException(CodeMsg.GETNULL_ERROR);
                }else{
                    userID = ctxFromUser;
                    ctx.addZuulResponseHeader("Content-Type","text/xml");
                }
            }else{
                userID = requserID;
            }
            String rspBody = ctx.getResponseBody();

//            // 对<>进行转义
//            rspBody = rspBody.replaceAll("<", "&lt;");
//            rspBody = rspBody.replaceAll(">", "&gt;");


            log.info("接收到返回的数据{}", rspBody);
            //获取记录主键ID(来自routing过滤器保存的上下文)
            Object recordID = ctx.get(GlobalConstants.RECORD_PRIMARY_KEY);
            Object accessClientIp = ctx.get(GlobalConstants.ACCESS_IP_KEY);
            if (Objects.equals(null, accessClientIp) || Objects.equals(null, recordID))
                throw new ZtgeoBizZuulException(CodeMsg.FAIL, "访问者IP或记录ID未获取到");
            CodecRegistry pojoCodecRegistry = CodecRegistries.fromRegistries(MongoClient.getDefaultCodecRegistry(),
                    CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build()));
            MongoDatabase mongoDB = mongoClient.getDatabase(httpName).withCodecRegistry(pojoCodecRegistry);
            MongoCollection<HttpEntity> collection = mongoDB.getCollection(userID + "_record", HttpEntity.class);
            BasicDBObject searchDoc = new BasicDBObject().append("iD", recordID);
            BasicDBObject newDoc = new BasicDBObject("$set",
                    new BasicDBObject().append("receiveBody", rspBody));
            collection.findOneAndUpdate(searchDoc, newDoc, new FindOneAndUpdateOptions().upsert(true));
            log.info("接收到返回的数据{}", rspBody);
            log.info("记录完成");
            return null;
        } catch (ZuulException z) {
            throw new ZtgeoBizZuulException(z, "post日志过滤器异常", z.nStatusCode, z.errorCause);
        } catch (Exception s) {
            throw new ZtgeoBizZuulException(s, CodeMsg.FAIL, "post日志过滤器内部异常");
        }
    }
}
