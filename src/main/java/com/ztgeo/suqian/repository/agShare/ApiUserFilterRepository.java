package com.ztgeo.suqian.repository.agShare;

import com.ztgeo.suqian.entity.ag_datashare.ApiUserFilter;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ApiUserFilterRepository extends CrudRepository<ApiUserFilter,String> {

    int countApiUserFiltersByFilterBcEqualsAndApiIdEquals(String filterBC, String apiId);
    List<ApiUserFilter> findApiUserFiltersByApiId(String apiId);
}
