# Build stage
FROM eclipse-temurin:17.0.8_7-jdk-alpine AS build

# Build arguments and environment variables
ARG GRADLE_VERSION=8.3
ENV GRADLE_HOME=/opt/gradle
ENV PATH=$PATH:$GRADLE_HOME/bin

# Install build dependencies
RUN apk add --no-cache \
    curl \
    unzip \
    bash \
    git \
    && rm -rf /var/cache/apk/*

# Set working directory
WORKDIR /app

# Copy Gradle configuration files for layer caching
COPY build.gradle settings.gradle gradlew ./
COPY gradle gradle/

# Download dependencies
RUN ./gradlew dependencies --no-daemon

# Copy source code
COPY src src/

# Build application
RUN ./gradlew clean bootJar --no-daemon \
    && echo "Verifying JAR signature..." \
    && jarsigner -verify build/libs/*.jar

# Runtime stage
FROM eclipse-temurin:17.0.8_7-jre-alpine

# Set environment variables
ENV APP_HOME=/app
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
ENV SECURITY_OPTS="-Djava.security.egd=file:/dev/./urandom -Dfile.encoding=UTF-8"

# Create non-root user and group
RUN addgroup -S -g 1000 vcms && \
    adduser -S -u 1000 -G vcms vcms

# Create application directory structure
RUN mkdir -p ${APP_HOME}/logs ${APP_HOME}/config /tmp && \
    chown -R vcms:vcms ${APP_HOME} /tmp && \
    chmod -R 755 ${APP_HOME} && \
    chmod 1777 /tmp

# Install runtime dependencies
RUN apk add --no-cache \
    curl \
    tzdata \
    && rm -rf /var/cache/apk/*

# Set working directory
WORKDIR ${APP_HOME}

# Copy JAR from build stage
COPY --from=build --chown=vcms:vcms /app/build/libs/*.jar app.jar

# Configure security permissions
RUN chmod 444 app.jar && \
    chmod -R 755 ${APP_HOME}/logs && \
    chmod -R 755 ${APP_HOME}/config

# Expose ports
EXPOSE 8080 8081

# Configure volumes
VOLUME ["/app/logs", "/app/config", "/tmp"]

# Health check configuration
HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=40s \
    CMD curl --fail http://localhost:8081/actuator/health || exit 1

# Switch to non-root user
USER vcms:vcms

# Set entrypoint
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS $SECURITY_OPTS -jar app.jar" ]