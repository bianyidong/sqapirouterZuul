package com.ztgeo.suqian.service;

import com.alibaba.fastjson.JSONObject;
import com.ztgeo.suqian.entity.ag_datashare.ApiBaseInfo;
import com.ztgeo.suqian.repository.ApiBaseInfoRepository;
import com.ztgeo.suqian.utils.HttpUtilsAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service(value = "ShenService")
public class ShengService {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    private ApiBaseInfoRepository apiBaseInfoRepository;
    @Autowired
    private StringRedisTemplate redisTemplate;


    public String forwardservice(String param,String api_id){

        /**
         *  参数定义
         */
        // 实际返回数据
        String respBodyRealStr = null;

        try {
            // 转发地址
            String forwardPath = null;

            ApiBaseInfo apiBaseInfo = apiBaseInfoRepository.findApiBaseInfosByApiIdEquals(api_id).get(0);

            forwardPath = apiBaseInfo.getBaseUrl() + apiBaseInfo.getPath();
            log.info("根据api_id：" + api_id + "获取到的转发地址：" + forwardPath);

            // 请求TOKEN
            // 判断redis中是否有数据
            String redisKey = "token:" + api_id;
            String token = "Bearer " + getProviceToken(redisKey);
            String secret = "5a9a0db0-387a-4daa-9aac-4ed9f70a50cb@3b91c09c2c254e3981466132faf8360d";
            Map<String,String> headerMap = new HashMap<>();

            headerMap.put("Secret",secret);
            headerMap.put("Authorization",token);
            log.info("组织请求头信息：" + headerMap);

            respBodyRealStr = HttpUtilsAll.post(forwardPath,headerMap,param).body();
            log.info("转发省厅接口响应报文：" + respBodyRealStr);

        } catch (IOException e) {
            log.info("转发省厅接口异常！",e);
            throw new RuntimeException("转发省厅接口异常");
        }
        return respBodyRealStr;
    }

    // token获取与配置
    private synchronized String getProviceToken(String configKey) {
        try {
            boolean totalIsHasKey = redisTemplate.hasKey(configKey);

            // 不存在
            if (!totalIsHasKey) {
                log.info("redis中不存在TOKEN信息，需要重新获取！" );

                String token = null;
                String tokenUrl = "https://2.211.38.98:8343/v1/apigw/oauth2/token";

                Map<String,String> map = new HashMap<>();
                map.put("grant_type","client_credentials");
                map.put("client_id","8947f32223bf4174bc7a014a96666ffc");
                map.put("client_secret","2805d50ef71246d3a394e078ba4a68fc");
                map.put("scope","default");

                token = HttpUtilsAll.post(tokenUrl,map).body();
                JSONObject tokenJson = JSONObject.parseObject(token);
                String accessToken = tokenJson.getString("access_token");

                redisTemplate.opsForValue().set(configKey, accessToken);
                redisTemplate.expire(configKey, 3333, TimeUnit.SECONDS);
                log.info("获取新TOKEN：" + accessToken + "差设置到redis中，redis过期时间为3333秒");

                return accessToken;
            }else{
                // 存在
                log.info("redis中存在TOKEN信息，直接读取！" );
                String accessToken = redisTemplate.opsForValue().get(configKey);
                return accessToken;
            }
        } catch (IOException e) {
            log.info("从redis中获取token异常！",e);
            throw new RuntimeException("调用getProviceToken方法异常，从redis中获取token异常");
        }
    }
}
