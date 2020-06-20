FROM oracle/graalvm-ce:20.1.0-java8 as graalvm
RUN gu install native-image

COPY . /home/app/ili2gpkg
WORKDIR /home/app/ili2gpkg

RUN ./mvnw dependency:go-offline || true
RUN ./mvnw -Pnative -DskipTests clean package

FROM frolvlad/alpine-glibc
RUN apk update && apk add libstdc++
EXPOSE 8080
COPY --from=graalvm /home/app/ili2gpkg/target/ch.so.agi.ili2gpkgwebserviceapplication /app/ili2gpkg
ENTRYPOINT ["/app/ili2gpkg"]
