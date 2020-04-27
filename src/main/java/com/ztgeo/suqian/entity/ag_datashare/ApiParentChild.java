package com.ztgeo.suqian.entity.ag_datashare;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "api_sqlparent_child")
public class ApiParentChild implements Serializable {
    @Id
    @Column(name = "id")
    private String id;
    @Column(name = "api_parenttableid")
    private String apiParenttableid;

    @Column(name = "childkeyname")
    private String childKeyname;

    @Column(name = "childtableid")
    private String childTableid;

}