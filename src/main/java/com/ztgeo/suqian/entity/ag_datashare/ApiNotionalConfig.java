package com.ztgeo.suqian.entity.ag_datashare;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "api_notionalshared_config")//徐州国家级表配置的
public class ApiNotionalConfig implements Serializable {
    @Id
    @Column(name = "userid")
    private String userid;
    @Column(name = "user_name")
    private String userName;
    @Column(name = "ip")
    private String ip;
    @Column(name = "dept_name")
    private String deptName;
    @Column(name = "qxdm")
    private String qxdm;
}
