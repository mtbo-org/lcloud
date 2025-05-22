FROM openjdk:24-jdk-slim AS build
COPY build/libs/lcloud-udp-discovery-example-3.1.2-ALPHA-all.jar demo.jar

RUN mkdir logs
ENTRYPOINT ["java", "-Dorg.mtbo.lcloud.discovery.level=INFO", "-Dreactor.schedulers.defaultBoundedElasticOnVirtualThreads=true", "-jar", "demo.jar"]