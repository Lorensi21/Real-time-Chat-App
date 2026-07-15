# Stage 1: Build and Extract
FROM maven:3.9.7-eclipse-temurin-21 AS builder
WORKDIR /workspace

COPY pom.xml .
COPY chat-common ./chat-common
COPY chat-persistence-node ./chat-persistence-node

RUN mvn -pl chat-persistence-node -am clean package -DskipTests

WORKDIR /workspace/chat-persistence-node/target
RUN java -Djarmode=layertools -jar chat-persistence-node-*.jar extract

# Stage 2: Secure Runtime Environment
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder /workspace/chat-persistence-node/target/dependencies/ ./
COPY --from=builder /workspace/chat-persistence-node/target/spring-boot-loader/ ./
COPY --from=builder /workspace/chat-persistence-node/target/snapshot-dependencies/ ./
COPY --from=builder /workspace/chat-persistence-node/target/application/ ./

EXPOSE 8090

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]