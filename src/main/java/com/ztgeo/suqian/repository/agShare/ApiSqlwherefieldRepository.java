package com.ztgeo.suqian.repository.agShare;

import com.ztgeo.suqian.entity.ag_datashare.Apisqlwherefield;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ApiSqlwherefieldRepository extends CrudRepository<Apisqlwherefield,String> {
    List<Apisqlwherefield> findApisqlwherefieldsByApiIdOrderByFieldorder(String apiId);
}
