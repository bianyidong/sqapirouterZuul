package com.ztgeo.suqian;

import com.ztgeo.suqian.config.register.DynamicDataSourceRegister;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Import(DynamicDataSourceRegister.class)
@EnableTransactionManagement
@EnableScheduling
@EnableZuulProxy
@SpringBootApplication
public class ApiRouterApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiRouterApplication.class, args);
	}

//	@Bean
//	public AccessAuthCheckFilter gateFilter(){
//		return new AccessAuthCheckFilter();
//	}
//
//
//	@Bean
//	public RecordRoutingFilter routeFilter(){
//		return new RecordRoutingFilter();
//	}
//
//	@Bean
//	public ResponseFilter responseFilter(){
//		return new ResponseFilter();
//	}
}
