package com.ztgeo.suqian.dao;

import com.ztgeo.suqian.config.annotation.DataSource;
import com.ztgeo.suqian.entity.ag_datashare.*;
import com.ztgeo.suqian.repository.agShare.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Repository
@Transactional
public class AGShareDao {
    @Resource
    private ApiBaseInfoRepository apiBaseInfoRepository;
    @Resource
    private ApiChangeTypeRepository apiChangeTypeRepository;
    @Resource
    private ApiCitySharedConfigRepository apiCitySharedConfigRepository;
    @Resource
    private ApiColMappingRepository apiColMappingRepository;
    @Resource
    private  ApiFlowConfigRepository apiFlowConfigRepository;
    @Resource
    private  ApiFlowInstRepository apiFlowInstRepository;
    @Resource
    private  ApiIpWhitelistFilterRepository apiIpWhitelistFilterRepository;
    @Resource
    private  ApiJgtoPtFilterRepository apiJgtoPtFilterRepository;
    @Resource
    private ApiJsonKeyFilterRepository apiJsonKeyFilterRepository;
    @Resource
    private ApiNotionalSharedConfigRepository apiNotionalSharedConfigRepository;
    @Resource
    private ApiRouterDefineRepository apiRouterDefineRepository;
    @Resource
    private ApiTimeFilterRepository apiTimeFilterRepository;
    @Resource
    private ApiUserFilterRepository apiUserFilterRepository;
    @Resource
    private DzYixingRepository dzYixingRepository;
    @Resource
    private ApiNotionalConfigRepository apiNotionalConfigRepository;
    @Resource
    private ApiSqlConfigInfoRepository apiSqlConfigInfoRepository;
    @Resource
    private ApiSqlwherefieldRepository apiSqlwherefieldRepository;
    //查询api_id的数量
    @DataSource
    public int countApiBaseInfosByApiIdEquals(String api_id){
        return apiBaseInfoRepository.countApiBaseInfosByApiIdEquals(api_id);
    }
    @DataSource
    public List<ApiBaseInfo> findApiBaseInfosByApiIdEquals(String api_id){
        return apiBaseInfoRepository.findApiBaseInfosByApiIdEquals(api_id);
    }
    @DataSource
    public List<ApiSqlConfigInfo> findApiSqlConfigInfosByApiId(String apiId){
        return apiSqlConfigInfoRepository.findApiSqlConfigInfosByApiIdEquals(apiId);
    }
    @DataSource
    public List<Apisqlwherefield> findApisqlwherefieldsByApiIdOrderByFieldorder(String api_id){
        return apiSqlwherefieldRepository.findApisqlwherefieldsByApiIdOrderByFieldorder(api_id);
    }
    @DataSource
    public ApiNotionalConfig findApiNotionalSharedConfigsByapiIdEquals(String apiid){
        return  apiNotionalConfigRepository.findApiNotionalSharedConfigsByapiIdEquals(apiid);
    }
    @DataSource
    public ApiBaseInfo queryApiBaseInfoByApiId(String ApiId){
        return apiBaseInfoRepository.queryApiBaseInfoByApiId(ApiId);
    }

    //查询是否配置当前过滤器
    @DataSource
    public int countApiUserFiltersByFilterBcEqualsAndApiIdEquals(String filterBC,String apiId){
        return apiUserFilterRepository.countApiUserFiltersByFilterBcEqualsAndApiIdEquals(filterBC, apiId);
    }

}
