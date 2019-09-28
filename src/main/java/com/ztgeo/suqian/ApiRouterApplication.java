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
@Bean
public Connector connector(){
	Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
	connector.setScheme("http");
	connector.setPort(8001);
	connector.setSecure(false);
	connector.setRedirectPort(443);
	return connector;
}

	@Bean
	public TomcatServletWebServerFactory tomcatServletWebServerFactory(){
		TomcatServletWebServerFactory tomcat=new TomcatServletWebServerFactory(){
			@Override
			protected void postProcessContext(Context context) {
				SecurityConstraint securityConstraint = new SecurityConstraint();
				securityConstraint.setUserConstraint("CONFIDENTIAL");
				SecurityCollection collection = new SecurityCollection();
				collection.addPattern("/*");
				securityConstraint.addCollection(collection);
				context.addConstraint(securityConstraint);
			}
		};
		tomcat.addAdditionalTomcatConnectors(connector());
		return tomcat;
	}
}
