//package com.ztgeo.suqian.controller;
//
//import com.ztgeo.suqian.service.XzService;
//import io.swagger.annotations.ApiOperation;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.stereotype.Controller;
//import org.springframework.util.StringUtils;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestMethod;
//import org.springframework.web.bind.annotation.ResponseBody;
//
//import javax.annotation.Resource;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import java.io.UnsupportedEncodingException;
//import java.net.URLEncoder;
//
//
//@Controller
//public class XzController {
//
//    private Logger log = LoggerFactory.getLogger(this.getClass());
//    @Resource
//    private XzService xzservice;
//    @RequestMapping(value = "/realestate-supervise-exchange/api/v1/other-subject/query",method = RequestMethod.POST, produces = "application/json;charset=utf-8")
//    @ApiOperation(value = "徐州请求省级代理接口", notes = "为徐州政务外网接口提供转发", httpMethod = "POST")
//    @ResponseBody
//    public String forwardCtrl(@RequestBody String param, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws UnsupportedEncodingException {
//        log.info("--------------------开始----进入徐州转发请求过滤器---------------------");
//        String requesturl=httpServletRequest.getRequestURL().toString();
//        String api_id = httpServletRequest.getHeader("api_id");
//        String userid = httpServletRequest.getHeader("from_user");
//        String id = com.ztgeo.suqian.utils.StringUtils.getShortUUID();
//        if(StringUtils.isEmpty(api_id)||StringUtils.isEmpty(userid)){
//            log.info("api_id或者from_user为空!");
//            log.info("--------------------结束----进入徐州转发转发请求过滤器---------------------");
//            httpServletResponse.addHeader("gx_resp_code","20012");
//            httpServletResponse.addHeader("gx_resp_msg", URLEncoder.encode("hear头信息from_user或者api_id没有获取到","UTF-8"));
//            httpServletResponse.addHeader("gx_resp_logid",id);
//            throw new RuntimeException("api_id或者from_user不能为空！");
//        }else{
//            log.info("请求报文：" + param);
//            log.info("api_id：" + api_id);
//            String respStr = "";
//            try {
//                respStr = xzservice.Xzservice(param,api_id,userid,id,requesturl);
//                httpServletResponse.addHeader("gx_resp_code","10000");
//                httpServletResponse.addHeader("gx_resp_msg", URLEncoder.encode("转发成功","UTF-8"));
//                httpServletResponse.addHeader("gx_resp_logid",id);
//            }catch (Exception e){
//                httpServletResponse.addHeader("gx_resp_code","20029");
//                httpServletResponse.addHeader("gx_resp_msg", URLEncoder.encode("徐州请求省级代理接口","UTF-8"));
//                httpServletResponse.addHeader("gx_resp_logid",id);
//            }
//            log.info("响应报文：" + respStr);
//            log.info("--------------------结束进入徐州转发转发请求过滤器---------------------");
//            return respStr;
//        }
//    }
//}
