package com.ztgeo.suqian.filter;

import com.alibaba.fastjson.JSONObject;
import com.nankang.tool.EncrypterAESTool;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.ztgeo.suqian.common.ZtgeoBizZuulException;
import com.ztgeo.suqian.dao.AGShareDao;
import com.ztgeo.suqian.msg.CodeMsg;
import com.ztgeo.suqian.repository.agShare.ApiBaseInfoRepository;
import com.ztgeo.suqian.repository.agShare.ApiNotionalSharedConfigRepository;
import com.ztgeo.suqian.repository.agShare.ApiUserFilterRepository;
import com.ztgeo.suqian.utils.StreamOperateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

/**
 *  房地产平台接口---南康
 */
@Component
public class SuqianNanKangRespFilter extends ZuulFilter {
    private static Logger log = LoggerFactory.getLogger(SuqianNanKangRespFilter.class);

    @Value(value = "${sqnankangkey}")
    private String NanKangKey;

//    @Resource
//    private ApiUserFilterRepository apiUserFilterRepository;
    @Resource
    private AGShareDao agShareDao;
    @Resource
    private ApiBaseInfoRepository apiBaseInfoRepository;
    @Resource
    private ApiNotionalSharedConfigRepository apiNotionalSharedConfigRepository;
    @Autowired
    private StringRedisTemplate redisTemplate;

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

        int useCount = agShareDao.countApiUserFiltersByFilterBcEqualsAndApiIdEquals(className, api_id);
        if (useCount == 0) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public Object run() throws ZuulException {

        try {
            RequestContext requestContext = RequestContext.getCurrentContext();
            HttpServletRequest httpServletRequest = requestContext.getRequest();
            String api_id = httpServletRequest.getHeader("api_id");

            // 获取响应
            InputStream inputStream = requestContext.getResponseDataStream();
            ByteArrayOutputStream byteArrayOutputStream = StreamOperateUtils.cloneInputStreamToByteArray(inputStream);
            String responseBody = StreamUtils.copyToString(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()), StandardCharsets.UTF_8);

          // 响应转json
            // 响应转json
            JSONObject responseBodyJson = JSONObject.parseObject(responseBody);
            log.info("南康响应JSON：" + responseBodyJson);
            // 获取外层JSON
            JSONObject responseBodyJsonByNankang = responseBodyJson.getJSONObject("dataInfo");
            log.info("南康外层：" + responseBodyJsonByNankang);
            // 获取内层JSON
            String responseBodyStrByNankang = responseBodyJsonByNankang.getString("sInfo");
            log.info("南康获取内层JSON：" + responseBodyStrByNankang);

            responseBodyJson.remove("dataInfo");
            log.info("南康删除密文数据后：" + responseBodyJson);

            // 内层JSON解密并赋值
            String responseDataRealByNanKang = EncrypterAESTool.decryptByStr(responseBodyStrByNankang,"nankang");

            // 重新设置
            responseBodyJsonByNankang.fluentPut("sInfo",responseDataRealByNanKang);
            responseBodyJson.put("dataInfo",responseBodyJsonByNankang);

            log.info("南康重置：" + responseBodyJson);
            // 重新配置响应数据
            requestContext.setResponseBody(responseBodyJson.toJSONString());

        } catch (Exception e) {
            throw new ZtgeoBizZuulException(e, CodeMsg.NANKANG_ERROR, "转发南康返回接口异常");
        }
        return null;
    }
}
