package com.neverpile.eureka.util;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * This Annotation is used to setup the main application class to be used for neverpile Eureka.
 */
@EnableTransactionManagement
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableHypermediaSupport(type = EnableHypermediaSupport.HypermediaType.HAL)
@EnableConfigurationProperties
public @interface EnableNeverpileEurekaSpringApplication {

}
