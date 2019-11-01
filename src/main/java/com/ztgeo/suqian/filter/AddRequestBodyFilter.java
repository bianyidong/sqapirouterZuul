package com.ztgeo.suqian.filter;


import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.netflix.zuul.http.ServletInputStreamWrapper;
import com.ztgeo.suqian.common.GlobalConstants;
import com.ztgeo.suqian.common.ZtgeoBizZuulException;
import com.ztgeo.suqian.dao.AGLogDao;
import com.ztgeo.suqian.entity.ag_datashare.ApiUserFilter;
import com.ztgeo.suqian.entity.ag_datashare.BaseUser;
import com.ztgeo.suqian.entity.ag_log.ApiAccessRecord;
import com.ztgeo.suqian.entity.ag_datashare.ApiBaseInfo;
import com.ztgeo.suqian.msg.CodeMsg;
import com.ztgeo.suqian.repository.agShare.ApiBaseInfoRepository;
import com.ztgeo.suqian.repository.agShare.ApiUserFilterRepository;
import com.ztgeo.suqian.repository.agShare.BaseUserRepository;
import com.ztgeo.suqian.utils.HttpUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import javax.annotation.Resource;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;


/**
 * 请求记录日志过滤器
 *
 * @author bianyidong
 * @version 2019-7-12
 */
@Component
public class AddRequestBodyFilter extends ZuulFilter {

    private static Logger log = LoggerFactory.getLogger(AddRequestBodyFilter.class);
    @Resource
    private ApiBaseInfoRepository apiBaseInfoRepository;
    @Resource
    private ApiUserFilterRepository apiUserFilterRepository;
    @Resource
    private BaseUserRepository baseUserRepository;
    @Resource
    private AGLogDao agLogDao;
    @Value("${customAttributes.httpName}")
    private String httpName; // 存储用户发送数据的数据库名
    private String UserFilter = "";

    @Override
    public Object run() throws ZuulException {
        try {
            log.info("=================进入记录数据日志过滤器,=====================");
            // 获取request
            RequestContext ctx = RequestContext.getCurrentContext();
            HttpServletRequest request = ctx.getRequest();
            String uri = request.getRequestURI();
            String url = request.getRequestURL().toString();
            String type = request.getContentType();
            String s = request.getSession().toString();
            String aa = request.getRequestedSessionId();
            //String sendbody = ctx.get(GlobalConstants.SENDBODY).toString();
            //2.获取body中的加密和加签数据并验签
            InputStream in = request.getInputStream();
            String requestBody = StreamUtils.copyToString(in, Charset.forName("UTF-8"));
            log.info("访问者IP:{}", HttpUtils.getIpAdrress(request));
            //1.获取heard中的userID和ApiID
            String apiID;
            String userID;
            String reqapiID = request.getHeader("api_id");
            String requserID = request.getHeader("from_user");
            if (StringUtils.isEmpty(reqapiID) || StringUtils.isEmpty(requserID)) {
                String ctxApiId = ctx.get("api_id").toString();
                String ctxFromUser = ctx.get("from_user").toString();
                if (StringUtils.isEmpty(ctxApiId)) {
                    throw new ZtgeoBizZuulException(CodeMsg.GETNULL_ERROR);
                } else {
                    apiID = ctxApiId;
                    userID = ctxFromUser;
                }
            } else {
                apiID = reqapiID;
                userID = requserID;
            }
            List<ApiUserFilter> listApiuserFilter = apiUserFilterRepository.findApiUserFiltersByApiId(apiID);
            if (listApiuserFilter.size() == 0) {
                log.info("该接口没有配置需要配置的过滤器");
            } else {
                for (ApiUserFilter apiUserFilter : listApiuserFilter) {
                    UserFilter = UserFilter + apiUserFilter.getFilterName() + ",";
                }
            }
            BaseUser baseUser = baseUserRepository.findByIdEquals(userID);
            String userName = baseUser.getName();
            List<ApiBaseInfo> list = apiBaseInfoRepository.findApiBaseInfosByApiIdEquals(apiID);
            ApiBaseInfo apiBaseInfo = list.get(0);
            String id = com.ztgeo.suqian.utils.StringUtils.getShortUUID();
            String accessClientIp = HttpUtils.getIpAdrress(request);
            LocalDateTime localTime = LocalDateTime.now();
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            DateTimeFormatter dateTimeFormatterYmd = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String currentTime = dateTimeFormatter.format(localTime);
            String currentymd = dateTimeFormatterYmd.format(localTime);
            ApiAccessRecord apiAccessRecord = new ApiAccessRecord();
            apiAccessRecord.setId(id);
            apiAccessRecord.setApiId(apiID);
            apiAccessRecord.setFromUser(userID);
            apiAccessRecord.setUserName(userName);
            apiAccessRecord.setApiName(apiBaseInfo.getApiName());
            apiAccessRecord.setApiUrl(apiBaseInfo.getBaseUrl() + apiBaseInfo.getPath());
            apiAccessRecord.setFilterUser(UserFilter);
            apiAccessRecord.setType(type);
            apiAccessRecord.setAccessClientIp(url);
            apiAccessRecord.setUri(uri);
            apiAccessRecord.setYearMonthDay(currentymd);
            apiAccessRecord.setAccessTime(currentTime);
            apiAccessRecord.setRequestData(requestBody);
            apiAccessRecord.setResponseData("");
            apiAccessRecord.setApiOwnerId(apiBaseInfo.getApiOwnerId());
            apiAccessRecord.setStatus("1");
            agLogDao.saveApiAccessRecord(apiAccessRecord);
            ctx.set(GlobalConstants.RECORD_PRIMARY_KEY, id);
            ctx.set(GlobalConstants.ACCESS_IP_KEY, accessClientIp);
            return getObject(ctx, request, requestBody);
            //return null;
        } catch (Exception e) {
            e.printStackTrace();
            log.info("请求方日志过滤器异常");
            throw new ZtgeoBizZuulException(CodeMsg.ADDSENDBODY_EXCEPTION, "内部异常");
        }
    }

    static Object getObject(RequestContext ctx, HttpServletRequest request, String newbody) {
        final byte[] reqBodyBytes = newbody.getBytes();
        ctx.setRequest(new HttpServletRequestWrapper(request) {
            @Override
            public ServletInputStream getInputStream() throws IOException {
                return new ServletInputStreamWrapper(reqBodyBytes);
            }

            @Override
            public int getContentLength() {
                return reqBodyBytes.length;
            }

            @Override
            public long getContentLengthLong() {
                return reqBodyBytes.length;
            }
        });
        return null;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public int filterOrder() {
        return -98;
    }

    @Override
    public String filterType() {
        return FilterConstants.PRE_TYPE;
    }


}
