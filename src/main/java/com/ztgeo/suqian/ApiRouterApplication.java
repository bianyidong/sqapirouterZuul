package com.ztgeo.suqian;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

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
