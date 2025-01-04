#!/bin/bash

# Vessel Call Management System Backup Script
# Version: 1.0.0
# Dependencies:
# - postgresql-client: 14
# - openssl: 1.1.1
# - azure-cli: 2.0
# - pigz: 2.6

set -euo pipefail

# Global Variables
BACKUP_ROOT="/var/backup/vcms"
AZURE_STORAGE_CONTAINER="database-backups"
BACKUP_RETENTION_DAYS=7
FULL_BACKUP_RETENTION_WEEKS=4
MAX_RETRY_ATTEMPTS=3
MIN_DISK_SPACE_GB=50
LOG_FILE="/var/log/vcms/backup.log"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_TYPE=${1:-"incremental"}  # Default to incremental backup

# Load database configuration from application-prod.yml
DB_URL=$(grep "url:" ../src/backend/src/main/resources/application-prod.yml | awk '{print $2}')
DB_USER=$(grep "username:" ../src/backend/src/main/resources/application-prod.yml | awk '{print $2}')
DB_PASS=$(grep "password:" ../src/backend/src/main/resources/application-prod.yml | awk '{print $2}')

# Logging function
log() {
    local level=$1
    local message=$2
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] [$level] $message" | tee -a "$LOG_FILE"
}

# Validate environment and dependencies
validate_environment() {
    log "INFO" "Validating environment and dependencies..."

    # Check required environment variables
    [[ -z "${AZURE_STORAGE_ACCOUNT}" ]] && { log "ERROR" "AZURE_STORAGE_ACCOUNT not set"; return 1; }
    [[ -z "${BACKUP_ENCRYPTION_KEY}" ]] && { log "ERROR" "BACKUP_ENCRYPTION_KEY not set"; return 1; }

    # Check dependencies
    command -v pg_dump >/dev/null 2>&1 || { log "ERROR" "pg_dump not installed"; return 1; }
    command -v openssl >/dev/null 2>&1 || { log "ERROR" "openssl not installed"; return 1; }
    command -v az >/dev/null 2>&1 || { log "ERROR" "azure-cli not installed"; return 1; }
    command -v pigz >/dev/null 2>&1 || { log "ERROR" "pigz not installed"; return 1; }

    # Check disk space
    local available_space=$(df -BG "${BACKUP_ROOT}" | awk 'NR==2 {print $4}' | sed 's/G//')
    if [[ $available_space -lt $MIN_DISK_SPACE_GB ]]; then
        log "ERROR" "Insufficient disk space. Required: ${MIN_DISK_SPACE_GB}GB, Available: ${available_space}GB"
        return 1
    fi

    # Verify backup directory exists and is writable
    mkdir -p "${BACKUP_ROOT}" || { log "ERROR" "Cannot create backup directory"; return 1; }
    [[ -w "${BACKUP_ROOT}" ]] || { log "ERROR" "Backup directory not writable"; return 1; }

    return 0
}

# Perform database backup
perform_backup() {
    local backup_type=$1
    local backup_dir="${BACKUP_ROOT}/${TIMESTAMP}"
    local backup_file="${backup_dir}/vcms_${backup_type}_${TIMESTAMP}.sql"
    
    mkdir -p "${backup_dir}"
    log "INFO" "Starting ${backup_type} backup..."

    # Set pg_dump options based on backup type
    local pg_dump_opts="--format=custom --compress=9 --no-owner --no-acl"
    [[ "${backup_type}" == "incremental" ]] && pg_dump_opts+=" --data-only"

    # Perform backup with retry mechanism
    local attempt=1
    while [[ $attempt -le $MAX_RETRY_ATTEMPTS ]]; do
        if PGPASSWORD="${DB_PASS}" pg_dump ${pg_dump_opts} \
            --username="${DB_USER}" \
            --host="$(echo ${DB_URL} | awk -F[/:] '{print $4}')" \
            --port="$(echo ${DB_URL} | awk -F[/:] '{print $5}')" \
            --dbname="$(echo ${DB_URL} | awk -F[/:] '{print $6}')" \
            --file="${backup_file}" 2>>"${LOG_FILE}"; then
            
            # Compress backup using pigz
            pigz -9 "${backup_file}"
            local compressed_file="${backup_file}.gz"
            
            # Calculate checksum
            local checksum=$(sha256sum "${compressed_file}" | awk '{print $1}')
            echo "${checksum}" > "${compressed_file}.sha256"
            
            log "INFO" "Backup completed successfully. Checksum: ${checksum}"
            return 0
        else
            log "WARN" "Backup attempt ${attempt} failed. Retrying..."
            ((attempt++))
            sleep 5
        fi
    done

    log "ERROR" "Backup failed after ${MAX_RETRY_ATTEMPTS} attempts"
    return 1
}

