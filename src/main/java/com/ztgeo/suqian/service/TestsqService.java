package com.ztgeo.suqian.service;

import com.alibaba.fastjson.JSONObject;
import com.isoftstone.sign.SignGeneration;
import com.ztgeo.suqian.utils.HttpClientUtil;
import com.ztgeo.suqian.utils.HttpUtilsAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service(value = "TestsqService")
public class TestsqService {
    private  Logger log = LoggerFactory.getLogger(this.getClass());
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
    public String testSq(){
        String sign="";
        String token = "Bearer " + getProviceToken();
        String url="http://172.22.137.43:8080/irsp/openApi/rkkjzxxcx/v2";
        String result = "";
        Map map=new HashMap();
        map.put("serviceId", "8080809175f86c020175f980df935623");
        map.put("ak", "2dea46502d814a3dbd4f458b6dc7444f");
        map.put("appId", "3857633054894E43A10BCBB24036311D");
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        map.put("timestamp",formatter.format(new Date()));
        map.put("method", "POST");
        //这里的xxxxxxxx为access token（访问令牌），需要调用另一个接口获取，注意Bearer后面有一个空格，然后才是访问令牌的值
        map.put("header_Authorization", token);
        map.put("request_body","{\"datalist\":[{\"sfzh\":\"32083019940712023x\",\"xm\":\"高晗\"}]}");
        try {
            sign= SignGeneration.generationSign(map,"25b7dd7ead5b420b");
            map.put("sign", sign);
            log.info("请求省厅报文"+map);
            result = HttpClientUtil.httpPostRequest(url,  map);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
    // token获取与配置
    private synchronized String getProviceToken() {
        try {

                log.info("需要重新获取！");

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
                return accessToken;
        } catch (IOException e) {
            log.info("从redis中获取token异常！", e);
            throw new RuntimeException("调用getProviceToken方法异常，从redis中获取token异常");
        }
    }
}
