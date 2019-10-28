package com.ztgeo.suqian.repository.agLog;
import com.ztgeo.suqian.entity.ag_log.ApiAccessRecord;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import javax.transaction.Transactional;


public interface ApiAccessRecordRepository extends CrudRepository<ApiAccessRecord,String> {
    @Transactional
    @Modifying
    @Query(value = "UPDATE api_access_record set response_data=? ,status=? where id=?",nativeQuery = true)
    void updateResponsedateById(String responseDate,String status,String id);
}
