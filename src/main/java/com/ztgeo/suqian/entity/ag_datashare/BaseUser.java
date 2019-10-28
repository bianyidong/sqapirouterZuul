package com.ztgeo.suqian.entity.ag_datashare;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.*;


/**
 * 用户表
 * 
 * @author bianyidong
 * @email 806316372@qq.com
 * @version 2019-10-28 15:53:00
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "base_user")
public class BaseUser implements Serializable {
	
	    //
    @Id
	@Column(name ="id")
    private String id;
	
	    //用户名
    @Column(name = "username")
    private String username;
	
	    //密码
    @Column(name = "password")
    private String password;
	
	    //名称
    @Column(name = "name")
    private String name;
	
	    //生日
    @Column(name = "birthday")
    private String birthday;
	
	    //地址
    @Column(name = "address")
    private String address;
	
	    //手机
    @Column(name = "mobile_phone")
    private String mobilePhone;
	
	    //座机
    @Column(name = "tel_phone")
    private String telPhone;
	
	    //邮箱
    @Column(name = "email")
    private String email;
	
	    //性别
    @Column(name = "sex")
    private String sex;
	
	    //用户类型
    @Column(name = "type")
    private String type;
	
	    //用户状态
    @Column(name = "status")
    private String status;
	
	    //
    @Column(name = "description")
    private String description;
	
	    //
    @Column(name = "crt_time")
    private Date crtTime;
	
	    //
    @Column(name = "crt_user_id")
    private String crtUserId;
	
	    //
    @Column(name = "crt_user_name")
    private String crtUserName;
	
	    //
    @Column(name = "upd_time")
    private Date updTime;
	
	    //
    @Column(name = "upd_user_id")
    private String updUserId;
	
	    //
    @Column(name = "upd_user_name")
    private String updUserName;
	
	    //
    @Column(name = "attr1")
    private String attr1;
	
	    //
    @Column(name = "attr2")
    private String attr2;
	
	    //
    @Column(name = "attr3")
    private String attr3;
	
	    //
    @Column(name = "attr4")
    private String attr4;
	
	    //
    @Column(name = "attr5")
    private String attr5;
	
	    //
    @Column(name = "attr6")
    private String attr6;
	
	    //
    @Column(name = "attr7")
    private String attr7;
	
	    //
    @Column(name = "attr8")
    private String attr8;
	
	    //租户Id
    @Column(name = "tenant_id")
    private String tenantId;
	
	    //是否删除
    @Column(name = "is_deleted")
    private String isDeleted;
	
	    //是否作废
    @Column(name = "is_disabled")
    private String isDisabled;
	
	    //默认部门
    @Column(name = "depart_id")
    private String departId;
	
	    //
    @Column(name = "is_super_admin")
    private String isSuperAdmin;

}
