package com.ztgeo.suqian.filter.Post_type;


import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.ztgeo.suqian.common.GlobalConstants;
import com.ztgeo.suqian.common.ZtgeoBizZuulException;
import com.ztgeo.suqian.dao.AGLogDao;
import com.ztgeo.suqian.msg.CodeMsg;
import org.apache.commons.lang.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
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
    @Resource
    private AGLogDao agLogDao;

    @Value("${customAttributes.httpName}")
    private String httpName; // 存储用户发送数据的数据库名

    @Override
    public String filterType() {
        return FilterConstants.POST_TYPE;
    }

    @Override
    public int filterOrder() {
        return 98;
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
            String requserID = ctx.getRequest().getHeader("from_user");
            HttpServletResponse response = ctx.getResponse();
            if (StringUtils.isEmpty(requserID)) {
                String ctxFromUser = ctx.get("from_user").toString();
                if (StringUtils.isEmpty(ctxFromUser)) {
                    throw new ZtgeoBizZuulException(CodeMsg.GETNULL_ERROR);
                } else {
                    userID = ctxFromUser;
                    ctx.addZuulResponseHeader("Content-Type", "text/xml");
                }
            } else {
                userID = requserID;
            }
            //获取记录主键ID(来自routing过滤器保存的上下文)
            Object recordID = ctx.get(GlobalConstants.RECORD_PRIMARY_KEY);
            Object accessClientIp = ctx.get(GlobalConstants.ACCESS_IP_KEY);
           Object issuccess= ctx.get(GlobalConstants.ISSUCCESS);
            if (Objects.equals(null, accessClientIp) || Objects.equals(null, recordID))
                throw new ZtgeoBizZuulException(CodeMsg.FAIL, "访问者IP或记录ID未获取到");
            String rspBody = ctx.getResponseBody();
            log.info("接收到返回的数据{}", rspBody);
            int statuscode = ctx.getResponseStatusCode();
            if (issuccess==null){
                if (statuscode == 200) {
                    agLogDao.updateResponsedateById(rspBody, "0", recordID.toString());
                    response.addHeader("gx_resp_code", "10000");
                    response.addHeader("gx_resp_logid", recordID.toString());
                    response.addHeader("gx_resp_msg", URLEncoder.encode("转发成功", "UTF-8"));
                } else {
                    agLogDao.updateResponsedateById(rspBody, "1", recordID.toString());
                }
            }
            else if (issuccess.equals("success")){
                agLogDao.updateResponsedateById(rspBody, "0", recordID.toString());
                response.addHeader("gx_resp_code", "10000");
                response.addHeader("gx_resp_logid", recordID.toString());
                response.addHeader("gx_resp_msg", URLEncoder.encode("转发成功", "UTF-8"));
            }else if (issuccess.equals("false")){
                response.addHeader("gx_resp_code", "30012");
                response.addHeader("gx_resp_logid", recordID.toString());
                response.addHeader("gx_resp_msg", URLEncoder.encode("各级接口转发请求过滤器异常", "UTF-8"));
            }
            else {

            }
            log.info("记录完成");
            return null;
        } catch (Exception s) {
            log.info("20027-post日志过滤器内部异常", s);
            throw new RuntimeException("20027-post日志过滤器内部异常");
        }
    }
}
