# Build multi-modulo: el contexto es la raiz del repo (lo fija docker-compose). El pom padre
# declara los 5 modulos, asi que se copia el arbol completo (.dockerignore excluye target/, .git
# y frontend). Maven compila solo este servicio y sus dependencias internas (-am arrastra
# shared-events) sin construir el resto.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
# El pom no vincula 'repackage' a la fase package (el jar principal queda como libreria). Se hace en
# pasos: 1) install del pom padre (-N) y de shared-events al repo local; 2) en una sola invocacion acotada al servicio
# (sin -am, asi 'repackage' no toca shared-events que no tiene main class) se ejecuta package +
# repackage en el mismo ciclo de vida. Resultado: fat-jar ejecutable con classifier 'exec'.
RUN mvn -q -N install \
 && mvn -q -pl shared-events -DskipTests install \
 && mvn -q -pl account-service -DskipTests package spring-boot:repackage

# Runtime ligero: solo JRE + el jar ejecutable repackaged por spring-boot-maven-plugin.
FROM eclipse-temurin:21-jre
WORKDIR /app
# El jar ejecutable lleva el classifier 'exec' (ver pom: el jar sin classifier es libreria normal).
COPY --from=build /workspace/account-service/target/account-service-0.0.1-SNAPSHOT-exec.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
