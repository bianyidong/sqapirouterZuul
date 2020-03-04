package com.ztgeo.suqian.service;

import com.alibaba.fastjson.JSONObject;
import com.isoftstone.sign.SignGeneration;
import com.ztgeo.suqian.dao.AGLogDao;
import com.ztgeo.suqian.dao.AGShareDao;
import com.ztgeo.suqian.entity.ag_datashare.ApiBaseInfo;
import com.ztgeo.suqian.entity.ag_datashare.ApiCitySharedConfig;
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
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service(value = "XzService")
public class XzService {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Resource
    private AGLogDao agLogDao;

    @Resource
    private AGShareDao agShareDao;

    @Value(value = "${xu.xzqdm}")
    private String xzqdm;
    @Value(value = "${xu.ip}")
    private String ip;
    @Value(value = "${xu.deptName}")
    private String deptName;
    @Value(value = "${xu.userName}")
    private String userName;

    public String Xzservice(String param,String api_id,String userid,String id,String requesturl) {
        String result = "";
        try {
            ApiBaseInfo apiBaseInfo = agShareDao.findApiBaseInfosByApiIdEquals(api_id).get(0);

            String url = apiBaseInfo.getBaseUrl() + apiBaseInfo.getPath();
            log.info("根据api_id：" + api_id + "获取到的转发地址：" + url);
            String redisKey = "token:" + api_id;
            String currentDays = new SimpleDateFormat("yyyyMMdd").format(new Date());

            JSONObject setResqJson =JSONObject.parseObject(param);
            JSONObject getHeadJson = setResqJson.getJSONObject("head");
            if(StringUtils.isEmpty(getHeadJson.get("cxqqdh"))){
                String configKey = currentDays + ":" + xzqdm;
                int xuHao = getXuHao(configKey);
                String cxqqdh = currentDays + xzqdm + String.format("%06d", xuHao);
                getHeadJson.put("cxqqdh",cxqqdh);
            }
            getHeadJson.put("xzqdm",xzqdm);
            getHeadJson.put("token", getProviceToken(redisKey));
            getHeadJson.put("deptName",deptName);
            getHeadJson.put("userName",userName);
            getHeadJson.put("ip",ip);
            Map<String, String> map = new HashMap<String, String>();
            map.put("gxData", setResqJson.toJSONString());
            log.info("组织好的请求报文"+map);
            result = HttpClientUtil.httpPostRequest(url, map);
            LocalDateTime localTime = LocalDateTime.now();
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            DateTimeFormatter dateTimeFormatterYmd = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String currentTime = dateTimeFormatter.format(localTime);
            String currentymd = dateTimeFormatterYmd.format(localTime);
            ApiAccessRecord apiAccessRecord = new ApiAccessRecord();
            apiAccessRecord.setId(id);
            apiAccessRecord.setApiId(api_id);
            apiAccessRecord.setFromUser(userid);
            apiAccessRecord.setUserName(userName);
            apiAccessRecord.setApiName(apiBaseInfo.getApiName());
            apiAccessRecord.setApiUrl(apiBaseInfo.getBaseUrl() + apiBaseInfo.getPath());
            //apiAccessRecord.setFilterUser(UserFilter);//使用的过滤器名称
            apiAccessRecord.setType("post");
            apiAccessRecord.setAccessClientIp(requesturl);
            apiAccessRecord.setUri(requesturl);
            apiAccessRecord.setYearMonthDay(currentymd);
            apiAccessRecord.setAccessTime(currentTime);
            apiAccessRecord.setRequestData(param);
            apiAccessRecord.setResponseData(result);
            apiAccessRecord.setApiOwnerId(apiBaseInfo.getApiOwnerId());
            apiAccessRecord.setStatus("0");
            agLogDao.saveApiAccessRecord(apiAccessRecord);
            log.info("记录日志完成");
      }
        catch (IOException e) {
            log.info("省级数据共享交换平台接口异常", e);
            throw new RuntimeException("省级数据共享交换平台接口异常");
        }
        catch (Exception e) {
            log.info("省级数据共享交换平台接口异常", e);
            throw new RuntimeException("省级数据共享交换平台接口异常");
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
