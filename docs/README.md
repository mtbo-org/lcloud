## Lightweight Cloud Tools

### Branches

| Branch    | Description                         | Java Version |
|-----------|-------------------------------------|--------------|
| 1-plain   | Plain implementation, using Threads | Java 8-      |
| 2-pure    | Flow implementation                 | Java 9+      |
| 3-reactor | Reactor implementation              | Java 8+      |

### Discovery

Enumerate endpoints on LAN.

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