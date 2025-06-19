# Read Me First
The following was discovered as part of building this project:

* The original package name 'com.iot.devices.management.registry-service' is invalid and this project uses 'com.iot.devices.management.registry_service' instead.

# Getting Started

### Reference Documentation
For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/3.5.1/maven-plugin)
* [Create an OCI image](https://docs.spring.io/spring-boot/3.5.1/maven-plugin/build-image.html)
* [Spring Boot Testcontainers support](https://docs.spring.io/spring-boot/3.5.1/reference/testing/testcontainers.html#testing.testcontainers)
* [Testcontainers Kafka Modules Reference Guide](https://java.testcontainers.org/modules/kafka/)
* [Testcontainers Postgres Module Reference Guide](https://java.testcontainers.org/modules/databases/postgres/)
* [Spring Boot DevTools](https://docs.spring.io/spring-boot/3.5.1/reference/using/devtools.html)
* [Spring Configuration Processor](https://docs.spring.io/spring-boot/3.5.1/specification/configuration-metadata/annotation-processor.html)
* [Spring Web](https://docs.spring.io/spring-boot/3.5.1/reference/web/servlet.html)
* [Rest Repositories](https://docs.spring.io/spring-boot/3.5.1/how-to/data-access.html#howto.data-access.exposing-spring-data-repositories-as-rest)
* [Spring Data JPA](https://docs.spring.io/spring-boot/3.5.1/reference/data/sql.html#data.sql.jpa-and-spring-data)
* [Testcontainers](https://java.testcontainers.org/)
* [Spring for Apache Kafka](https://docs.spring.io/spring-boot/3.5.1/reference/messaging/kafka.html)
* [Prometheus](https://docs.spring.io/spring-boot/3.5.1/reference/actuator/metrics.html#actuator.metrics.export.prometheus)
* [Spring Boot Actuator](https://docs.spring.io/spring-boot/3.5.1/reference/actuator/index.html)

### Guides
The following guides illustrate how to use some features concretely:

* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)
* [Accessing JPA Data with REST](https://spring.io/guides/gs/accessing-data-rest/)
* [Accessing Neo4j Data with REST](https://spring.io/guides/gs/accessing-neo4j-data-rest/)
* [Accessing MongoDB Data with REST](https://spring.io/guides/gs/accessing-mongodb-data-rest/)
* [Accessing Data with JPA](https://spring.io/guides/gs/accessing-data-jpa/)
* [Building a RESTful Web Service with Spring Boot Actuator](https://spring.io/guides/gs/actuator-service/)

### Testcontainers support

This project uses [Testcontainers at development time](https://docs.spring.io/spring-boot/3.5.1/reference/features/dev-services.html#features.dev-services.testcontainers).

Testcontainers has been configured to use the following Docker images:

* [`apache/kafka-native:latest`](https://hub.docker.com/r/apache/kafka-native)
* [`postgres:latest`](https://hub.docker.com/_/postgres)

Please review the tags of the used images and set them to the same as you're running in production.

### Maven Parent overrides

Due to Maven's design, elements are inherited from the parent POM to the project POM.
While most of the inheritance is fine, it also inherits unwanted elements like `<license>` and `<developers>` from the parent.
To prevent this, the project POM contains empty overrides for these elements.
If you manually switch to a different parent and actually want the inheritance, you need to remove those overrides.

