package com.ztgeo.suqian.controller;

import com.ztgeo.suqian.service.ShengService;
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
public class ShengController {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    private ShengService shengService;

    @RequestMapping(value = "/forwardprovincial",method = RequestMethod.POST, produces = "application/json;charset=utf-8")
    @ApiOperation(value = "转发省厅接口", notes = "为宿迁各程序调用国土资源政务外网接口提供转发", httpMethod = "POST")
    @ResponseBody
    public String forwardCtrl(@RequestBody String param, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse){
        log.info("--------------------开始----调用转发省厅接口---------------------");
        String api_id = httpServletRequest.getHeader("api_id");
        if(StringUtils.isEmpty(api_id)){
            log.info("api_id为空!");
            log.info("--------------------结束----调用转发省厅接口---------------------");
            throw new RuntimeException("api_id不能为空！");
        }else{
            log.info("请求报文：" + param);
            log.info("api_id：" + api_id);
            String respStr = shengService.forwardservice(param,api_id);
            log.info("响应报文：" + respStr);
            log.info("--------------------结束----调用转发省厅接口---------------------");
            return respStr;
        }


//        String token = null;
//        try {
//            String tokenUrl = "https://2.211.38.98:8343/v1/apigw/oauth2/token";
//
//            Map<String,String> map = new HashMap<>();
//            map.put("grant_type","client_credentials");
//            map.put("client_id","8947f32223bf4174bc7a014a96666ffc");
//            map.put("client_secret","2805d50ef71246d3a394e078ba4a68fc");
//            map.put("scope","default");
//
//            token = HttpUtilsAll.post(tokenUrl,map).body();
//
//            log.info("toke:" + token);
//        } catch (Exception e) {
//            log.info("异常了",e);
//        }
//        return token;
    }
}
