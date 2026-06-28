# Build multi-modulo: contexto = raiz del repo (.dockerignore excluye target/, .git y frontend).
# -am arrastra shared-events; no se construyen los demas modulos.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
# 'repackage' no esta vinculado a package: 1) install del pom padre (-N) y de shared-events al repo
# local; 2) en una sola invocacion acotada al servicio (sin -am) se ejecuta package + repackage en el
# mismo ciclo de vida, sin tocar shared-events. Produce el -exec.jar.
RUN mvn -q -N install \
 && mvn -q -pl shared-events -DskipTests install \
 && mvn -q -pl transfer-service -DskipTests package spring-boot:repackage

FROM eclipse-temurin:21-jre
WORKDIR /app
# El jar ejecutable lleva el classifier 'exec' (el jar sin classifier es libreria normal).
COPY --from=build /workspace/transfer-service/target/transfer-service-0.0.1-SNAPSHOT-exec.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
