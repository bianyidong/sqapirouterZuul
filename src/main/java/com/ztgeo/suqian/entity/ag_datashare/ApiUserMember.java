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
@Entity(name = "api_user_member")
public class ApiUserMember implements Serializable {

	    //
    @Id
	@Column(name = "id")
    private String id;
	
	    //
    @Column(name = "api_id")
    private String apiId;
	
	    //
    @Column(name = "user_id")
    private String userId;

}
