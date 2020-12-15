package com.ztgeo.suqian.filter.Pre_type;

import com.isoftstone.sign.SignGeneration;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.ztgeo.suqian.common.ZtgeoBizZuulException;
import com.ztgeo.suqian.dao.AGShareDao;
import com.ztgeo.suqian.entity.ag_datashare.ApiCitySharedConfig;
import com.ztgeo.suqian.msg.CodeMsg;
import com.ztgeo.suqian.repository.agShare.ApiCitySharedConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 *  宿迁市各部门接口
 */
@Component
public class CitySharedReqFilter extends ZuulFilter {

    private static Logger log = LoggerFactory.getLogger(CitySharedReqFilter.class);

    @Resource
    private AGShareDao agShareDao;
    @Resource
    private ApiCitySharedConfigRepository apiCitySharedConfigRepository;
    @Autowired
    private StringRedisTemplate redisTemplate;

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
        RequestContext requestContext = RequestContext.getCurrentContext();
        HttpServletRequest httpServletRequest = requestContext.getRequest();
        String api_id = httpServletRequest.getHeader("api_id");

        int useCount = agShareDao.countApiUserFiltersByFilterBcEqualsAndApiIdEquals(className,api_id);
        int configCount = apiCitySharedConfigRepository.countApiCitySharedConfigsByApiIdEquals(api_id);
        if(useCount == 0){
            return false;
        }else {
            if(configCount == 0){
                return false;
            }else{
                return true;
            }
        }
    }

    @Override
    public Object run() throws ZuulException {
        log.info("--------------进入京东云市本级接口过滤器------------------");
        try {
            RequestContext requestContext = RequestContext.getCurrentContext();
            HttpServletRequest httpServletRequest = requestContext.getRequest();

            String api_id = httpServletRequest.getHeader("api_id");
            // 获取配置信息
            ApiCitySharedConfig apiCitySharedConfig = apiCitySharedConfigRepository.findApiCitySharedConfigsByApiIdEquals(api_id);
            String sk = apiCitySharedConfig.getSk();
//
//            String contentType = httpServletRequest.getContentType();
//            if(contentType.contains("json")){
//                log.info("请求为JSON");
//                // 获取请求参数
//                InputStream inReq = httpServletRequest.getInputStream();
//                String requestBody = IOUtils.toString(inReq,Charset.forName("UTF-8"));
//                log.info("原始请求报文：" + requestBody);
//
//                // 配置公共部分
//                JSONObject requestBodyRealJson = new JSONObject();
//                requestBodyRealJson.put("serviceId",apiCitySharedConfig.getServiceId());
//                requestBodyRealJson.put("ak",apiCitySharedConfig.getAk());
//                requestBodyRealJson.put("appId",apiCitySharedConfig.getAppId());
//                requestBodyRealJson.put("timestamp",new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
//
//                // 处理请求参数，循环KEY后写入
//                Map<String,Object> requestBodyMap = (Map) JSON.parse(requestBody);
//                for (Map.Entry<String, Object> entry : requestBodyMap.entrySet()) {
//                    requestBodyRealJson.put(entry.getKey(),entry.getValue());
//                }
//
//                // 请求加签处理，使用sk
//                Map<String,Object> requestBodyRealMap = (Map) JSON.parse(requestBodyRealJson.toJSONString());
//                String sign = SignGeneration.generationSign(requestBodyRealMap,sk);
//                requestBodyRealJson.put("sign",sign);
//                log.info("已加签，待转发：" + requestBodyRealJson);
//
//                // 重新配置请求体
//                // 将JSON设置到请求体中，并设置请求方式为POST
//                // BODY体设置
//                final byte[] reqBodyBytes = requestBodyRealJson.toJSONString().getBytes();
//                requestContext.setRequest(new HttpServletRequestWrapper(httpServletRequest) {
//
//                    @Override
//                    public ServletInputStream getInputStream() throws IOException {
//                        return new ServletInputStreamWrapper(reqBodyBytes);
//                    }
//
//                    @Override
//                    public int getContentLength() {
//                        return reqBodyBytes.length;
//                    }
//
//                    @Override
//                    public long getContentLengthLong() {
//                        return reqBodyBytes.length;
//                    }
//
//                });
//            }else{
            log.info("请求为formdata");
            Map<String,String> toBeJiamiMap = new HashMap<>();

            Map<String,String[]> requestMap = httpServletRequest.getParameterMap();

            toBeJiamiMap.put("serviceId",apiCitySharedConfig.getServiceId());
            toBeJiamiMap.put("ak",apiCitySharedConfig.getAk());
            toBeJiamiMap.put("appId",apiCitySharedConfig.getAppId());
            toBeJiamiMap.put("timestamp",new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));

            for(Map.Entry<String, String[]> entry : requestMap.entrySet()){
                String mapKey = entry.getKey();
                String mapValue = StringArray2String(entry.getValue());
                toBeJiamiMap.put(mapKey,mapValue);
            }
            log.info("待加密map<toBeJiamiMap>：" + toBeJiamiMap);

            // 请求加签处理，使用sk
            String sign = SignGeneration.generationSign(toBeJiamiMap,sk);
            toBeJiamiMap.put("sign",sign);
            log.info("加密值<sign>：" + sign);

            // 一定要get一下,下面这行代码才能取到值... [注1]
            // httpServletRequest.getParameterMap();
            Map<String, List<String>> requestQueryParams = requestContext.getRequestQueryParams();

            if (requestQueryParams==null) {
                requestQueryParams=new HashMap<>();
            }

            for(Map.Entry<String, String> entry : toBeJiamiMap.entrySet()){
                requestQueryParams.put(entry.getKey(),String2List(entry.getValue()));
            }

            log.info("已加签，待转发map<requestQueryParams>：" + requestQueryParams);

        } catch (Exception e) {
            log.info("30015-转发市级共享接口异常",e);
            throw new RuntimeException("30015-转发市级共享接口异常");
        }
        return null;
    }


    private String StringArray2String(String[] strs){

        StringBuffer str = new StringBuffer();
        for (String s : strs) {
            str.append(s);
        }

        return str.toString();
    }

    private List<String> String2List(String str){
        List<String> listTmp = new ArrayList<>();
        listTmp.add(str);
        return listTmp;
    }
}
