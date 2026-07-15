# Stage 1: Build and Extract
FROM maven:3.9.7-eclipse-temurin-21 AS builder
WORKDIR /workspace

# Copy the root POM and the required modules
COPY pom.xml .
COPY chat-common ./chat-common
COPY chat-connection-node ./chat-connection-node

# Compile the specific module and its internal dependencies (-am = also make)
RUN mvn -pl chat-connection-node -am clean package -DskipTests

# Extract the layered components from the built artifact
WORKDIR /workspace/chat-connection-node/target
RUN java -Djarmode=layertools -jar chat-connection-node-*.jar extract

# Stage 2: Secure Runtime Environment
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Enforce security boundaries by creating a non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the extracted layers in strictly increasing order of modification frequency
COPY --from=builder /workspace/chat-connection-node/target/dependencies/ ./
COPY --from=builder /workspace/chat-connection-node/target/spring-boot-loader/ ./
COPY --from=builder /workspace/chat-connection-node/target/snapshot-dependencies/ ./
COPY --from=builder /workspace/chat-connection-node/target/application/ ./

EXPOSE 8081

# Execute via the specialized launcher to leverage the exploded layer structure
# Note: Spring Boot 3.2+ moved the launcher to the .launch sub-package
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]