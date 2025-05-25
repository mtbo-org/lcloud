## Rebuild and install on k8s (minikube)

Rebuild jar:

```shell
cd ..
./gradlew spotlessApply clean build publish
```

Start minikube:

```shell
minikube start
minikube docker-env

@FOR /f "tokens=*" %i IN ('minikube -p minikube docker-env --shell cmd') DO @%i 
docker login
docker build -t lcloud-udp-discovery-example:latest .
helm install udp-discovery-example udp-discovery-example
```
```shell
helm uninstall udp-discovery-example
```
Rebuild example:

```shell
docker build -t lcloud-udp-discovery-example:latest .
```

Re-apply image:

```shell
kubectl scale deployment -n default udp-discovery-example --replicas=0
kubectl scale deployment -n default udp-discovery-example --replicas=3
```

Scale up:

```shell
kubectl scale deployment -n default udp-discovery-example --replicas=8
```

Turn off:

```shell
kubectl scale deployment -n default udp-discovery-example --replicas=0
```

Run locally:

```shell

cd  /d/work/java/lcloud/lcloud-udp-discovery-example
HOSTNAME="AAA" \
  java -Dorg.mtbo.lcloud.discovery.level=INFO \
    -Dreactor.schedulers.defaultBoundedElasticOnVirtualThreads=true \
    -jar build/libs/lcloud-udp-discovery-example-3.1.2-M5-all.jar
    
cd  /d/work/java/lcloud/lcloud-udp-discovery-example
HOSTNAME="BBB" \
  java -Dorg.mtbo.lcloud.discovery.level=INFO \
    -Dreactor.schedulers.defaultBoundedElasticOnVirtualThreads=true \
    -jar build/libs/lcloud-udp-discovery-example-3.1.2-M5-all.jar
    
cd  /d/work/java/lcloud/lcloud-udp-discovery-example
HOSTNAME="XXX" \
  java -Dorg.mtbo.lcloud.discovery.level=INFO \
    -Dreactor.schedulers.defaultBoundedElasticOnVirtualThreads=true \
    -jar build/libs/lcloud-udp-discovery-example-3.1.2-M5-all.jar
    
cd  /d/work/java/lcloud/lcloud-udp-discovery-example
HOSTNAME="YYY" \
  java -Dorg.mtbo.lcloud.discovery.level=INFO \
    -Dreactor.schedulers.defaultBoundedElasticOnVirtualThreads=true \
    -jar build/libs/lcloud-udp-discovery-example-3.1.2-M5-all.jar
    
```

Uninstall:

```shell
helm uninstall udp-discovery-example
```
