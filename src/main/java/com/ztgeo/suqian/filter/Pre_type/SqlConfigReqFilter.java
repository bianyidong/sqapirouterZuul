package com.ztgeo.suqian.filter.Pre_type;

import com.alibaba.fastjson.JSONObject;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.ztgeo.suqian.common.GlobalConstants;
import com.ztgeo.suqian.dao.AGShareDao;
import com.ztgeo.suqian.entity.ag_datashare.ApiBaseInfo;
import com.ztgeo.suqian.entity.ag_datashare.ApiSqlConfigInfo;
import com.ztgeo.suqian.utils.HttpUtilsAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class SqlConfigReqFilter extends ZuulFilter {

    private Logger log = LoggerFactory.getLogger(this.getClass());
    private String api_id;
    @Resource
    private AGShareDao agShareDao;


    @Override
    public String filterType() {
        return FilterConstants.PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return 0;
    }

    @Override
    public boolean shouldFilter() {
        String className = this.getClass().getSimpleName();
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();
        api_id = request.getHeader("api_id");
        int count = agShareDao.countApiUserFiltersByFilterBcEqualsAndApiIdEquals(className, api_id);
        if (count > 0) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Object run() throws ZuulException {

        log.info("-------------开始---进入Sql配置接口过滤器-------------");
        RequestContext requestContext = RequestContext.getCurrentContext();
        try {
            HttpServletRequest request = requestContext.getRequest();
            InputStream in = request.getInputStream();
            String requestBody = StreamUtils.copyToString(in, Charset.forName("UTF-8"));
            String api_id = request.getHeader("api_id");
            ApiSqlConfigInfo apiSqlConfigInfo = agShareDao.findApiSqlConfigInfosByApiId(api_id).get(0);
            String result="测试";
            requestContext.setResponseBody(result);
            requestContext.setSendZuulResponse(false);
        } catch (Exception e) {
            log.info("SQL配置请求过滤器异常", e);
            log.info("-------------结束---Sql配置接口-------------");
            throw new RuntimeException("30018-SQL配置过滤器异常");
        }
        return null;

    }

}
