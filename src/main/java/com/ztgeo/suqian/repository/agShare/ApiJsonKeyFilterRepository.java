package com.ztgeo.suqian.repository.agShare;

import com.ztgeo.suqian.entity.ag_datashare.ApiJsonKeyFilter;
import org.springframework.data.repository.CrudRepository;

public interface ApiJsonKeyFilterRepository extends CrudRepository<ApiJsonKeyFilter, String> {
    ApiJsonKeyFilter findApiJsonKeyFiltersByApiIdEqualsAndFromUserEquals(String apiId, String fromUser);
}