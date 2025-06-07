FROM ubuntu:plucky-20250521

RUN apt-get update && \
  apt-get install -y openjdk-24-jre-headless

COPY build/libs/lcloud-discovery-example-3.1.2-M7-all.jar demo.jar

RUN mkdir logs
ENTRYPOINT ["java", "-Xmx16m", "-Dorg.mtbo.lcloud.discovery.level=INFO",  "-Dreactor.schedulers.defaultBoundedElasticOnVirtualThreads=true",  "-jar", "demo.jar"]