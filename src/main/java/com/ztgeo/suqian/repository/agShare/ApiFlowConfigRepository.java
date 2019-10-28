package com.ztgeo.suqian.repository.agShare;

import com.ztgeo.suqian.entity.ag_datashare.ApiFlowConfig;
import org.springframework.data.repository.CrudRepository;

public interface ApiFlowConfigRepository extends CrudRepository<ApiFlowConfig, String> {

    ApiFlowConfig findApiFlowConfigsByApiIdEquals(String apiId);
}