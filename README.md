# ili2gpkg-web-service

## Hybrid mode

```
mkdir -p src/main/resources/META-INF/native-image
```

```
mvn -DskipTests clean package
java -agentlib:native-image-agent=config-output-dir=src/main/resources/META-INF/native-image \
  -Dorg.graalvm.nativeimage.imagecode=agent \
  -jar target/ili2gpkg-web-service-0.0.1-SNAPSHOT.jar
```

```
mvn -Pnative -DskipTests clean package
```
