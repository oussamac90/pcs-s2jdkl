#!/bin/bash

# Vessel Call Management System - Secret Rotation Script
# Version: 1.0.0
# Purpose: Automated rotation of Kubernetes secrets and credentials
# Dependencies: kubectl v1.24+, az cli 2.40+, openssl 1.1.1+

set -euo pipefail

# Global variables
NAMESPACE="${NAMESPACE:-vcms}"
SECRET_NAME="${SECRET_NAME:-vcms-secrets}"
BACKUP_DIR="${BACKUP_DIR:-/tmp/secret-backups}"
LOG_FILE="${LOG_FILE:-/var/log/vcms/secret-rotation.log}"
ROTATION_INTERVAL="${ROTATION_INTERVAL:-90}"
MAX_RETRIES="${MAX_RETRIES:-3}"
HEALTH_CHECK_TIMEOUT="${HEALTH_CHECK_TIMEOUT:-300}"
CLEANUP_RETENTION="${CLEANUP_RETENTION:-7}"

# Logging function
log() {
    local level="$1"
    local message="$2"
    echo "$(date '+%Y-%m-%d %H:%M:%S') [$level] $message" | tee -a "$LOG_FILE"
}

# Initialize logging
mkdir -p "$(dirname "$LOG_FILE")"
touch "$LOG_FILE"
chmod 600 "$LOG_FILE"

# Generate new credentials
generate_new_credentials() {
    local credential_type="$1"
    local length=32
    local result=""

    case "$credential_type" in
        "database")
            # Generate complex database password
            result=$(openssl rand -base64 32 | tr -dc 'a-zA-Z0-9!@#$%^&*()' | head -c 32)
            ;;
        "jwt")
            # Generate JWT signing key (512 bits)
            result=$(openssl rand -base64 64)
            ;;
        "api")
            # Generate API key
            result=$(openssl rand -hex 32)
            ;;
        "ssl")
            # Generate SSL private key and CSR
            local domain="vcms.example.com"
            openssl req -new -newkey rsa:4096 -nodes -keyout /tmp/ssl.key -out /tmp/ssl.csr -subj "/CN=$domain"
            result=$(cat /tmp/ssl.key | base64 -w 0)
            rm -f /tmp/ssl.key /tmp/ssl.csr
            ;;
        *)
            log "ERROR" "Unknown credential type: $credential_type"
            return 1
            ;;
    esac

    echo "$result" | base64 -w 0
}

# Backup current secrets
backup_current_secrets() {
    local backup_path="${BACKUP_DIR}/$(date +%Y%m%d_%H%M%S)"
    local checksum_file="${backup_path}.sha256"
    
    log "INFO" "Creating backup at $backup_path"
    
    mkdir -p "$BACKUP_DIR"
    chmod 700 "$BACKUP_DIR"

    # Export current secrets
    kubectl get secret "$SECRET_NAME" -n "$NAMESPACE" -o yaml > "${backup_path}.yaml"
    
    # Encrypt backup
    openssl enc -aes-256-cbc -salt -in "${backup_path}.yaml" -out "${backup_path}.enc" -pass file:/etc/vcms/backup.key
    
    # Generate checksum
    sha256sum "${backup_path}.enc" > "$checksum_file"
    
    # Cleanup unencrypted backup
    rm -f "${backup_path}.yaml"
    
    # Verify backup
    if ! openssl enc -d -aes-256-cbc -in "${backup_path}.enc" -pass file:/etc/vcms/backup.key > /dev/null 2>&1; then
        log "ERROR" "Backup verification failed"
        return 1
    fi

    # Cleanup old backups
    find "$BACKUP_DIR" -type f -mtime +"$CLEANUP_RETENTION" -delete

    log "INFO" "Backup completed successfully"
    return 0
}

