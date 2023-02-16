[![Actions Status](https://github.com/levigo/neverpile-eureka/workflows/Continuous%20Delivery/badge.svg)](https://github.com/levigo/neverpile-fusion/actions)
[![Made with JAVA](https://img.shields.io/badge/MADE%20with-JAVA-RED.svg)](#JAVA)
[![Generic badge](https://img.shields.io/badge/current%20version-0.2.66-1abc9c.svg)](https://github.com/levigo/neverpile-eureka/tree/v0.2.60)

# neverpile-eureka
neverpile eureka - The archive system for the cloud generation.

# Getting started
The easiest way to get started delevoping with neverpile eureka is to follow [our getting started tutorials](https://github.com/levigo/neverpile-eureka-getting-started/wiki). We also have [a couple of demos](https://github.com/levigo/neverpile-eureka-demos) illustrating further concepts.

# Current version
__Import BOM__

    <dependencyManagement>
      <dependencies>
        <dependency>
          <groupId>com.neverpile.eureka</groupId>
          <artifactId>neverpile-eureka-bom</artifactId>
          <version>0.2.66</version>
          <type>pom</type>
          <scope>import</scope>
        </dependency>
      </dependencies>
    </dependencyManagement>

__Import Spring-Boot starter__

    <dependency>
      <groupId>com.neverpile.eureka</groupId>
      <artifactId>neverpile-eureka-spring-boot-starter</artifactId>
      <version>0.2.66</version>
    </dependency>
