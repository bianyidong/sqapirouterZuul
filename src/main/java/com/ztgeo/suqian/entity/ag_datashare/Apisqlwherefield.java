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
@Entity(name = "api_sqlwherefield")
public class Apisqlwherefield implements Serializable {
    @Id
    @Column(name = "id")
    private String id;
    @Column(name = "api_id")
    private String apiId;
    @Column(name = "fieldtype")
    private String fieldtype;
    @Column(name = "tablefield")
    private String tablefield;
    @Column(name = "fieldorder")
    private String fieldorder;
}


