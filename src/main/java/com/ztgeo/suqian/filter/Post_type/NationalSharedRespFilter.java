package com.ztgeo.suqian.filter.Post_type;

import com.alibaba.fastjson.JSONObject;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.ztgeo.suqian.common.ZtgeoBizZuulException;
import com.ztgeo.suqian.entity.ag_datashare.*;
import com.ztgeo.suqian.msg.CodeMsg;
import com.ztgeo.suqian.repository.agShare.ApiBaseInfoRepository;
import com.ztgeo.suqian.repository.agShare.ApiNotionalSharedConfigRepository;
import com.ztgeo.suqian.repository.agShare.ApiUserFilterRepository;
import com.ztgeo.suqian.utils.RSAUtils;
import com.ztgeo.suqian.utils.StreamOperateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component//
public class NationalSharedRespFilter extends ZuulFilter {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    private ApiUserFilterRepository apiUserFilterRepository;
    @Resource
    private ApiBaseInfoRepository apiBaseInfoRepository;
    @Resource
    private ApiNotionalSharedConfigRepository apiNotionalSharedConfigRepository;

    @Override
    public String filterType() {
        return FilterConstants.POST_TYPE;
    }

    @Override
    public int filterOrder() {
        return -99;
    }

    @Override
    public boolean shouldFilter() {
        String className = this.getClass().getSimpleName();
        RequestContext requestContext = RequestContext.getCurrentContext();
        HttpServletRequest httpServletRequest = requestContext.getRequest();
        String api_id = httpServletRequest.getHeader("api_id");

        ApiBaseInfo apiBaseInfo = apiBaseInfoRepository.queryApiBaseInfoByApiId(api_id);
        String apiOwnerid = apiBaseInfo.getApiOwnerId();

        int useCount = apiUserFilterRepository.countApiUserFiltersByFilterBcEqualsAndApiIdEquals(className,api_id);
        int configCount = apiNotionalSharedConfigRepository.countApiNotionalSharedConfigsByUseridEquals(apiOwnerid);

        if(useCount == 0){
            return false;
        }else {
            if(configCount == 0){
                return false;
            }else {
                return true;
            }
        }
    }

    @Override
    public Object run() throws ZuulException {
        log.info("-------------开始---进入国家级接口转发响应过滤器-------------");

        try {
            RequestContext requestContext = RequestContext.getCurrentContext();
            HttpServletRequest httpServletRequest = requestContext.getRequest();

            String api_id = httpServletRequest.getHeader("api_id");

            ApiBaseInfo apiBaseInfo = apiBaseInfoRepository.queryApiBaseInfoByApiId(api_id);
            String apiOwnerid = apiBaseInfo.getApiOwnerId();

            ApiNotionalSharedConfig apiNotionalSharedConfig = apiNotionalSharedConfigRepository.findById(apiOwnerid).get();


            InputStream inputStream = requestContext.getResponseDataStream();
            ByteArrayOutputStream byteArrayOutputStream = StreamOperateUtils.cloneInputStreamToByteArray(inputStream);
            String responseBody = StreamUtils.copyToString(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()), StandardCharsets.UTF_8);

            JSONObject responseBodyJson = JSONObject.parseObject(responseBody);
            log.info("转发国家级接口响应报文：" + responseBodyJson);

            // 解密
            String dataRespStr = responseBodyJson.getString("data");
            String decodeRespStr = null;
            if(!StringUtils.isEmpty(dataRespStr)){
                log.info("DATA字段不为空，可以进行解密");
                decodeRespStr = RSAUtils.decodeByPublic(dataRespStr,apiNotionalSharedConfig.getToken());
                log.info("解密后DATA字段信息：" + decodeRespStr);
            }else{
                log.info("DATA字段为空，无法进行解密");
            }

            responseBodyJson.put("data",decodeRespStr);

            log.info("转发国家级接口过滤器响应报文：" + responseBodyJson);
            requestContext.setResponseBody(responseBodyJson.toJSONString());

        } catch (Exception e) {
            log.info("转发国家级共享接口响应过滤器异常",e);
            log.info("-------------结束---进入国家级接口转发响应过滤器-------------");
            throw new RuntimeException("30013-转发国家级共享接口响应过滤器异常");
        }
        log.info("-------------结束---进入国家级接口转发响应过滤器-------------");
        return null;
    }
}
