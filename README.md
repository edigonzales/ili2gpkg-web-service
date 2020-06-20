# ili2gpkg-web-service

## Building (hybrid mode)

```
mkdir -p src/main/resources/META-INF/native-image
```

### Local

```
mvn -DskipTests clean package
java -agentlib:native-image-agent=config-output-dir=src/main/resources/META-INF/native-image \
  -Dorg.graalvm.nativeimage.imagecode=agent \
  -jar target/ili2gpkg-web-service-0.0.1-SNAPSHOT.jar
```

Die vom _tracing agent_ erstellten Config-Dateien sind nicht OS-agnostisch (z.B. SQLite-Bibliotheken). Aus diesem Grund können die Config-Dateien nicht auf macOS erstellt werden und später mit diesen Config-Dateien ein _native binary_ für Linux gebuildet werden. 


_Native binary_ erstellen:
```
./mvnw -Pnative -DskipTests clean package
```


### Docker-Linux-Build

Zuerst muss ein Fat-Jar gebuildet und anschliessend ein einfaches Docker-Image erstellt werden. Die Anwendung im Image wird mit dem _tracing agent_ gestartet.
```
./mvnw -DskipTests clean package
docker build -t sogis/ili2gpkg-web-service-agent -f Dockerfile.jvm .
docker run --rm --name ili2gpkg-web-service-agent -v /tmp:/tmp -p8080:8080 sogis/ili2gpkg-web-server-agent
```

Möglichst alles in der Anwendung manuell durchspielen/durchklicken und mit Ctrl+C Anwendung beenden. Im `/tmp`-Ordner sollten die vier Config-Dateien liegen:

- `jni-config.json`
- `proxy-config.json`
- `reflect-config.json`
- `resource-config.json`

Diese müssen in den `src/main/resources/META-INF/native-image`-Ordner kopiert werden.

Mit dem Multi-Stage Dockerfile das Dockerimage erstellen:
```
docker build -t sogis/ili2gpkg-web-service -f Dockerfile .
```

## Running
```
docker run --rm --name ili2gpkg-web-service -p8080:8080 sogis/ili2gpkg-web-service
```