package com.ztgeo.suqian.service;

import com.alibaba.fastjson.JSONObject;
import com.ztgeo.suqian.entity.ag_datashare.ApiBaseInfo;
import com.ztgeo.suqian.repository.agShare.ApiBaseInfoRepository;
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

@Service(value = "AshxService")
public class AshxService {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    private ApiBaseInfoRepository apiBaseInfoRepository;

    public String wardservice(String param, String api_id) {

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
            respBodyRealStr = HttpUtilsAll.post(forwardPath, param).body();
            log.info("转发接口响应报文：" + respBodyRealStr);

        } catch (IOException e) {
            log.info("转发接口异常！", e);
            throw new RuntimeException("转发接口异常");
        }
        return respBodyRealStr;

    }
}
