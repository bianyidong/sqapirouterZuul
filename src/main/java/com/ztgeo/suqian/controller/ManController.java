package com.ztgeo.suqian.controller;

import com.isoftstone.sign.SignGeneration;
import com.ztgeo.suqian.service.RxdbService;
import com.ztgeo.suqian.service.ShengService;
import com.ztgeo.suqian.utils.HttpClientUtil;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
public class ManController {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    private RxdbService rxdbService;

    @RequestMapping(value = "/garx",method = RequestMethod.POST, produces = "application/json;charset=utf-8")
    @ApiOperation(value = "转发人像对比接口", notes = "为宿迁各程序调用国土资源政务外网接口提供转发", httpMethod = "POST")
    @ResponseBody
    public String forwardCtrl(@RequestBody String param, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        log.info("--------------------开始----人像对比接口---------------------");
        String api_id = httpServletRequest.getHeader("api_id");
        if(StringUtils.isEmpty(api_id)){
            log.info("api_id为空!");
            log.info("--------------------结束----人像对比接口---------------------");
            throw new RuntimeException("api_id不能为空！");
        }else{
            log.info("请求报文：" + param);
            log.info("api_id：" + api_id);
            String respStr = rxdbService.rxdbservice(param,api_id);
            log.info("响应报文：" + respStr);
            log.info("--------------------人像对比接口---------------------");
            return respStr;
        }

    }
}
