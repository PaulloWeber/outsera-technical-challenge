FROM eclipse-temurin:21-jdk-alpine AS deps
WORKDIR /app
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw -q dependency:go-offline

FROM deps AS build
COPY src src
RUN ./mvnw -q package -DskipTests

FROM build AS test
RUN ./mvnw test

FROM eclipse-temurin:21-jdk-alpine AS jre
RUN jlink \
    --add-modules java.base,java.compiler,java.logging,java.xml,java.naming,java.desktop,java.management,java.security.jgss,java.instrument,java.sql,java.transaction.xa,java.net.http,jdk.unsupported,jdk.jfr \
    --strip-debug --no-man-pages --no-header-files --compress=zip-6 \
    --output /javaruntime

FROM alpine:3.22 AS runtime
ENV JAVA_HOME=/opt/java
ENV PATH="${JAVA_HOME}/bin:${PATH}"
COPY --from=jre /javaruntime ${JAVA_HOME}
RUN addgroup -S spring && adduser -S spring -G spring
USER spring
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