# Update Kubernetes secrets
update_kubernetes_secrets() {
    local temp_secret="/tmp/new-secret.yaml"
    local retry_count=0
    
    log "INFO" "Generating new credentials"
    
    # Generate new credentials
    local new_db_password=$(generate_new_credentials "database")
    local new_jwt_secret=$(generate_new_credentials "jwt")
    local new_api_key=$(generate_new_credentials "api")
    local new_ssl_cert=$(generate_new_credentials "ssl")

    # Create temporary secret file
    cat > "$temp_secret" <<EOF
apiVersion: v1
kind: Secret
metadata:
    name: $SECRET_NAME
    namespace: $NAMESPACE
type: Opaque
data:
    database_password: $new_db_password
    jwt_secret: $new_jwt_secret
    api_key: $new_api_key
    ssl_certificate: $new_ssl_cert
EOF

    # Update secret with retry logic
    while [ $retry_count -lt "$MAX_RETRIES" ]; do
        if kubectl replace -f "$temp_secret" --namespace "$NAMESPACE"; then
            log "INFO" "Secret updated successfully"
            rm -f "$temp_secret"
            return 0
        fi
        
        retry_count=$((retry_count + 1))
        log "WARN" "Secret update failed, attempt $retry_count of $MAX_RETRIES"
        sleep 5
    done

    log "ERROR" "Failed to update secrets after $MAX_RETRIES attempts"
    rm -f "$temp_secret"
    return 1
}

# Update Azure resources
update_azure_resources() {
    log "INFO" "Updating Azure resources"
    
    # Update database password
    if ! az postgres server update --resource-group vcms-rg --name vcms-db --admin-password "$new_db_password"; then
        log "ERROR" "Failed to update database password"
        return 1
    fi

    # Update Key Vault secrets
    if ! az keyvault secret set --vault-name vcms-kv --name db-password --value "$new_db_password"; then
        log "ERROR" "Failed to update Key Vault secrets"
        return 1
    fi

    log "INFO" "Azure resources updated successfully"
    return 0
}

# Validate rotation
validate_rotation() {
    log "INFO" "Validating secret rotation"
    
    # Check pod health
    local start_time=$(date +%s)
    local current_time=0
    local pods_healthy=false
    
    while [ $((current_time - start_time)) -lt "$HEALTH_CHECK_TIMEOUT" ]; do
        if kubectl get pods -n "$NAMESPACE" -l app=vcms -o jsonpath='{.items[*].status.containerStatuses[*].ready}' | grep -q false; then
            log "WARN" "Pods still starting up, waiting..."
            sleep 10
            current_time=$(date +%s)
        else
            pods_healthy=true
            break
        fi
    done

    if [ "$pods_healthy" = false ]; then
        log "ERROR" "Pod health check failed after timeout"
        return 1
    fi

    # Verify database connectivity
    if ! kubectl exec -n "$NAMESPACE" deploy/vcms-backend -- pg_isready -h vcms-db; then
        log "ERROR" "Database connectivity check failed"
        return 1
    }

    log "INFO" "Rotation validation completed successfully"
    return 0
}

# Main rotation process
main() {
    log "INFO" "Starting secret rotation process"
    
    # Check prerequisites
    for cmd in kubectl az openssl; do
        if ! command -v "$cmd" >/dev/null 2>&1; then
            log "ERROR" "Required command not found: $cmd"
            exit 1
        fi
    done

    # Create backup
    if ! backup_current_secrets; then
        log "ERROR" "Backup failed, aborting rotation"
        exit 1
    fi

    # Update secrets
    if ! update_kubernetes_secrets; then
        log "ERROR" "Secret update failed"
        exit 1
    fi

    # Update Azure resources
    if ! update_azure_resources; then
        log "ERROR" "Azure resource update failed"
        exit 1
    }

    # Trigger pod rollout
    log "INFO" "Triggering pod rollout"
    kubectl rollout restart deployment/vcms-backend -n "$NAMESPACE"

    # Validate rotation
    if ! validate_rotation; then
        log "ERROR" "Validation failed, initiating rollback"
        # Implement rollback logic here
        exit 1
    fi

    log "INFO" "Secret rotation completed successfully"
    exit 0
}

# Execute main function
main "$@"