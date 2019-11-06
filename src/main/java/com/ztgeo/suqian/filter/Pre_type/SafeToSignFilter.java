package com.ztgeo.suqian.filter.Pre_type;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.ztgeo.suqian.common.CryptographyOperation;
import com.ztgeo.suqian.common.GlobalConstants;
import com.ztgeo.suqian.common.ZtgeoBizZuulException;
import com.ztgeo.suqian.entity.ag_datashare.ApiBaseInfo;
import com.ztgeo.suqian.msg.CodeMsg;
import com.ztgeo.suqian.repository.agShare.ApiBaseInfoRepository;
import com.ztgeo.suqian.repository.agShare.ApiUserFilterRepository;
import com.ztgeo.suqian.utils.HttpUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import java.io.InputStream;
import java.nio.charset.Charset;

import static com.ztgeo.suqian.filter.Pre_type.AddRequestBodyFilter.getObject;


/**
 * 用于转发时共享平台重新加签
 */
@Component
public class SafeToSignFilter extends ZuulFilter {

    private static Logger log = LoggerFactory.getLogger(SafeToSignFilter.class);
    private String api_id;
    private String Sign_pt_secret_key;
//    @Resource
//    private UserKeyInfoRepository userKeyInfoRepository;
//    @Autowired
//    private RedisOperator redis;
    @Resource
    private ApiBaseInfoRepository apiBaseInfoRepository;
    @Resource
    private ApiUserFilterRepository apiUserFilterRepository;


    @Override
    public Object run() throws ZuulException {
        try {
            log.info("=================进入安全密钥共享平台重新加签验证过滤器,=====================");
            // 获取request
            RequestContext ctx = RequestContext.getCurrentContext();
            HttpServletRequest request = ctx.getRequest();
            String sendbody = ctx.get(GlobalConstants.SENDBODY).toString();
            log.info("访问者IP:{}", HttpUtils.getIpAdrress(request));
            //1.获取heard中的userID和ApiID
            String apiID=request.getHeader("api_id");
            //2.获取body中的解密后的数据
            InputStream in = request.getInputStream();
            String body = StreamUtils.copyToString(in, Charset.forName("UTF-8"));
            JSONObject jsonObject = JSON.parseObject(body);
            String data=jsonObject.get("data").toString();
            String sign=jsonObject.get("sign").toString();
            if (StringUtils.isBlank(data) && StringUtils.isBlank(sign))
                throw new RuntimeException("未获取到安全密钥共享平台重新加签验证过滤器数据或签名");
//            List<com.ztgeo.suqian.entity.ag_datashare.ApiBaseInfo> list =apiBaseInfoRepository.findApiBaseInfosByApiIdEquals(apiID);
//            if (!Objects.equals(null, list) && list.size() != 0) {
//                ApiBaseInfo apiBaseInfo = list.get(0);
//                String apiUserID = redis.get(USER_REDIS_SESSION + ":" + apiBaseInfo.getApiOwnerId());
//                if (StringUtils.isBlank(apiUserID)) {
//                    UserKeyInfo userKeyInfo = userKeyInfoRepository.findByUserRealIdEquals(apiBaseInfo.getApiOwnerId());
//                    Sign_pt_secret_key = userKeyInfo.getSymmetricPubkey();
//                    JSONObject setjsonObject = new JSONObject();
//                    setjsonObject.put("Symmetric_pubkey", userKeyInfo.getSymmetricPubkey());
//                    setjsonObject.put("Sign_secret_key", userKeyInfo.getSignSecretKey());
//                    setjsonObject.put("Sign_pub_key", userKeyInfo.getSignPubKey());
//                    setjsonObject.put("Sign_pt_secret_key", userKeyInfo.getSignPtSecretKey());
//                    setjsonObject.put("Sign_pt_pub_key", userKeyInfo.getSignPtPubKey());
//                    //存入Redis
//                    redis.set(USER_REDIS_SESSION + ":" + apiBaseInfo.getApiOwnerId(), setjsonObject.toJSONString());
//                } else {
//                    JSONObject getjsonObject = JSONObject.parseObject(apiUserID);
//                    Sign_pt_secret_key = getjsonObject.getString("Sign_pt_secret_key");
//                    if (StringUtils.isBlank(Sign_pt_secret_key)) {
//                        throw new ZtgeoBizRuntimeException(CodeMsg.FAIL, "未查询到安全密钥共享平台重新加签验证过滤器密钥信息");
//                    }
//                }
//            }else {
//                log.info("未匹配到注册路由,请求路径");
//                throw new ZtgeoBizZuulException(CodeMsg.NOT_FOUND, "未匹配到安全密钥共享平台重新加签验证过滤器注册接口路由");
//            }
            ApiBaseInfo apiBaseInfo=apiBaseInfoRepository.queryApiBaseInfoByApiId(apiID);
            Sign_pt_secret_key=apiBaseInfo.getSignPtSecretKey();
            //3.重新加签
            String receiveSign = CryptographyOperation.generateSign(Sign_pt_secret_key, data);
            //4.重新加载到requset中
            jsonObject.put("data", data);
            jsonObject.put("sign", receiveSign);
            String newbody = jsonObject.toString();
            ctx.set(GlobalConstants.SENDBODY, newbody);
            return getObject(ctx, request, newbody);
        }catch (Exception e) {
            log.info("20010-共享平台请求方重新加签过滤器异常");
            throw new RuntimeException("20010-共享平台请求方重新加签过滤器异常");
        }
    }

    @Override
    public boolean shouldFilter() {
        String className = this.getClass().getSimpleName();
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();
        api_id = request.getHeader("api_id");
        int count = apiUserFilterRepository.countApiUserFiltersByFilterBcEqualsAndApiIdEquals(className, api_id);
        if (count > 0) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int filterOrder() {
        return 4;
    }

    @Override
    public String filterType() {
        return FilterConstants.PRE_TYPE;
    }


}
