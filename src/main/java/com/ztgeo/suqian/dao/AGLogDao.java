package com.ztgeo.suqian.dao;

import com.ztgeo.suqian.config.annotation.DataSource;
import com.ztgeo.suqian.entity.ag_datashare.NoticeRecord;
import com.ztgeo.suqian.entity.ag_log.*;
import com.ztgeo.suqian.repository.agLog.*;
import com.ztgeo.suqian.repository.agShare.NoticeRecordRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.List;

@Repository
public class AGLogDao {
    @Resource
    private ApiAccessRecordRepository apiAccessRecordRepository;
    @Resource
    private NoticeRecordRepository noticeRecordRepository;
    @DataSource("slave1")
    public void saveApiAccessRecord(ApiAccessRecord apiAccessRecord){
        apiAccessRecordRepository.save(apiAccessRecord);
    }
    @DataSource("slave1")
    public void updateResponsedateById(String responsedata,String status,String id){
        apiAccessRecordRepository.updateResponsedateById(responsedata,status,id);
    }
    @DataSource("slave1")
    public void saveNoticeRecord(NoticeRecord noticeRecord){
        noticeRecordRepository.save(noticeRecord);
    }
    @DataSource("slave1")
    public int updateNoticeRecordCount(int count,String recordId){
       return noticeRecordRepository.updateNoticeRecordCount(count,recordId);
    }
    @DataSource("slave1")
    public List<NoticeRecord> findNoticeRecordsByStatusAndCountLessThan(int status, int count){
        return noticeRecordRepository.findNoticeRecordsByStatusAndCountLessThan(status,count);
    }
}
