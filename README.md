# ili2gpkg-web-service

## Hybrid mode (local)

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

## Hybrid mode (docker)
```
docker build -t sogis/ili2gpkg-web-server-agent -f Dockerfile.jvm .
```

```
docker run --rm --name ili2gpkg-web-service-agent -v /tmp:/tmp -p8080:8080 sogis/ili2gpkg-web-server-agent
```
