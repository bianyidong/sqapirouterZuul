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
import org.springframework.beans.factory.annotation.Value;
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
    @Value(value = "${sqtoke.header_Secret}")
    private String header_Secret;
    @Value(value = "${sqtoke.header_bmd}")
    private String header_bmd;


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
            toBeJiamiMap.put("header_Secret", header_Secret);
            toBeJiamiMap.put("header_Authorization", token);
            toBeJiamiMap.put("header_bmd",header_bmd);
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
            throw new RuntimeException("30015-转发市级共享接口异常");
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
