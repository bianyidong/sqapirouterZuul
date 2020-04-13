package com.ztgeo.suqian.repository.agShare;

import com.ztgeo.suqian.entity.ag_datashare.ApiSqlConfigInfo;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ApiSqlConfigInfoRepository extends CrudRepository<ApiSqlConfigInfo,String> {
    List<ApiSqlConfigInfo> findApiSqlConfigInfosByApiIdEquals(String apiId);
}
