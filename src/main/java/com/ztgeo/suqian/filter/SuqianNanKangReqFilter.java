package com.ztgeo.suqian.filter;

import com.alibaba.fastjson.JSONObject;
import com.nankang.tool.EncrypterAESTool;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.netflix.zuul.http.ServletInputStreamWrapper;
import com.ztgeo.suqian.common.ZtgeoBizZuulException;
import com.ztgeo.suqian.dao.AGShareDao;
import com.ztgeo.suqian.msg.CodeMsg;
import com.ztgeo.suqian.repository.agShare.ApiBaseInfoRepository;
import com.ztgeo.suqian.repository.agShare.ApiNotionalSharedConfigRepository;
import com.ztgeo.suqian.repository.agShare.ApiUserFilterRepository;
import io.micrometer.core.instrument.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

/**
 *  房地产平台接口---南康
 */
@Component
public class SuqianNanKangReqFilter extends ZuulFilter {

    private static Logger log = LoggerFactory.getLogger(SuqianNanKangReqFilter.class);

    @Value(value = "${sqnankangkey}")
    private String NanKangKey;

    @Resource
    private AGShareDao agShareDao;
    @Resource
    private ApiNotionalSharedConfigRepository apiNotionalSharedConfigRepository;
    @Resource
    private ApiBaseInfoRepository apiBaseInfoRepository;
    @Autowired
    private StringRedisTemplate redisTemplate;

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
        RequestContext requestContext = RequestContext.getCurrentContext();
        HttpServletRequest httpServletRequest = requestContext.getRequest();
        String api_id = httpServletRequest.getHeader("api_id");

        int useCount = agShareDao.countApiUserFiltersByFilterBcEqualsAndApiIdEquals(className, api_id);

        if (useCount == 0) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public Object run() throws ZuulException {

        try {
            RequestContext requestContext = RequestContext.getCurrentContext();
            HttpServletRequest httpServletRequest = requestContext.getRequest();
            String api_id = httpServletRequest.getHeader("api_id");

            // 获取请求参数
            InputStream inReq = httpServletRequest.getInputStream();
            String requestBody = IOUtils.toString(inReq,Charset.forName("UTF-8"));
            log.info("南康请求JSON：" + requestBody);

            // 使用南康加密
            String requestBodyByNankang = EncrypterAESTool.encryptByStr(requestBody,NanKangKey);
            log.info("南康加参数加密后：" + requestBodyByNankang);

            JSONObject contryReqJson = new JSONObject();
            contryReqJson.put("edata",requestBodyByNankang);
            log.info("转发南康JSON字符串：" + contryReqJson);


            // 重新配置请求体
            // 将JSON设置到请求体中，并设置请求方式为POST
            String newbody = contryReqJson.toJSONString();
            // BODY体设置
            final byte[] reqBodyBytes = newbody.getBytes("UTF-8");
            requestContext.setRequest(new HttpServletRequestWrapper(httpServletRequest) {

                @Override
                public ServletInputStream getInputStream() throws IOException {
                    return new ServletInputStreamWrapper(reqBodyBytes);
                }

                @Override
                public int getContentLength() {
                    return reqBodyBytes.length;
                }

                @Override
                public long getContentLengthLong() {
                    return reqBodyBytes.length;
                }

            });

        } catch (Exception e) {
            throw new ZtgeoBizZuulException(e, CodeMsg.NANKANG_ERROR, "转发南康接口异常");
        }


        return null;
    }

    // 序号获取与配置
    private synchronized int getXuHao(String configKey) {
        boolean totalIsHasKey = redisTemplate.hasKey(configKey);
        if (!totalIsHasKey) {
            redisTemplate.opsForValue().set(configKey, "1");
            redisTemplate.expire(configKey, 2, TimeUnit.DAYS);
            return 1;
        } else {
            int xuhao = Integer.valueOf(redisTemplate.opsForValue().get(configKey)) + 1;
            redisTemplate.opsForValue().set(configKey, String.valueOf(xuhao));
            return xuhao;
        }
    }
}
