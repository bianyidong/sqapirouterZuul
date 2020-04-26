package com.ztgeo.suqian.filter.Post_type;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.ztgeo.suqian.common.GlobalConstants;
import com.ztgeo.suqian.common.ZtgeoBizZuulException;
import com.ztgeo.suqian.msg.CodeMsg;
import com.ztgeo.suqian.utils.StreamOperateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * 响应过滤器
 *
 * @author bianyidong
 * @version 2019-7-19
 */
@Component
public class ResponseFilter extends ZuulFilter {

    private static Logger log = LoggerFactory.getLogger(ResponseFilter.class);
    private String api_id;
//    @Resource
//    private ApiUserFilterRepository apiUserFilterRepository;

    @Override
    public String filterType() {
        return FilterConstants.POST_TYPE;
    }

    @Override
    public int filterOrder() {
        return 97;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() throws ZuulException {
        log.info("=================进入post通用过滤器,接收返回的数据=====================");
        InputStream inputStream = null;
        InputStream inputStreamOld = null;
        InputStream inputStreamNew = null;
        try {
            RequestContext ctx = RequestContext.getCurrentContext();
            inputStream = ctx.getResponseDataStream();
            String rspBody = ctx.getResponseBody();
            //获取记录主键ID(来自routing过滤器保存的上下文)
            //Object recordID = ctx.get(GlobalConstants.RECORD_PRIMARY_KEY);
//            if (Objects.equals(null, recordID)) {
//                throw new ZtgeoBizZuulException(CodeMsg.FAIL, "post通用过滤器访问者IP或记录ID未获取到");
//            }
//            String ContentType=ctx.getRequest().getContentType();
//            if ("text/xml".equals(ContentType)) {
//                log.info("请求为text/xml，返回日志不操作");
//                return null;
//            }
            if (!Objects.equals(null, inputStream) && Objects.equals(null, rspBody)) {
               // String body = StreamUtils.copyToString(inputStream, Charset.forName("UTF-8"));
//                ctx.setResponseDataStream(new ByteArrayInputStream(body.getBytes()));
                // 获取返回的body
                ByteArrayOutputStream byteArrayOutputStream = StreamOperateUtils.cloneInputStreamToByteArray(inputStream);
                inputStreamOld = new ByteArrayInputStream(byteArrayOutputStream.toByteArray()); // 原始流
                //inputStreamNew = new ByteArrayInputStream(byteArrayOutputStream.toByteArray()); // 复制流
                inputStreamNew = inputStreamOld;
                // 获取返回的body字符串
                String responseBody = StreamUtils.copyToString(inputStreamOld, StandardCharsets.UTF_8);
                log.info("post通用过滤器返回数据{}", responseBody);
                if (Objects.equals(null, responseBody)) {
                    throw new ZtgeoBizZuulException(CodeMsg.FAIL, "post通用过滤器响应报文未获取到");
                }
                ctx.setResponseBody(responseBody);
                log.info("post通用过滤器入库完成");
                ctx.setResponseDataStream(inputStreamNew);
            } else if (!Objects.equals(null, rspBody)) {
                ctx.setResponseBody(rspBody);
                log.info("post通用过滤器入库完成{}", rspBody);
            } else {
                //log.info("未接收到返回的任何数据,记录ID:{}", recordID);
                log.info("未接收到返回的任何数据,记录ID:{}", "0000");

            }

            return null;
        } catch (Exception s) {
            log.info("10006-响应通用过滤器内部异常", s);
            throw new RuntimeException("10006-"+s.getMessage());
        } finally {
            ResponseSafeToSignFilter.getFindlly(inputStream, inputStreamOld, inputStreamNew);
        }
    }
}
