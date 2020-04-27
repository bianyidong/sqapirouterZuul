package com.ztgeo.suqian.repository.agShare;

import com.ztgeo.suqian.entity.ag_datashare.ApiParentChild;
import com.ztgeo.suqian.entity.ag_datashare.ApiSqlConfigInfo;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ApiSqlParentChildRepository extends CrudRepository<ApiParentChild,String> {
    int countApiParentChildByApiParenttableidEquals(String api_id);
    List<ApiParentChild> findApiParentChildByApiParenttableidEquals(String api_id);
}
