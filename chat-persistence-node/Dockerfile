# Stage 1: Build and Extract
FROM maven:3.9.7-eclipse-temurin-21 AS builder
WORKDIR /workspace

COPY pom.xml .
COPY chat-common ./chat-common
COPY chat-persistence-node ./chat-persistence-node
COPY chat-connection-node ./chat-connection-node

RUN mvn -pl chat-persistence-node -am clean package -DskipTests

# Extract the layered components using the modern Spring Boot 3.3+ tools mode
WORKDIR /workspace/chat-persistence-node/target
RUN java -Djarmode=tools -jar chat-persistence-node-*.jar extract --layers --destination extracted

# Stage 2: Secure Runtime Environment
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy from the new 'extracted' destination directory
COPY --from=builder /workspace/chat-persistence-node/target/extracted/dependencies/ ./
COPY --from=builder /workspace/chat-persistence-node/target/extracted/spring-boot-loader/ ./
COPY --from=builder /workspace/chat-persistence-node/target/extracted/snapshot-dependencies/ ./
COPY --from=builder /workspace/chat-persistence-node/target/extracted/application/ ./

EXPOSE 8090

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]