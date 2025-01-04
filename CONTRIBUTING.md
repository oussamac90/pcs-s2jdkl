# Contributing to Vessel Call Management System

## Table of Contents
- [Introduction](#introduction)
- [Development Setup](#development-setup)
- [Development Workflow](#development-workflow)
- [Code Standards](#code-standards)
- [Testing Guidelines](#testing-guidelines)
- [Pull Request Process](#pull-request-process)
- [Release Process](#release-process)

## Introduction

Welcome to the Vessel Call Management System project. This document provides comprehensive guidelines for contributing to our codebase while maintaining high security and quality standards.

### Mission Statement
Our goal is to build a secure, reliable, and efficient port management system. Every contribution should align with our security-first development philosophy.

### Code of Conduct
Please read our [Code of Conduct](CODE_OF_CONDUCT.md) before contributing. We are committed to providing a welcoming and professional environment for all contributors.

## Development Setup

### Required Software
- Java Development Kit 17
- Node.js 18.x
- Docker Desktop
- Git 2.40+
- SonarQube Scanner
- OWASP Dependency Check Tool

### Security Configuration
1. Install SSL certificates for local development:
```bash
./scripts/setup-local-ssl.sh
```

2. Configure security scanning tools:
```bash
./scripts/configure-security-tools.sh
```

### Local Environment Setup
1. Clone the repository:
```bash
git clone https://github.com/organization/vessel-call-management.git
cd vessel-call-management
```

2. Set up the secure development environment:
```bash
./mvnw clean install
npm install
```

3. Configure the local database with security measures:
```bash
docker-compose up -d postgres
./scripts/init-secure-db.sh
```

## Development Workflow

### Branch Naming Convention
- Feature: `feature/VCMS-<ticket>-<description>`
- Bugfix: `bugfix/VCMS-<ticket>-<description>`
- Security: `security/VCMS-<ticket>-<description>`
- Hotfix: `hotfix/VCMS-<ticket>-<description>`

### Commit Message Standards
```
<type>(<scope>): <subject>

<body>

<footer>
```
Where:
- `type`: feat, fix, docs, style, refactor, test, chore, security
- `scope`: component affected
- `subject`: concise description
- `body`: detailed explanation
- `footer`: breaking changes, security implications

### Security-First Development
1. Never commit sensitive data
2. Use environment variables for configurations
3. Implement proper input validation
4. Follow the principle of least privilege
5. Document security implications

## Code Standards

### Java Code Standards
- Follow Oracle's Java Code Conventions
- Use Spring Security best practices
- Implement proper exception handling
- Apply secure coding patterns
- Document security considerations

### TypeScript/Angular Standards
- Follow Angular security best practices
- Implement CSRF protection
- Use safe template binding
- Sanitize user inputs
- Implement proper authentication guards

### API Security Standards
- Document authentication requirements
- Specify rate limiting rules
- Define data validation rules
- Document error responses
- Specify security headers

## Testing Guidelines

### Required Tests
1. Unit Tests
   - ≥85% code coverage
   - Security function validation
   - Input validation testing
   - Error handling verification

2. Integration Tests
   - API security testing
   - Authentication flow testing
   - Authorization checks
   - Data integrity validation

3. Security Tests
   - OWASP Top 10 verification
   - Penetration testing
   - Security headers validation
   - SSL/TLS configuration testing

## Pull Request Process

### PR Requirements
1. Fill out the security-enhanced PR template
2. Pass all automated checks:
   - SonarQube analysis
   - OWASP dependency check
   - Unit and integration tests
   - Security scans
   - Performance benchmarks

### Security Review Checklist
- [ ] No sensitive data exposure
- [ ] Proper input validation
- [ ] Secure authentication/authorization
- [ ] XSS prevention measures
- [ ] CSRF protection
- [ ] Secure dependencies
- [ ] Proper error handling
- [ ] Security documentation

### Code Review Process
1. Security review by security team
2. Technical review by two senior developers
3. Documentation review
4. Performance impact assessment
5. Final security validation

## Release Process

### Version Control
Follow semantic versioning (MAJOR.MINOR.PATCH):
- MAJOR: Breaking changes
- MINOR: New features
- PATCH: Bug fixes and security patches

### Security Release Process
1. Security patch preparation
2. Emergency review process
3. Hotfix deployment procedure
4. Security advisory publication
5. Post-deployment verification

### Deployment Security Checks
1. Dependency vulnerability scan
2. Security configuration validation
3. Sensitive data exposure check
4. Access control verification
5. SSL/TLS configuration test

### Documentation Requirements
- Update security documentation
- Document API security changes
- Update deployment security notes
- Record security implications
- Update security advisories

## Validation Rules

### Code Quality Gates
- SonarQube Quality Gate: PASS
- Test Coverage: ≥85%
- Security Vulnerabilities: 0 (High/Critical)
- Code Smells: 0 (Critical)
- Technical Debt: Must not increase

### Security Requirements
- OWASP Dependency Check: PASS
- Security Scan: No Critical Findings
- Authentication Tests: 100% PASS
- Authorization Tests: 100% PASS
- SSL/TLS Tests: 100% PASS

For additional information or questions, please contact the security team at security@vcms.org.