# Encrypt backup file
encrypt_backup() {
    local input_file=$1
    local encrypted_file="${input_file}.enc"
    
    log "INFO" "Encrypting backup file..."

    # Generate random IV
    local iv=$(openssl rand -hex 16)
    
    # Encrypt file with AES-256-CBC
    echo "${iv}" | xxd -r -p > "${encrypted_file}"
    if openssl enc -aes-256-cbc -in "${input_file}" \
        -out "${encrypted_file}.tmp" \
        -K "${BACKUP_ENCRYPTION_KEY}" \
        -iv "${iv}" 2>>"${LOG_FILE}"; then
        
        cat "${encrypted_file}.tmp" >> "${encrypted_file}"
        rm -f "${encrypted_file}.tmp"
        
        # Calculate encrypted file checksum
        local checksum=$(sha256sum "${encrypted_file}" | awk '{print $1}')
        echo "${checksum}" > "${encrypted_file}.sha256"
        
        # Securely remove original file
        shred -u "${input_file}"
        
        log "INFO" "Encryption completed. Checksum: ${checksum}"
        return 0
    else
        log "ERROR" "Encryption failed"
        return 1
    fi
}

# Upload to Azure Storage
upload_to_azure() {
    local file=$1
    local attempt=1
    
    log "INFO" "Uploading to Azure Storage..."

    while [[ $attempt -le $MAX_RETRY_ATTEMPTS ]]; do
        if az storage blob upload \
            --container-name "${AZURE_STORAGE_CONTAINER}" \
            --file "${file}" \
            --name "$(basename ${file})" \
            --auth-mode login \
            --overwrite true 2>>"${LOG_FILE}"; then
            
            log "INFO" "Upload completed successfully"
            return 0
        else
            log "WARN" "Upload attempt ${attempt} failed. Retrying..."
            ((attempt++))
            sleep 5
        fi
    done

    log "ERROR" "Upload failed after ${MAX_RETRY_ATTEMPTS} attempts"
    return 1
}

# Cleanup old backups
cleanup_old_backups() {
    local backup_type=$1
    local retention_days

    [[ "${backup_type}" == "full" ]] && retention_days=$((FULL_BACKUP_RETENTION_WEEKS * 7)) || retention_days=$BACKUP_RETENTION_DAYS
    
    log "INFO" "Cleaning up old ${backup_type} backups older than ${retention_days} days..."

    # Clean local backups
    find "${BACKUP_ROOT}" -type f -name "vcms_${backup_type}_*.sql.gz.enc" -mtime +${retention_days} -exec rm -f {} \;
    find "${BACKUP_ROOT}" -type f -name "*.sha256" -mtime +${retention_days} -exec rm -f {} \;

    # Clean Azure storage backups
    az storage blob delete-batch \
        --source "${AZURE_STORAGE_CONTAINER}" \
        --pattern "vcms_${backup_type}_*.sql.gz.enc" \
        --if-unmodified-since "$(date -d "${retention_days} days ago" +'%Y-%m-%dT%H:%M:%SZ')" \
        --auth-mode login 2>>"${LOG_FILE}"

    log "INFO" "Cleanup completed"
}

# Main execution
main() {
    log "INFO" "Starting backup process..."

    # Validate environment
    validate_environment || exit 1

    # Perform backup
    if perform_backup "${BACKUP_TYPE}"; then
        local backup_file="${BACKUP_ROOT}/${TIMESTAMP}/vcms_${BACKUP_TYPE}_${TIMESTAMP}.sql.gz"
        
        # Encrypt backup
        if encrypt_backup "${backup_file}"; then
            # Upload to Azure
            if upload_to_azure "${backup_file}.enc"; then
                # Cleanup old backups
                cleanup_old_backups "${BACKUP_TYPE}"
                log "INFO" "Backup process completed successfully"
                exit 0
            fi
        fi
    fi

    log "ERROR" "Backup process failed"
    exit 1
}

# Execute main function
main