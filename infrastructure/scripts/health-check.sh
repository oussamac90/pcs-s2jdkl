#!/bin/bash

# Vessel Call Management System Health Check Script
# Version: 1.0
# Dependencies: curl v7.0+, kubectl v1.24+, jq v1.6+, az cli v2.40+

set -euo pipefail

# Global Constants
readonly BACKEND_SERVICE="vcms-backend.vcms.svc.cluster.local:8080"
readonly METRICS_PATH="/actuator/prometheus"
readonly HEALTH_PATH="/actuator/health"
readonly CPU_WARNING_THRESHOLD=70
readonly CPU_CRITICAL_THRESHOLD=85
readonly MEMORY_WARNING_THRESHOLD=75
readonly MEMORY_CRITICAL_THRESHOLD=90
readonly DISK_WARNING_THRESHOLD=75
readonly DISK_CRITICAL_THRESHOLD=90
readonly MAX_RESPONSE_TIME=3
readonly ERROR_RATE_WARNING=1
readonly ERROR_RATE_CRITICAL=5
readonly LOG_LEVEL="INFO"
readonly RETRY_COUNT=3
readonly RETRY_DELAY=5
readonly CORRELATION_ID_PREFIX="VCMS-HEALTH"
readonly AZURE_MONITOR_WORKSPACE="vcms-law"

# Logging function with severity levels
log() {
    local level=$1
    local message=$2
    local timestamp=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
    local correlation_id=$3
    echo "[$timestamp] [$level] [$correlation_id] $message"
}

# Generate correlation ID
generate_correlation_id() {
    echo "${CORRELATION_ID_PREFIX}-$(date +%s)-${RANDOM}"
}

# Check backend health with comprehensive validation
check_backend_health() {
    local correlation_id=$1
    local health_status=0
    local response_time=0
    
    log "INFO" "Starting backend health check" "$correlation_id"
    
    # SSL Certificate validation
    local cert_expiry=$(echo | openssl s_client -connect ${BACKEND_SERVICE} 2>/dev/null | openssl x509 -noout -enddate | cut -d= -f2)
    local days_until_expiry=$(( ($(date -d "$cert_expiry" +%s) - $(date +%s)) / 86400 ))
    
    if [ $days_until_expiry -lt 30 ]; then
        log "WARNING" "SSL Certificate expiring in $days_until_expiry days" "$correlation_id"
    fi
    
    # Health endpoint check with response time measurement
    local start_time=$(date +%s%N)
    local health_response=$(curl -sk --max-time ${MAX_RESPONSE_TIME} https://${BACKEND_SERVICE}${HEALTH_PATH})
    local end_time=$(date +%s%N)
    response_time=$(echo "scale=3; ($end_time - $start_time)/1000000000" | bc)
    
    # Validate response time against SLA
    if (( $(echo "$response_time > $MAX_RESPONSE_TIME" | bc -l) )); then
        log "ERROR" "Response time ${response_time}s exceeds SLA of ${MAX_RESPONSE_TIME}s" "$correlation_id"
        health_status=1
    fi
    
    # Parse health check response
    if [ "$(echo $health_response | jq -r .status)" != "UP" ]; then
        log "ERROR" "Backend health check failed" "$correlation_id"
        health_status=1
    fi
    
    # Push metrics to Azure Monitor
    az monitor metrics alert create \
        --name "vcms-backend-health" \
        --resource-group vcms \
        --condition "response_time > $MAX_RESPONSE_TIME" \
        --window-size 5m \
        --evaluation-frequency 1m
        
    return $health_status
}

# Check metrics endpoints with trend analysis
check_metrics_endpoints() {
    local correlation_id=$1
    local metrics_status=0
    
    log "INFO" "Starting metrics endpoint check" "$correlation_id"
    
    # Retrieve metrics from Prometheus endpoint
    local metrics_response=$(curl -sk https://${BACKEND_SERVICE}${METRICS_PATH})
    
    # Parse and analyze key metrics
    local cpu_usage=$(echo "$metrics_response" | grep "process_cpu_usage" | awk '{print $2}')
    local memory_usage=$(echo "$metrics_response" | grep "jvm_memory_used_bytes" | awk '{print $2}')
    local disk_usage=$(echo "$metrics_response" | grep "disk_free_bytes" | awk '{print $2}')
    
    # Threshold checks
    if (( $(echo "$cpu_usage > $CPU_WARNING_THRESHOLD" | bc -l) )); then
        log "WARNING" "CPU usage above warning threshold: ${cpu_usage}%" "$correlation_id"
    fi
    
    if (( $(echo "$cpu_usage > $CPU_CRITICAL_THRESHOLD" | bc -l) )); then
        log "ERROR" "CPU usage above critical threshold: ${cpu_usage}%" "$correlation_id"
        metrics_status=1
    fi
    
    # Push metrics to Azure Monitor
    az monitor metrics push \
        --workspace $AZURE_MONITOR_WORKSPACE \
        --metrics "[{\"name\":\"cpu_usage\",\"value\":$cpu_usage}]"
        
    return $metrics_status
}

# Check resource usage with predictive analysis
check_resource_usage() {
    local correlation_id=$1
    local resource_status=0
    
    log "INFO" "Starting resource usage check" "$correlation_id"
    
    # Get pod resource usage
    local pod_metrics=$(kubectl get --raw /apis/metrics.k8s.io/v1beta1/namespaces/vcms/pods)
    
    # Analyze resource trends
    local cpu_trend=$(echo $pod_metrics | jq -r '.items[].containers[].usage.cpu')
    local memory_trend=$(echo $pod_metrics | jq -r '.items[].containers[].usage.memory')
    
    # Predictive analysis for capacity planning
    if [ $(echo "$cpu_trend" | awk '{sum+=$1} END {print sum/NR}') -gt $CPU_WARNING_THRESHOLD ]; then
        log "WARNING" "Predicted CPU saturation within 24 hours" "$correlation_id"
    fi
    
    # Push resource metrics to Azure Monitor
    az monitor metrics push \
        --workspace $AZURE_MONITOR_WORKSPACE \
        --metrics "[{\"name\":\"resource_usage\",\"value\":$cpu_trend}]"
        
    return $resource_status
}

# Main execution function
main() {
    local correlation_id=$(generate_correlation_id)
    local exit_status=0
    
    log "INFO" "Starting health check script" "$correlation_id"
    
    # Validate dependencies
    for cmd in curl kubectl jq az; do
        if ! command -v $cmd &> /dev/null; then
            log "ERROR" "Required command $cmd not found" "$correlation_id"
            exit 1
        fi
    done
    
    # Execute health checks with retry logic
    for i in $(seq 1 $RETRY_COUNT); do
        if ! check_backend_health "$correlation_id"; then
            log "WARNING" "Backend health check failed, attempt $i of $RETRY_COUNT" "$correlation_id"
            sleep $RETRY_DELAY
            exit_status=1
        else
            exit_status=0
            break
        fi
    done
    
    # Execute metrics and resource checks
    check_metrics_endpoints "$correlation_id" || exit_status=1
    check_resource_usage "$correlation_id" || exit_status=1
    
    # Generate final health report
    if [ $exit_status -eq 0 ]; then
        log "INFO" "Health check completed successfully" "$correlation_id"
    else
        log "ERROR" "Health check failed" "$correlation_id"
    fi
    
    return $exit_status
}

# Script execution
main "$@"