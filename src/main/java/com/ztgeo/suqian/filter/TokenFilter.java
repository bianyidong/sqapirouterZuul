package com.ztgeo.suqian.filter;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.ztgeo.suqian.repository.agShare.ApiUserFilterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;

@Component
public class TokenFilter extends ZuulFilter {

    @Resource
    private ApiUserFilterRepository apiUserFilterRepository;
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
        int count = apiUserFilterRepository.countApiUserFiltersByFilterBcEqualsAndApiIdEquals(className,api_id);

        if(count == 0){
            return false;
        }else {
            return true;
        }
    }

    @Override
    public Object run() throws ZuulException {

        try {
            RequestContext requestContext = RequestContext.getCurrentContext();
            HttpServletRequest httpServletRequest = requestContext.getRequest();
            String api_id = httpServletRequest.getHeader("api_id");


//            // 重新配置请求体
//            // 将JSON设置到请求体中，并设置请求方式为POST
//            String newbody = contryReqJson.toJSONString();
//            // BODY体设置
//            final byte[] reqBodyBytes = newbody.getBytes();
//            requestContext.setRequest(new HttpServletRequestWrapper(httpServletRequest) {
//
//                @Override
//                public String getMethod() {
//                    return "POST";
//                }
//
//                @Override
//                public ServletInputStream getInputStream() throws IOException {
//                    return new ServletInputStreamWrapper(reqBodyBytes);
//                }
//
//                @Override
//                public int getContentLength() {
//                    return reqBodyBytes.length;
//                }
//
//                @Override
//                public long getContentLengthLong() {
//                    return reqBodyBytes.length;
//                }
//
//            });




        } catch (Exception e) {
            e.printStackTrace();
        }


        return null;
    }

    // 序号获取与配置
    private synchronized int getToken(String configKey) {
        boolean totalIsHasKey = redisTemplate.hasKey(configKey);
        if (!totalIsHasKey) {
            System.out.println("未发现接口总访问量配置KEY，新建");
            redisTemplate.opsForValue().set(configKey, "1");
            redisTemplate.expire(configKey, 45, TimeUnit.MINUTES);
            return 1;
        }else{
            int xuhao = Integer.valueOf(redisTemplate.opsForValue().get(configKey)) + 1;
            redisTemplate.opsForValue().set(configKey,String.valueOf(xuhao));
            return xuhao;
        }
    }

}
