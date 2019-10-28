package com.ztgeo.suqian.repository.agShare;

import com.ztgeo.suqian.entity.ag_datashare.BaseUser;
import org.springframework.data.repository.CrudRepository;



public interface BaseUserRepository extends CrudRepository<BaseUser,String> {
    BaseUser findByIdEquals(String Id);
}
