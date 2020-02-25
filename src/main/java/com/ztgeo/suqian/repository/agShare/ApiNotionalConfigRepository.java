package com.ztgeo.suqian.repository.agShare;

import com.ztgeo.suqian.entity.ag_datashare.ApiNotionalConfig;
import com.ztgeo.suqian.entity.ag_datashare.ApiNotionalSharedConfig;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface ApiNotionalConfigRepository extends CrudRepository<ApiNotionalConfig,String> {
    @Query(value = "SELECT * from api_notionalshared_config anc where anc.userid in (SELECT abi.api_owner_id  from api_base_info abi where abi.api_id=?)",nativeQuery = true)
    ApiNotionalConfig findApiNotionalSharedConfigsByapiIdEquals(String apiid);
}
