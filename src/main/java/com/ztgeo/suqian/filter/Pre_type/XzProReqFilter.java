package com.ztgeo.suqian.filter.Pre_type;

import com.alibaba.fastjson.JSONObject;
import com.isoftstone.sign.SignGeneration;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.netflix.zuul.http.ServletInputStreamWrapper;
import com.ztgeo.suqian.common.GlobalConstants;
import com.ztgeo.suqian.dao.AGShareDao;
import com.ztgeo.suqian.entity.ag_datashare.ApiBaseInfo;
import com.ztgeo.suqian.entity.ag_datashare.ApiNotionalConfig;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
public class XzProReqFilter extends ZuulFilter {

    private Logger log = LoggerFactory.getLogger(this.getClass());
    private String api_id;
    @Resource
    private AGShareDao agShareDao;
    @Autowired
    private StringRedisTemplate redisTemplate;

        @Value(value = "${xu.xzqdm}")
    private String xzqdm;
//    @Value(value = "${xu.ip}")
//    private String ip;

    @Value(value = "${xu.username}")
    private String username;
    @Value(value = "${xu.password}")
    private String password;
    @Value(value = "${sttokenUrl}")
    private String getTokenUrl;
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

        log.info("-------------开始---进入徐州转发请求过滤器-------------");
        RequestContext requestContext = RequestContext.getCurrentContext();
        try {
            HttpServletRequest request = requestContext.getRequest();
            InputStream in = request.getInputStream();
            String requestBody = StreamUtils.copyToString(in, Charset.forName("UTF-8"));
            String api_id = request.getHeader("api_id");
            ApiBaseInfo apiBaseInfo = agShareDao.findApiBaseInfosByApiIdEquals(api_id).get(0);
            String url = apiBaseInfo.getBaseUrl() + apiBaseInfo.getPath();
            log.info("根据api_id：" + api_id + "获取到的转发地址：" + url);
            String redisKey = "token:" + api_id;
            String currentDays = new SimpleDateFormat("yyyyMMdd").format(new Date());
            ApiNotionalConfig apiNotionalConfig = agShareDao.findApiNotionalSharedConfigsByapiIdEquals(api_id);
            String xzqdm = apiNotionalConfig.getQxdm();
            String deptName = apiNotionalConfig.getDeptName();
            String userName = apiNotionalConfig.getUserName();
            String ip = apiNotionalConfig.getIp();
            JSONObject setResqJson = JSONObject.parseObject(requestBody);
            JSONObject getHeadJson = setResqJson.getJSONObject("head");
            if (StringUtils.isEmpty(getHeadJson.get("cxqqdh"))) {
                String configKey = currentDays + ":" + xzqdm;
                int xuHao = getXuHao(configKey);
                String cxqqdh = currentDays + xzqdm + String.format("%06d", xuHao);
                getHeadJson.put("cxqqdh", cxqqdh);
            }
            getHeadJson.put("xzqdm", xzqdm);
            getHeadJson.put("token", getProviceToken(redisKey));
            getHeadJson.put("deptName", deptName);
            getHeadJson.put("userName", userName);
            getHeadJson.put("ip", ip);
            Map<String, String> map = new HashMap<String, String>();
            Map<String, String> paramMap=new HashMap<String, String>();
            Map<String, File> fileMap=new HashMap<>();
            map.put("gxData", setResqJson.toJSONString());
            log.info("组织好的请求报文" + map);
            String result = null;
//            result = HttpClientUtil.httpPostRequest(url, map);
            result = HttpUtilsAll.post(url,null, map, null).body();
            if (!StringUtils.isEmpty(result)) {
                requestContext.set(GlobalConstants.ISSUCCESS, "success");
            } else {
                requestContext.set(GlobalConstants.ISSUCCESS, "false");
            }
            requestContext.setResponseBody(result);
            requestContext.setSendZuulResponse(false);
        } catch (Exception e) {
            log.info("各级接口转发请求过滤器异常", e);
            log.info("-------------结束---各级接口转发请求过滤器异常-------------");
            throw new RuntimeException("30012-各级接口转发请求过滤器异常");
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
                String tokenUrl =getTokenUrl;
                JSONObject tokenHeardJson = new JSONObject();

                tokenHeardJson.put("xzqdm", xzqdm);
                JSONObject dataJson = new JSONObject();
                dataJson.put("username", username);
                dataJson.put("password", password);
                JSONObject tokenJson = new JSONObject();
                tokenJson.put("head", tokenHeardJson);
                tokenJson.put("data", dataJson);
                Map<String, String> map = new HashMap<>();
                map.put("gxData", tokenJson.toJSONString());

                token = HttpUtilsAll.post(tokenUrl, map).body();
                JSONObject tokenResponseJson = JSONObject.parseObject(token);
                JSONObject accessData = tokenResponseJson.getJSONObject("data");
                String accessToken = accessData.getString("token");
                //因为token接口设置的半个小时过期，redis缓存过期时间少于半个小时
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
