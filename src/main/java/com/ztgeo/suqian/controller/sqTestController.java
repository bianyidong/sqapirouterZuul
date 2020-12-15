package com.ztgeo.suqian.controller;

import com.ztgeo.suqian.service.ShengService;
import com.ztgeo.suqian.service.TestsqService;
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

@Controller
public class sqTestController {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    private TestsqService testsqService;

    @RequestMapping(value = "/test/sq",method = RequestMethod.POST, produces = "application/json;charset=utf-8")
    @ApiOperation(value = "测试省厅接口", notes = "接口提供转发", httpMethod = "POST")
    @ResponseBody
    public String forwardCtrl(){
        log.info("--------------------开始----调用转发省厅接口---------------------");


            String respStr = testsqService.testSq();
            log.info("响应报文：" + respStr);
            log.info("--------------------结束----调用转发省厅接口---------------------");
            return respStr;

    }
}
