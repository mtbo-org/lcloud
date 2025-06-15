## Lightweight Cloud

### SQL Discovery

Enumerate endpoints using PostgreSQL.

Example:

```
lcloud-discovery-example/src/main/java/SqlDiscoveryExample.java
``` 


### Multicast Discovery (Java)

Enumerate endpoints on LAN if available. For Kubernetes in case of Single-Node deployment multicast is enabled. Tested on minikube. For multi-node cluster multicast must be enabled explicitly.

Example:

```
lcloud-discovery-example/src/main/java/MulticastDiscoveryExample.java
``` 

Gradle:

```groovy
implementation 'org.mtbo.lcloud:lcloud-udp-discovery:3.0.1'
```

Maven:

```xml

<dependency>
    <groupId>org.mtbo.lcloud</groupId>
    <artifactId>lcloud-udp-discovery</artifactId>
    <version>3.0.1</version>
</dependency>
```

### Dev-Area

[Leader Election MINDMAP](MINDMAP.md)

[How-To](DEV.md)

Copyright (C) 2025 Vladimir Koltunov
