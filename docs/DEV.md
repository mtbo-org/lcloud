## Build System

Deploy:

```shell
./gradlew clean publish
cd lcloud-udp-discovery
jreleaser deploy --strict -g --git-root-search
```

Dry run full release:

```shell
jreleaser full-release --strict -g --git-root-search --dry-run
```

### Slf4j to java.util.logging

| logging | slf4j |
|---------|-------|
| ALL     | TRACE |
| FINEST  | TRACE |
| FINER   | DEBUG |
| FINE    | DEBUG |
| CONFIG  | INFO  |
| INFO    | INFO  |
| WARNING | WARN  |
| SEVERE  | ERROR |
| OFF     | ERROR |

