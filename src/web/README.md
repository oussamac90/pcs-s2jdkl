# Vessel Call Management System Frontend

## Overview

The Vessel Call Management System (VCMS) frontend is an enterprise-grade Angular 16 application designed for maritime port operations. It provides a comprehensive user interface for vessel arrival management, berth planning, service coordination, and clearance processing.

![VCMS Architecture](docs/images/architecture.png)

## Prerequisites

- Node.js >= 18.0.0
- NPM >= 9.0.0
- Angular CLI ^16.0.0 (`npm install -g @angular/cli@16`)
- Docker >= 20.10.0 (for containerized deployment)
- Azure CLI >= 2.40.0 (for cloud deployment)

## Quick Start

```bash
# Clone the repository
git clone <repository-url>
cd src/web

# Install dependencies
npm install

# Start development server
npm start

# Access the application
open http://localhost:4200
```

## Installation

### Development Environment Setup

1. Clone the repository:
```bash
git clone <repository-url>
cd src/web
```

2. Install dependencies:
```bash
npm install
```

3. Configure environment variables:
```bash
cp src/environments/environment.template.ts src/environments/environment.ts
# Edit environment.ts with appropriate values
```

4. Set up security credentials:
```bash
# Configure Azure AD credentials for authentication
cp auth.config.template.ts src/app/config/auth.config.ts
# Edit auth.config.ts with OAuth2 settings
```

## Development

### Maritime Domain Standards

- Follow TypeScript strict mode guidelines
- Implement reactive patterns using RxJS
- Use NgRx for state management
- Follow SOLID principles
- Implement proper error handling

### Code Structure

```
src/
├── app/
│   ├── core/              # Singleton services, guards
│   ├── features/          # Feature modules
│   │   ├── vessel-calls/  # Vessel management
│   │   ├── berth-plan/    # Berth planning
│   │   └── services/      # Port services
│   ├── shared/           # Shared components, pipes
│   └── app.module.ts
├── assets/
└── environments/
```

### Development Server

```bash
# Start development server with hot reload
npm start

# Start with mock data
npm run start:mock

# Start with specific port authority config
npm run start:custom -- --configuration=port-authority-1
```

## Building

### Development Build

```bash
# Development build
npm run build

# Production build
npm run build:prod

# Specific port authority build
npm run build:custom -- --configuration=port-authority-1
```

### Docker Build

```bash
# Build Docker image
docker build -t vcms-frontend:latest .

# Run Docker container
docker run -p 8080:80 vcms-frontend:latest
```

## Deployment

### Azure Kubernetes Service (AKS)

1. Build production image:
```bash
docker build -t vcms-frontend:prod -f Dockerfile.prod .
```

2. Push to Azure Container Registry:
```bash
az acr login --name <registry-name>
docker tag vcms-frontend:prod <registry-name>.azurecr.io/vcms-frontend:prod
docker push <registry-name>.azurecr.io/vcms-frontend:prod
```

3. Deploy to AKS:
```bash
kubectl apply -f kubernetes/frontend-deployment.yaml
```

### Environment Configuration

- Production: `environment.prod.ts`
- Staging: `environment.staging.ts`
- Development: `environment.ts`

## Testing

### Unit Testing

```bash
# Run unit tests
npm test

# Run with coverage
npm run test:coverage

# Run specific test suite
npm test -- --include=vessel-calls
```

### E2E Testing

```bash
# Run e2e tests
npm run e2e

# Run specific e2e suite
npm run e2e -- --suite=vessel-management
```

### Performance Testing

```bash
# Run Lighthouse audit
npm run lighthouse

# Run bundle analysis
npm run analyze
```

## Security

### Authentication

- OAuth2/JWT implementation with Azure AD
- Role-based access control (RBAC)
- Session management
- Token refresh mechanisms

### Security Best Practices

- Regular dependency updates
- Security headers configuration
- XSS prevention
- CSRF protection
- Content Security Policy

## Accessibility

- WCAG 2.1 Level AA compliance
- Screen reader compatibility
- Keyboard navigation
- High contrast support
- Focus management

## Performance Optimization

- Lazy loading of modules
- Virtual scrolling for large datasets
- Progressive web app (PWA) capabilities
- Image optimization
- Bundle optimization

## Contributing

1. Follow Angular style guide
2. Maintain test coverage > 80%
3. Document new features
4. Update CHANGELOG.md
5. Submit pull request

## License

[Specify License]

## Support

For technical support, contact:
- Email: support@vcms.com
- Documentation: [Internal Wiki Link]
- Issue Tracker: [JIRA Link]

## Version History

See [CHANGELOG.md](CHANGELOG.md)