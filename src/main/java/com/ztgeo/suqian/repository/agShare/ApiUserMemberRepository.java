package com.ztgeo.suqian.repository.agShare;

import com.ztgeo.suqian.entity.ag_datashare.ApiBaseInfo;
import com.ztgeo.suqian.entity.ag_datashare.ApiUserMember;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ApiUserMemberRepository extends CrudRepository<ApiUserMember,String> {

    int countApiUserMembersByApiIdAndUserIdEquals(String api_id,String usr_id);

}
