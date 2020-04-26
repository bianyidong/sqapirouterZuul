package com.ztgeo.suqian.filter.Pre_type;

import com.alibaba.fastjson.JSONObject;
import com.isoftstone.sign.SignGeneration;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.ztgeo.suqian.dao.AGShareDao;
import com.ztgeo.suqian.entity.ag_datashare.ApiCitySharedConfig;
import com.ztgeo.suqian.repository.agShare.ApiCitySharedConfigRepository;
import com.ztgeo.suqian.utils.HttpUtilsAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 宿迁市各部门接口
 */
@Component
public class CitySharedNotionalReqFilter extends ZuulFilter {

    private static Logger log = LoggerFactory.getLogger(CitySharedNotionalReqFilter.class);

    @Resource
    private AGShareDao agShareDao;
    @Resource
    private ApiCitySharedConfigRepository apiCitySharedConfigRepository;
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
        int configCount = apiCitySharedConfigRepository.countApiCitySharedConfigsByApiIdEquals(api_id);
        if (useCount == 0) {
            return false;
        } else {
            if (configCount == 0) {
                return false;
            } else {
                return true;
            }
        }
    }

    @Override
    public Object run() throws ZuulException {
        log.info("--------------进入京东云市级代理省级接口过滤器------------------");
        try {
            RequestContext requestContext = RequestContext.getCurrentContext();
            HttpServletRequest httpServletRequest = requestContext.getRequest();

            String api_id = httpServletRequest.getHeader("api_id");
            // 获取配置信息
            ApiCitySharedConfig apiCitySharedConfig = apiCitySharedConfigRepository.findApiCitySharedConfigsByApiIdEquals(api_id);
            String sk = apiCitySharedConfig.getSk();
            String redisKey = "token:" + api_id;
            String token = "Bearer " + getProviceToken(redisKey);
            log.info("请求为formdata");
            Map<String, String> toBeJiamiMap = new HashMap<>();

            Map<String, String[]> requestMap = httpServletRequest.getParameterMap();

            toBeJiamiMap.put("serviceId", apiCitySharedConfig.getServiceId());
            toBeJiamiMap.put("ak", apiCitySharedConfig.getAk());
            toBeJiamiMap.put("appId", apiCitySharedConfig.getAppId());
            toBeJiamiMap.put("timestamp", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
            toBeJiamiMap.put("header_Secret", "5a9a0db0-387a-4daa-9aac-4ed9f70a50cb@3b91c09c2c254e3981466132faf8360d");
            toBeJiamiMap.put("header_Authorization", token);
            toBeJiamiMap.put("header_bmd","2.193.64.2");
            System.out.println("token"+token);
            toBeJiamiMap.put("method", "POST");
            for (Map.Entry<String, String[]> entry : requestMap.entrySet()) {
                String mapKey = entry.getKey();
                String mapValue = StringArray2String(entry.getValue());
                toBeJiamiMap.put(mapKey, mapValue);
            }
            log.info("待加密map<toBeJiamiMap>：" + toBeJiamiMap);

            // 请求加签处理，使用sk
            String sign = SignGeneration.generationSign(toBeJiamiMap, sk);
            toBeJiamiMap.put("sign", sign);
            log.info("加密值<sign>：" + sign);

            // 一定要get一下,下面这行代码才能取到值... [注1]
            // httpServletRequest.getParameterMap();
            Map<String, List<String>> requestQueryParams = requestContext.getRequestQueryParams();

            if (requestQueryParams == null) {
                requestQueryParams = new HashMap<>();
            }

            for (Map.Entry<String, String> entry : toBeJiamiMap.entrySet()) {
                requestQueryParams.put(entry.getKey(), String2List(entry.getValue()));
            }

            log.info("已加签，待转发map<requestQueryParams>：" + requestQueryParams);

        } catch (Exception e) {
            log.info("30015-转发市级共享接口异常", e);
            throw new RuntimeException("30015-"+e.getMessage()+"转发市级共享接口异常");
        }
        return null;
    }


    private String StringArray2String(String[] strs) {

        StringBuffer str = new StringBuffer();
        for (String s : strs) {
            str.append(s);
        }

        return str.toString();
    }

    private List<String> String2List(String str) {
        List<String> listTmp = new ArrayList<>();
        listTmp.add(str);
        return listTmp;
    }

    // token获取与配置
    private synchronized String getProviceToken(String configKey) {
        try {
            boolean totalIsHasKey = redisTemplate.hasKey(configKey);

            // 不存在
            if (!totalIsHasKey) {
                log.info("redis中不存在TOKEN信息，需要重新获取！");

                String token = null;
                String tokenUrl = "https://2.211.38.98:8343/v1/apigw/oauth2/token";

                Map<String, String> map = new HashMap<>();
                map.put("grant_type", "client_credentials");
                map.put("client_id", "8947f32223bf4174bc7a014a96666ffc");
                map.put("client_secret", "2805d50ef71246d3a394e078ba4a68fc");
                map.put("scope", "default");

                token = HttpUtilsAll.post(tokenUrl, map).body();
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
