
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