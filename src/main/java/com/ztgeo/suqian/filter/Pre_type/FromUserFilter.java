package com.ztgeo.suqian.filter.Pre_type;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.ztgeo.suqian.common.ZtgeoBizZuulException;
import com.ztgeo.suqian.dao.AGShareDao;
import com.ztgeo.suqian.entity.ag_datashare.ApiBaseInfo;
import com.ztgeo.suqian.msg.CodeMsg;
import com.ztgeo.suqian.repository.agShare.ApiBaseInfoRepository;
import com.ztgeo.suqian.repository.agShare.ApiUserFilterRepository;
import com.ztgeo.suqian.repository.agShare.ApiUserMemberRepository;
import com.ztgeo.suqian.repository.agShare.UserKeyInfoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.stereotype.Component;
import sun.rmi.runtime.Log;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 *  请求者过滤器 FromUserFilter
 *  用于过滤请求头中的FROM_USER
 *  判断是否为平台注册合法用户
 *
 *  例外情况：若某机构未在平台注册用户，且平台已经注册用户允许其访问，则不需要验证
 *  （业务情况基本不现实，若要访问平台注册用户则本身也需要注册---姜总讨论）
 */
@Component
public class FromUserFilter extends ZuulFilter {
    private Logger log = LoggerFactory.getLogger(this.getClass());
    @Resource
    private AGShareDao agShareDao;
    @Resource
    private ApiUserMemberRepository apiUserMemberRepository;
    @Resource
    private ApiBaseInfoRepository apiBaseInfoRepository;
    private String api_id;
    private boolean isConfig = false;


    @Override
    public String filterType() {
        return FilterConstants.PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return -98;
    }

    // 此过滤器是否被执行，需详细明确需求，暂时默认执行
    @Override
    public boolean shouldFilter() {
        String className = this.getClass().getSimpleName();
        RequestContext requestContext = RequestContext.getCurrentContext();
        HttpServletRequest httpServletRequest = requestContext.getRequest();
        api_id = httpServletRequest.getHeader("api_id");
        int count = agShareDao.countApiUserFiltersByFilterBcEqualsAndApiIdEquals(className,api_id);
        if(count == 0){
            return false;
        }else{
            isConfig = true;
            return true;
        }
    }

    @Override
    public Object run() throws ZuulException {
        RequestContext requestContext = RequestContext.getCurrentContext();
        HttpServletRequest httpServletRequest = requestContext.getRequest();
        String from_user = httpServletRequest.getHeader("from_user");

        // 从数据库中判断是否存在from_user
        if(from_user != null){
            ApiBaseInfo apiBaseInfo = apiBaseInfoRepository.queryApiBaseInfoByApiId(api_id);
            String apiOwnerid = apiBaseInfo.getApiOwnerId();
            int count = apiUserMemberRepository.countApiUserMembersByApiIdAndUserIdEquals(api_id,from_user);
            if(count == 0&&(!apiOwnerid.equals(from_user))){
                log.info("20001-无法识别身份，拒绝访问");
                throw new RuntimeException("20001-无法识别身份，拒绝访问");
            }
        }else{
            log.info("20001-无法识别身份，拒绝访问");
            throw new RuntimeException("20001-无法识别身份，拒绝访问");
        }
        return null;
    }
}
