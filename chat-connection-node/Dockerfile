# Stage 1: Build and Extract
FROM maven:3.9.7-eclipse-temurin-21 AS builder
WORKDIR /workspace

# Copy the root POM and the required modules
COPY pom.xml .
COPY chat-common ./chat-common
COPY chat-connection-node ./chat-connection-node
COPY chat-persistence-node ./chat-persistence-node

# Compile the specific module and its internal dependencies (-am = also make)
RUN mvn -pl chat-connection-node -am clean package -DskipTests

# Extract the layered components using the modern Spring Boot 3.3+ tools mode
WORKDIR /workspace/chat-connection-node/target
RUN java -Djarmode=tools -jar chat-connection-node-*.jar extract --layers --destination extracted

# Stage 2: Secure Runtime Environment
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Enforce security boundaries by creating a non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy from the new 'extracted' destination directory
COPY --from=builder /workspace/chat-connection-node/target/extracted/dependencies/ ./
COPY --from=builder /workspace/chat-connection-node/target/extracted/spring-boot-loader/ ./
COPY --from=builder /workspace/chat-connection-node/target/extracted/snapshot-dependencies/ ./
COPY --from=builder /workspace/chat-connection-node/target/extracted/application/ ./

EXPOSE 8081

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]