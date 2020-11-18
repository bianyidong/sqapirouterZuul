package com.ztgeo.suqian.filter.Pre_type;

import com.alibaba.fastjson.JSONObject;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.netflix.zuul.http.ServletInputStreamWrapper;
import com.ztgeo.suqian.common.ZtgeoBizZuulException;
import com.ztgeo.suqian.msg.CodeMsg;
import com.ztgeo.suqian.repository.agShare.ApiBaseInfoRepository;
import com.ztgeo.suqian.repository.agShare.ApiNotionalSharedConfigRepository;
import com.ztgeo.suqian.repository.agShare.ApiUserFilterRepository;
import com.ztgeo.suqian.utils.HttpOperation;
import com.ztgeo.suqian.utils.HttpUtilsAll;
import io.micrometer.core.instrument.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import sun.rmi.runtime.Log;

import javax.annotation.Resource;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 *  国土资源部接口---政务外网（省厅大数据中心转发的接口，需要TOKEN验证dddddd
 */
@Component
public class ProvinceSharedReqFilter extends ZuulFilter {
    private Logger log = LoggerFactory.getLogger(this.getClass());
    @Resource
    private ApiUserFilterRepository apiUserFilterRepository;
    @Resource
    private ApiNotionalSharedConfigRepository apiNotionalSharedConfigRepository;
    @Resource
    private ApiBaseInfoRepository apiBaseInfoRepository;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Value(value = "${sqtoke.sqjdtokenurl}")
    private String sqjdtokenurl;
    @Value(value = "${sqtoke.granttype}")
    private String granttype;
    @Value(value = "${sqtoke.client_id}")
    private String client_id;
    @Value(value = "${sqtoke.client_secret}")
    private String client_secret;
    @Value(value = "${sqtoke.scope}")
    private String scope;
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
        int useCount = apiUserFilterRepository.countApiUserFiltersByFilterBcEqualsAndApiIdEquals(className,api_id);
        if(useCount == 0){
            return false;
        }else {
                return true;
        }
    }
    @Override
    public Object run() throws ZuulException {

        try {
            RequestContext requestContext = RequestContext.getCurrentContext();
            HttpServletRequest httpServletRequest = requestContext.getRequest();
            String api_id = httpServletRequest.getHeader("api_id");

            // 判断redis中是否有数据
            String redisKey = "token:" + api_id;
            String token = "Bearer " + getProviceToken(redisKey);

            requestContext.addZuulRequestHeader("Secret","5a9a0db0-387a-4daa-9aac-4ed9f70a50cb@3b91c09c2c254e3981466132faf8360d");
            requestContext.addZuulRequestHeader("Authorization",token);

            InputStream inReq = httpServletRequest.getInputStream();
            String requestBody = IOUtils.toString(inReq,Charset.forName("UTF-8"));

            // 重新配置请求体
            // 将JSON设置到请求体中，并设置请求方式为POST
            // BODY体设置
            final byte[] reqBodyBytes = requestBody.getBytes();
            requestContext.setRequest(new HttpServletRequestWrapper(httpServletRequest) {

                @Override
                public String getMethod() {
                    return "POST";
                }

                @Override
                public String getContentType() {
                    return "application/json;charset=utf-8";
                }

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
            log.info("30014-转发省级共享接口异常");
            throw new RuntimeException("30014-转发省级共享接口异常");
        }


        return null;
    }

    // token获取与配置
    private synchronized String getProviceToken(String configKey) {
        try {
            boolean totalIsHasKey = redisTemplate.hasKey(configKey);

            // 不存在
            if (!totalIsHasKey) {
                log.info("redis中不存在TOKEN信息，需要重新获取！");

                String token = null;
                String tokenUrl = sqjdtokenurl;

                Map<String, String> map = new HashMap<>();
                map.put("grant_type", granttype);
                map.put("client_id", client_id);
                map.put("client_secret",client_secret);
                map.put("scope", scope);

                token = HttpUtilsAll.post(tokenUrl, map).body();
                log.info("请求省厅返回报文；"+token);
                JSONObject tokenJson = JSONObject.parseObject(token);
                String accessToken = tokenJson.getString("access_token");

                redisTemplate.opsForValue().set(configKey, accessToken);
                redisTemplate.expire(configKey, 3333, TimeUnit.SECONDS);
                log.info("获取新TOKEN：" + accessToken + "差设置到redis中，redis过期时间为3333秒");

                return accessToken;
            } else {
                // 存在
                log.info("redis中存在TOKEN信息，直接读取！");
                String accessToken = redisTemplate.opsForValue().get(configKey);
                return accessToken;
            }
        } catch (IOException e) {
            log.info("从redis中获取token异常！", e);
            throw new RuntimeException("调用getProviceToken方法异常，从redis中获取token异常");
        }
    }
}
