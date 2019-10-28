package com.ztgeo.suqian.dao;

import com.ztgeo.suqian.config.annotation.DataSource;
import com.ztgeo.suqian.entity.ag_log.*;
import com.ztgeo.suqian.repository.agLog.*;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;

@Repository
public class AGLogDao {
    @Resource
    private ApiAccessRecordRepository apiAccessRecordRepository;
    @DataSource("slave1")
    public void saveApiAccessRecord(ApiAccessRecord apiAccessRecord){
        apiAccessRecordRepository.save(apiAccessRecord);
    }
    @DataSource("slave1")
    public void updateResponsedateById(String responsedata,String status,String id){
        apiAccessRecordRepository.updateResponsedateById(responsedata,status,id);
    }
}
