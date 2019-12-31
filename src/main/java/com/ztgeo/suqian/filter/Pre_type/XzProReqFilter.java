package com.ztgeo.suqian.filter.Pre_type;

import com.alibaba.fastjson.JSONObject;
import com.isoftstone.sign.SignGeneration;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.netflix.zuul.http.ServletInputStreamWrapper;
import com.ztgeo.suqian.entity.ag_datashare.ApiBaseInfo;
import com.ztgeo.suqian.entity.ag_datashare.ApiNotionalSharedConfig;
import com.ztgeo.suqian.repository.agShare.ApiBaseInfoRepository;
import com.ztgeo.suqian.repository.agShare.ApiNotionalSharedConfigRepository;
import com.ztgeo.suqian.repository.agShare.ApiUserFilterRepository;
import com.ztgeo.suqian.utils.HttpClientUtil;
import com.ztgeo.suqian.utils.HttpUtilsAll;
import com.ztgeo.suqian.utils.RSAUtils;
import io.micrometer.core.instrument.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
public class XzProReqFilter extends ZuulFilter {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    private ApiUserFilterRepository apiUserFilterRepository;
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
        int useCount = apiUserFilterRepository.countApiUserFiltersByFilterBcEqualsAndApiIdEquals(className, api_id);

        if (useCount == 0) {
            return false;
        } else {
            return false;
        }
    }

    @Override
    public Object run() throws ZuulException {

        log.info("-------------开始---进入徐州转发转发请求过滤器-------------");
        RequestContext requestContext = RequestContext.getCurrentContext();
        try {
            HttpServletRequest httpServletRequest = requestContext.getRequest();
            log.info("请求为formdata");
            Map<String,String> toBeJiamiMap = new HashMap<>();
            Map<String,String[]> requestMap = httpServletRequest.getParameterMap();
            System.out.println("ce"+requestMap);
            for(Map.Entry<String, String[]> entry : requestMap.entrySet()){
                String mapKey = entry.getKey();
                String mapValue = StringArray2String(entry.getValue());
                toBeJiamiMap.put(mapKey,mapValue);
            }
            log.info("待加密map<toBeJiamiMap>：" + toBeJiamiMap);


            // 一定要get一下,下面这行代码才能取到值... [注1]
            // httpServletRequest.getParameterMap();
            Map<String, List<String>> requestQueryParams = requestContext.getRequestQueryParams();

            if (requestQueryParams==null) {
                requestQueryParams=new HashMap<>();
            }

            for(Map.Entry<String, String> entry : toBeJiamiMap.entrySet()){
                requestQueryParams.put(entry.getKey(),String2List(entry.getValue()));
            }

            log.info("待转发map<requestQueryParams>：" + requestQueryParams);
//
//         //String head= String.valueOf(requestMap.get("gxData"));
//            //String gxData=  httpServletRequest.getParameter("gxData");
//            String api_id = httpServletRequest.getHeader("api_id");
//            Map<String,String[]> requestMap = httpServletRequest.getParameterMap();
//            Map<String,String> toBeJiamiMap = new HashMap<>();
//            String redisKey = "token:" + api_id;
//            //JSONObject setTokenJson=JSONObject.parseObject(gxData);
//            //toBeJiamiMap.put("token",getProviceToken(redisKey));
//            for(Map.Entry<String, String[]> entry : requestMap.entrySet()){
//                String mapKey = entry.getKey();
//                String mapValue = StringArray2String(entry.getValue());
//                JSONObject jsonObject=JSONObject.parseObject(mapValue);
//                JSONObject headJson=jsonObject.getJSONObject("head");
//                headJson.put("token",getProviceToken(redisKey));
//                String jsonString=jsonObject.toJSONString();
//                toBeJiamiMap.put(mapKey,jsonString);
//            }
//            log.info("待转发map<toBeJiamiMap>：" + toBeJiamiMap);
//            // 一定要get一下,下面这行代码才能取到值... [注1]
//            // httpServletRequest.getParameterMap();
//            Map<String, List<String>> requestQueryParams = requestContext.getRequestQueryParams();
//
//            if (requestQueryParams==null) {
//                requestQueryParams=new HashMap<>();
//            }
//
//            for(Map.Entry<String, String> entry : toBeJiamiMap.entrySet()){
//                requestQueryParams.put(entry.getKey(),String2List(entry.getValue()));
//            }
//
//            log.info("已加签，待转发map<requestQueryParams>：" + requestQueryParams);
            //requestContext.setRequestQueryParams(requestQueryParams);
        }catch (Exception e) {

            log.info("111", e);
            log.info("-------------结束---111-------------");
            throw new RuntimeException("30012-11111");
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
                String tokenUrl = "http://10.0.0.6:8090/realestate-supervise-exchange/api/v1/bdc/token";
                JSONObject tokenHeardJson = new JSONObject();

                tokenHeardJson.put("xzqdm", "320300");
                JSONObject dataJson = new JSONObject();
                dataJson.put("username", "gx320300");
                dataJson.put("password", "6f715e67548d147c17cd408fe4201cc1");
                JSONObject tokenJson = new JSONObject();
                tokenJson.put("head", tokenHeardJson);
                tokenJson.put("data",dataJson);
                Map<String, String> map = new HashMap<>();
                map.put("gxData",tokenJson.toJSONString());

                token = HttpUtilsAll.post(tokenUrl, map).body();
                JSONObject tokenResponseJson = JSONObject.parseObject(token);
                JSONObject accessData = tokenResponseJson.getJSONObject("data");
                String accessToken=accessData.getString("token");

                redisTemplate.opsForValue().set(configKey, accessToken);
                redisTemplate.expire(configKey, 1600, TimeUnit.SECONDS);
                log.info("获取新TOKEN：" + accessToken + "差设置到redis中，redis过期时间为1600秒");

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

    private String StringArray2String(String[] strs){

        StringBuffer str = new StringBuffer();
        for (String s : strs) {
            str.append(s);
        }

        return str.toString();
    }

    private List<String> String2List(String str){
        List<String> listTmp = new ArrayList<>();
        listTmp.add(str);
        return listTmp;
    }
}
