---
name: Bug Report
about: Report a bug in the Vessel Call Management System
title: '[BUG] '
labels: ['bug', 'triage']
assignees: []
---

## Bug Description
**Title**: [BUG] <component> - <brief description>

**Description**:
<!-- Provide a clear and detailed description of the bug -->

## Environment
**Deployment Environment**:
<!-- Select the environment where the bug was encountered -->
- [ ] Development
- [ ] Staging
- [ ] Production
- [ ] DR Site

**Infrastructure**:
<!-- Select the infrastructure where the bug was found -->
- [ ] Azure Cloud
- [ ] On-Premises DR
- [ ] Hybrid

**Version**: <!-- e.g., 1.2.3 -->

**Browser**: <!-- If frontend issue, specify browser name and version -->

## Steps to Reproduce
1. <!-- First step -->
2. <!-- Second step -->
3. <!-- Additional steps as needed -->

## Expected Behavior
<!-- Describe what should happen -->

## Actual Behavior
<!-- Describe what actually happens -->

## Impact Assessment
**Severity**:
<!-- Select the severity based on system impact -->
- [ ] Critical - System Unusable (>90% Resource Usage)
- [ ] High - Major Feature Impact (>75% Resource Usage)
- [ ] Medium - Minor Feature Impact (>50% Resource Usage)
- [ ] Low - Cosmetic Issue (<50% Resource Usage)

**Affected Users**:
<!-- Select the impacted user groups -->
- [ ] All Users
- [ ] Port Authority Users
- [ ] Vessel Agents
- [ ] Service Providers
- [ ] System Administrators

**Affected Component**:
<!-- Select the affected system component -->
- [ ] Frontend - Dashboard
- [ ] Frontend - Vessel Calls
- [ ] Frontend - Berth Management
- [ ] Frontend - Service Booking
- [ ] Frontend - Clearance
- [ ] Backend - Vessel Management
- [ ] Backend - Berth Allocation
- [ ] Backend - Service Coordination
- [ ] Backend - Clearance Processing
- [ ] Infrastructure - AKS Cluster
- [ ] Infrastructure - Azure Database
- [ ] Infrastructure - Azure Cache
- [ ] Infrastructure - Azure Storage
- [ ] Infrastructure - On-Premises DR
- [ ] Other

**Performance Impact**:
<!-- Select the performance impact level -->
- [ ] Response Time > 5s (Critical)
- [ ] Response Time 2s-5s (High)
- [ ] Response Time 1s-2s (Medium)
- [ ] Response Time < 1s (Low)

## Debug Information
**Error Logs**:
```
<!-- Paste relevant logs from Azure Monitor or ELK Stack -->
```

**Metrics**:
```
<!-- Paste relevant metrics from Azure Monitor -->
```

**Stack Trace**:
```
<!-- Paste error stack trace if available -->
```

**Screenshots**:
<!-- Attach screenshots demonstrating the issue -->

## Additional Context
<!-- Add any additional context about the problem here -->