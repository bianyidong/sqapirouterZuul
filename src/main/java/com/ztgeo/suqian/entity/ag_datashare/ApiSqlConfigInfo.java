package com.ztgeo.suqian.entity.ag_datashare;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "api_sql_config")
public class ApiSqlConfigInfo implements Serializable {
    @Id
    @Column(name = "id")
    private String id;
    @Column(name = "api_id")
    private String apiId;
    @Column(name = "Db_username")
    private String dbUsername;
    @Column(name = "Db_password")
    private String dbPassword;
    @Column(name = "Db_ip")
    private String dbIp;
    @Column(name = "Db_sql")
    private String dbSql;
}
