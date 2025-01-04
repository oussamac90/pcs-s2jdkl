# Vessel Call Management System - Backend Service

## Overview

The Vessel Call Management System (VCMS) backend is a Spring Boot-based monolithic application designed to digitize and streamline vessel arrival, berthing, and departure processes. This service provides REST APIs, WebSocket notifications, and secure authentication for managing port operations.

## Technical Stack

- **Java Version**: JDK 17
- **Framework**: Spring Boot 3.1.x
- **Build Tool**: Gradle 8.x
- **Database**: PostgreSQL 14
- **Cache**: Redis 6.x
- **Message Broker**: RabbitMQ 3.x
- **Container Runtime**: Docker 24.x
- **Orchestration**: Kubernetes 1.26+

## Prerequisites

### Required Software

- JDK 17 or higher
- Gradle 8.x
- PostgreSQL 14
- Redis 6.x
- RabbitMQ 3.x
- Docker 24.x
- Kubernetes 1.26+ (for production deployment)

### Development Tools

- IntelliJ IDEA 2023.2+ (recommended) or Eclipse
- Postman for API testing
- Git for version control
- Docker Desktop for local container testing

## Development Setup

### Environment Configuration

1. Clone the repository:
```bash
git clone <repository-url>
cd src/backend
```

2. Configure environment variables:
```bash
export SPRING_PROFILES_ACTIVE=dev
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=vcms
export REDIS_HOST=localhost
export REDIS_PORT=6379
```

3. Database setup:
```bash
# Create database
psql -U postgres -c "CREATE DATABASE vcms;"

# Run migrations
./gradlew flywayMigrate
```

### Building the Application

```bash
# Clean and build
./gradlew clean build

# Run tests
./gradlew test

# Generate test coverage report
./gradlew jacocoTestReport
```

### Running Locally

```bash
# Run with Gradle
./gradlew bootRun

# Run with specific profile
./gradlew bootRun --args='--spring.profiles.active=dev'
```

## Security Implementation

### Authentication & Authorization

- OAuth2/JWT implementation for secure authentication
- Role-Based Access Control (RBAC) with the following roles:
  - PORT_AUTHORITY
  - VESSEL_AGENT
  - SERVICE_PROVIDER
  - SYSTEM_ADMIN

### Security Configuration

1. JWT Configuration in application.yml:
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${JWT_ISSUER_URI}
          jws-algorithms: RS256
```

2. Enable security features:
```yaml
security:
  require-ssl: true
  encryption:
    enabled: true
    algorithm: AES-256
```

## API Documentation

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- API Docs: `http://localhost:8080/v3/api-docs`

## Deployment

### Container Build

```bash
# Build Docker image
docker build -t vcms-backend:latest .

# Run container locally
docker run -p 8080:8080 vcms-backend:latest
```

### Kubernetes Deployment

```bash
# Apply Kubernetes configurations
kubectl apply -f k8s/

# Verify deployment
kubectl get pods -n vcms
```

### Environment-Specific Deployments

- Development: Azure Dev/Test Labs
- Staging: Azure App Service
- Production: Azure Kubernetes Service
- DR Site: On-premises datacenter

## Monitoring & Logging

### Health Checks

- Actuator endpoints: `http://localhost:8080/actuator`
- Health status: `http://localhost:8080/actuator/health`
- Metrics: `http://localhost:8080/actuator/metrics`

### Logging Configuration

```yaml
logging:
  level:
    root: INFO
    com.pcs.vcms: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

## Troubleshooting

### Common Issues

1. Database Connection:
```bash
# Check database connectivity
psql -h localhost -U postgres -d vcms -c "\dt"
```

2. Redis Connection:
```bash
# Test Redis connection
redis-cli ping
```

3. Application Logs:
```bash
# View application logs
kubectl logs -f deployment/vcms-backend -n vcms
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit changes
4. Push to the branch
5. Create a Pull Request

### Code Style

- Follow Google Java Style Guide
- Use provided checkstyle configuration
- Maintain test coverage above 80%

## License

Proprietary - All rights reserved

## Support

Contact the development team:
- Email: dev-team@pcs.com
- Slack: #vcms-backend-support

## Links

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/3.1.x/reference/html/)
- [Kubernetes Documentation](https://kubernetes.io/docs/home/)
- [Azure Documentation](https://docs.microsoft.com/en-us/azure/)

## Maintainers

- Backend Development Team
- DevOps Team
- Security Team

Last Updated: [Version Controlled]