package com.ztgeo.suqian.entity.ag_log;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.time.Month;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "api_access_record")
public class ApiAccessRecord implements Serializable {
	private static final long serialVersionUID = 1L;
	
	    //
    @Id
	@Column(name = "id")
    private String id;
	
	    //api主键ID
    @Column(name = "api_id")
    private String apiId;

	@Column(name = "from_user")
	private String fromUser;
	@Column(name = "user_name")
	private String userName;
	    //
    @Column(name = "api_name")
    private String apiName;
	
	    //
    @Column(name = "api_url")
    private String apiUrl;

//	@Column(name = "filter_user")
////	private String filterUser;
	// 访问者IP
	@Column(name = "type")
	private String type;
    // 访问者IP
	@Column(name = "access_client_ip")
    private String accessClientIp;
	// 访问者IP
	@Column(name = "uri")
	private String uri;
	@Column(name = "year_month_day")
	private String yearMonthDay;
	    //访问时间
//    @Column(name = "access_year")
//    private int accessYear;
//
//	    //
//    @Column(name = "access_month")
//    private int accessMonth;
//
//	    //
//    @Column(name = "access_day")
//    private int accessDay;
//
	    //访问时间
    @Column(name = "access_time")
    private String accessTime;

    // 请求数据
	@Column(name = "request_data")
	private String requestData;

	// 响应数据
	@Column(name = "response_data")
	private String responseData;
	// 发送方用户ID
	@Column(name = "api_owner_id")
	private String apiOwnerId;
	//发送是否成功0-成功1-失败
    @Column(name="status")
	private  String status;

}
