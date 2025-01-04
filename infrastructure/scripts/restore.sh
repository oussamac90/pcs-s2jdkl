#!/bin/bash

# Vessel Call Management System - Database Restore Script
# Version: 1.0
# Dependencies:
# - postgresql-client: 14
# - openssl: 1.1.1
# - azure-cli: 2.0

set -euo pipefail

# Global Variables
RESTORE_ROOT="/var/restore/vcms"
AZURE_STORAGE_CONTAINER="database-backups"
BACKUP_CHECKSUM_FILE="backup_checksums.sha256"
RESTORE_LOG_FILE="/var/log/vcms/restore.log"
MAX_RETRY_ATTEMPTS=3

# Logging function with timestamps
log() {
    local level=$1
    shift
    echo "$(date '+%Y-%m-%d %H:%M:%S') [$level] $*" | tee -a "$RESTORE_LOG_FILE"
}

# Validate environment and dependencies
validate_environment() {
    log "INFO" "Validating environment and dependencies..."

    # Check required environment variables
    local required_vars=("AZURE_STORAGE_ACCOUNT" "BACKUP_ENCRYPTION_KEY" "DATABASE_URL" "DB_USERNAME" "DB_PASSWORD")
    for var in "${required_vars[@]}"; do
        if [[ -z "${!var:-}" ]]; then
            log "ERROR" "Required environment variable $var is not set"
            return 1
        fi
    done

    # Check required tools
    local required_tools=("pg_restore" "openssl" "az")
    for tool in "${required_tools[@]}"; do
        if ! command -v "$tool" >/dev/null 2>&1; then
            log "ERROR" "Required tool $tool is not installed"
            return 1
        fi
    done

    # Verify PostgreSQL client version
    local pg_version
    pg_version=$(pg_restore --version | grep -oP '\d+' | head -1)
    if [[ "$pg_version" -lt 14 ]]; then
        log "ERROR" "PostgreSQL client version must be 14 or higher"
        return 1
    fi

    # Verify Azure CLI authentication
    if ! az account show >/dev/null 2>&1; then
        log "ERROR" "Azure CLI is not authenticated"
        return 1
    }

    # Create restore directory if it doesn't exist
    mkdir -p "$RESTORE_ROOT"
    
    # Verify write permissions
    if ! touch "$RESTORE_ROOT/.write_test" 2>/dev/null; then
        log "ERROR" "No write permission in restore directory"
        return 1
    fi
    rm -f "$RESTORE_ROOT/.write_test"

    log "INFO" "Environment validation completed successfully"
    return 0
}

# Download backup from Azure Storage with retry mechanism
download_from_azure() {
    local backup_file_name=$1
    local checksum=$2
    local attempt=1
    local download_path="$RESTORE_ROOT/$backup_file_name"

    while [[ $attempt -le $MAX_RETRY_ATTEMPTS ]]; do
        log "INFO" "Downloading backup file (attempt $attempt/$MAX_RETRY_ATTEMPTS)..."
        
        if az storage blob download \
            --account-name "$AZURE_STORAGE_ACCOUNT" \
            --container-name "$AZURE_STORAGE_CONTAINER" \
            --name "$backup_file_name" \
            --file "$download_path" 2>/dev/null; then
            
            # Verify checksum
            local calculated_checksum
            calculated_checksum=$(sha256sum "$download_path" | cut -d' ' -f1)
            if [[ "$calculated_checksum" == "$checksum" ]]; then
                log "INFO" "Backup file downloaded and verified successfully"
                echo "$download_path"
                return 0
            else
                log "ERROR" "Checksum verification failed"
                rm -f "$download_path"
            fi
        fi

        log "WARN" "Download attempt $attempt failed, retrying..."
        sleep $((2 ** attempt))
        ((attempt++))
    done

    log "ERROR" "Failed to download backup file after $MAX_RETRY_ATTEMPTS attempts"
    return 1
}

# Decrypt backup file using AES-256-CBC
decrypt_backup() {
    local encrypted_file=$1
    local decrypted_file="${encrypted_file%.enc}"
    
    log "INFO" "Decrypting backup file..."
    
    # Extract IV from first 16 bytes of file
    local iv_file="$RESTORE_ROOT/iv.bin"
    head -c 16 "$encrypted_file" > "$iv_file"
    
    # Decrypt the file (excluding IV)
    if tail -c +17 "$encrypted_file" | \
        openssl enc -aes-256-cbc -d \
        -K "$(echo -n "$BACKUP_ENCRYPTION_KEY" | xxd -p -c 64)" \
        -iv "$(xxd -p "$iv_file")" \
        -out "$decrypted_file"; then
        
        log "INFO" "Backup file decrypted successfully"
        rm -f "$iv_file"
        echo "$decrypted_file"
        return 0
    else
        log "ERROR" "Failed to decrypt backup file"
        rm -f "$iv_file" "$decrypted_file"
        return 1
    fi
}

# Perform database restore
perform_restore() {
    local backup_file=$1
    local target_database=$2
    local parallel_jobs=${3:-4}
    
    log "INFO" "Starting database restore process..."
    
    # Create database if it doesn't exist
    if ! psql -h "${DATABASE_URL}" -U "${DB_USERNAME}" -d postgres -c "SELECT 1 FROM pg_database WHERE datname = '$target_database'" | grep -q 1; then
        log "INFO" "Creating database $target_database..."
        createdb -h "${DATABASE_URL}" -U "${DB_USERNAME}" "$target_database"
    fi
    
    # Terminate existing connections
    psql -h "${DATABASE_URL}" -U "${DB_USERNAME}" -d postgres <<EOF
SELECT pg_terminate_backend(pid) 
FROM pg_stat_activity 
WHERE datname = '$target_database' 
AND pid <> pg_backend_pid();
EOF
    
    # Perform restore
    if pg_restore \
        -h "${DATABASE_URL}" \
        -U "${DB_USERNAME}" \
        -d "$target_database" \
        -j "$parallel_jobs" \
        --clean \
        --if-exists \
        --no-owner \
        --no-privileges \
        "$backup_file"; then
        
        log "INFO" "Database restore completed successfully"
        return 0
    else
        log "ERROR" "Database restore failed"
        return 1
    fi
}

# Main execution
main() {
    local backup_file_name=$1
    local target_database=$2
    
    log "INFO" "Starting restore process for backup: $backup_file_name"
    
    # Validate environment
    if ! validate_environment; then
        log "ERROR" "Environment validation failed"
        exit 1
    fi
    
    # Download checksum file
    local checksum
    checksum=$(az storage blob download \
        --account-name "$AZURE_STORAGE_ACCOUNT" \
        --container-name "$AZURE_STORAGE_CONTAINER" \
        --name "$BACKUP_CHECKSUM_FILE" \
        --query "content" -o tsv | grep "$backup_file_name" | cut -d' ' -f1)
    
    # Download and decrypt backup
    local downloaded_file
    downloaded_file=$(download_from_azure "$backup_file_name" "$checksum") || exit 1
    
    local decrypted_file
    decrypted_file=$(decrypt_backup "$downloaded_file") || exit 1
    
    # Perform restore
    if ! perform_restore "$decrypted_file" "$target_database"; then
        log "ERROR" "Restore process failed"
        exit 1
    fi
    
    # Cleanup
    rm -f "$downloaded_file" "$decrypted_file"
    log "INFO" "Restore process completed successfully"
}

# Script entry point
if [[ $# -ne 2 ]]; then
    echo "Usage: $0 <backup_file_name> <target_database>"
    exit 1
fi

main "$1" "$2"