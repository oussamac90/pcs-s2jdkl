# Vessel Call Management System (VCMS)

[![Build Status](https://github.com/your-org/vcms/workflows/CI/badge.svg)](https://github.com/your-org/vcms/actions)
[![Code Coverage](https://sonarcloud.io/api/project_badges/measure?project=vcms&metric=coverage)](https://sonarcloud.io/dashboard?id=vcms)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A comprehensive Port Community System module for managing vessel calls, berth allocations, and port services.

## Project Overview

### Executive Summary
The Vessel Call Management System (VCMS) is a state-of-the-art solution designed to digitize and streamline vessel arrival, berthing, and departure processes. It provides real-time visibility, automated workflows, and intelligent resource allocation for port authorities, vessel agents, service providers, and regulatory bodies.

### Key Features
- Electronic pre-arrival notification processing
- Automated berth allocation with conflict resolution
- Digital service coordination and booking
- Real-time vessel tracking and status updates
- Regulatory compliance and clearance automation
- Comprehensive reporting and analytics

### System Architecture
- Monolithic application using Spring Boot (Backend) and Angular (Frontend)
- Cloud-native deployment on Azure Kubernetes Service
- Event-driven architecture with WebSocket support
- Integrated monitoring and observability

### Technology Stack
- Backend: Java 17 LTS, Spring Boot 3.1.x
- Frontend: Angular 16.x, TypeScript 4.9+
- Database: PostgreSQL 14
- Cache: Redis 6
- Infrastructure: Azure Kubernetes Service, Azure Monitor

## Prerequisites

Ensure you have the following installed:

- Java Development Kit (JDK) 17 LTS
- Node.js (>=18.0.0) and npm (>=9.0.0)
- Docker and Docker Compose
- PostgreSQL 14
- Redis 6
- Azure CLI

## Getting Started

### Local Development Setup

1. Clone the repository:
```bash
git clone https://github.com/your-org/vcms.git
cd vcms
```

2. Install dependencies:

Backend:
```bash
./gradlew build
```

Frontend:
```bash
cd frontend
npm install
```

3. Configure environment variables:
```bash
cp .env.example .env
# Edit .env with your configuration
```

4. Run database migrations:
```bash
./gradlew flywayMigrate
```

5. Start the application:

Using Docker:
```bash
docker-compose up
```

Manual start:
```bash
# Backend
./gradlew bootRun

# Frontend
cd frontend
ng serve
```

The application will be available at:
- Frontend: http://localhost:4200
- Backend API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html

## Deployment

### Azure Infrastructure Setup

1. Create required Azure resources:
```bash
az group create --name vcms-rg --location eastus
az aks create --resource-group vcms-rg --name vcms-aks --node-count 3
```

2. Configure Kubernetes credentials:
```bash
az aks get-credentials --resource-group vcms-rg --name vcms-aks
```

3. Deploy the application:
```bash
kubectl apply -f k8s/
```

### Monitoring Setup

1. Enable Azure Monitor:
```bash
az monitor log-analytics workspace create --resource-group vcms-rg --workspace-name vcms-logs
```

2. Configure application insights:
```bash
az monitor app-insights component create --app vcms-insights --location eastus --resource-group vcms-rg
```

## Documentation

- [API Documentation](docs/api/README.md)
- [User Guide](docs/user/README.md)
- [Architecture Guide](docs/architecture/README.md)
- [Security Guide](docs/security/README.md)

For detailed information about specific topics:
- [Contributing Guidelines](CONTRIBUTING.md)
- [Changelog](CHANGELOG.md)
- [License](LICENSE)

## Support

- Report bugs and feature requests via [GitHub Issues](https://github.com/your-org/vcms/issues)
- Join discussions in [GitHub Discussions](https://github.com/your-org/vcms/discussions)
- For security issues, please review our [Security Policy](SECURITY.md)

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.