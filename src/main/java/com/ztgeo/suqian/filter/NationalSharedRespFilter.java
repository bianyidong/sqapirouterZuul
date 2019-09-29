package com.ztgeo.suqian.filter;

import com.alibaba.fastjson.JSONObject;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.ztgeo.suqian.common.ZtgeoBizZuulException;
import com.ztgeo.suqian.entity.ag_datashare.ApiBaseInfo;
import com.ztgeo.suqian.entity.ag_datashare.ApiNotionalSharedConfig;
import com.ztgeo.suqian.msg.CodeMsg;
import com.ztgeo.suqian.repository.ApiBaseInfoRepository;
import com.ztgeo.suqian.repository.ApiNotionalSharedConfigRepository;
import com.ztgeo.suqian.repository.ApiUserFilterRepository;
import com.ztgeo.suqian.utils.RSAUtils;
import com.ztgeo.suqian.utils.StreamOperateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

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
            String decodeRespStr = RSAUtils.decodeByPublic(dataRespStr,apiNotionalSharedConfig.getToken());
            log.info("param解密后：" + decodeRespStr);

            responseBodyJson.put("data",decodeRespStr);

            log.info("转发国家级接口过滤器响应报文：" + responseBodyJson);
            requestContext.setResponseBody(responseBodyJson.toJSONString());

        } catch (Exception e) {
            log.info("转发国家级共享接口响应过滤器异常",e);
            throw new ZtgeoBizZuulException(e, CodeMsg.NATIONALSHARED_RESP_ERROR, "转发国家级共享接口响应过滤器异常");
        }

        return null;
    }
}
