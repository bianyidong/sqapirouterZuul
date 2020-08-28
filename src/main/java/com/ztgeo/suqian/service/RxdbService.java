package com.ztgeo.suqian.service;

import com.alibaba.fastjson.JSONObject;
import com.isoftstone.sign.SignGeneration;
import com.ztgeo.suqian.dao.AGLogDao;
import com.ztgeo.suqian.entity.ag_datashare.ApiBaseInfo;
import com.ztgeo.suqian.entity.ag_datashare.ApiCitySharedConfig;
import com.ztgeo.suqian.entity.ag_datashare.BaseUser;
import com.ztgeo.suqian.entity.ag_log.ApiAccessRecord;
import com.ztgeo.suqian.repository.agShare.ApiBaseInfoRepository;
import com.ztgeo.suqian.repository.agShare.ApiCitySharedConfigRepository;
import com.ztgeo.suqian.repository.agShare.BaseUserRepository;
import com.ztgeo.suqian.utils.HttpClientUtil;
import com.ztgeo.suqian.utils.HttpUtilsAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service(value = "RxdbService")
public class RxdbService {

    private Logger log = LoggerFactory.getLogger(this.getClass());
    @Value(value = "${sqtoke.sqjdtokenurl}")
    private String sqjdtokenurl;
    @Value(value = "${sqtoke.sqjdtokenurl}")
    private String client_credentials;
    @Value(value = "${sqtoke.client_id}")
    private String client_id;
    @Value(value = "${sqtoke.client_secret}")
    private String client_secret;
    @Value(value = "${sqtoke.client_secret}")
    private String scope;
    @Resource
    private ApiCitySharedConfigRepository apiCitySharedConfigRepository;
    @Resource
    private ApiBaseInfoRepository apiBaseInfoRepository;
    @Resource
    private BaseUserRepository baseUserRepository;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Resource
    private AGLogDao agLogDao;

    private String UserFilter = "";

    public String rxdbservice(String param, String api_id) {
        String result = "";
        try {
            ApiCitySharedConfig apiCitySharedConfig = apiCitySharedConfigRepository.findApiCitySharedConfigsByApiIdEquals(api_id);
            String sk = apiCitySharedConfig.getSk();
            ApiBaseInfo apiBaseInfo = apiBaseInfoRepository.findApiBaseInfosByApiIdEquals(api_id).get(0);

            String url = apiBaseInfo.getBaseUrl() + apiBaseInfo.getPath();
            log.info("根据api_id：" + api_id + "获取到的转发地址：" + url);
            String sign = "";

            String redisKey = "token:" + api_id;

            Map<String, String> map = new HashMap<String, String>();

            map.put("serviceId", apiCitySharedConfig.getServiceId());
            map.put("ak", apiCitySharedConfig.getAk());
            map.put("appId", apiCitySharedConfig.getAppId());

            map.put("header_Content-Type", "application/json;charset=utf-8");

            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            map.put("timestamp", formatter.format(new Date()));

            map.put("method", "POST");
            map.put("header_bmd", "2.193.64.2");
            map.put("header_Secret", "5a9a0db0-387a-4daa-9aac-4ed9f70a50cb@3b91c09c2c254e3981466132faf8360d");
            map.put("header_Authorization", "Bearer " + getProviceToken(redisKey));
            map.put("request_body", param);
            sign = SignGeneration.generationSign(map, sk);
            map.put("sign", sign);
            result = HttpClientUtil.httpPostRequest(url, map);
        } catch (IOException e) {
            log.info("转发人像对比接口异常！", e);
            throw new RuntimeException("转发人像对比接口异常");
        } catch (Exception e) {
            log.info("转发人像对比接口异常！", e);
            throw new RuntimeException("转发人像对比接口异常");
        }
        return result;
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
                map.put("grant_type", client_credentials);
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
