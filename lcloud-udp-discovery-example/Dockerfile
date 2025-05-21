FROM openjdk:24-jdk-slim AS build
COPY build/libs/lcloud-udp-discovery-example-3.1.0-ALPHA-all.jar demo.jar

RUN mkdir logs
ENTRYPOINT ["java", "-Dreactor.schedulers.defaultBoundedElasticOnVirtualThreads=true", "-jar", "demo.jar"]