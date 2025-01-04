# Core Terraform functionality for variable definitions
terraform {
  required_version = "~> 1.0"
}

# Resource Group Configuration
variable "resource_group_name" {
  description = "Name of the resource group where database will be deployed"
  type        = string

  validation {
    condition     = length(var.resource_group_name) > 0
    error_message = "Resource group name cannot be empty"
  }
}

# Location Configuration
variable "location" {
  description = "Azure region where database will be deployed"
  type        = string
  default     = "eastus"
}

# Server Configuration
variable "server_name" {
  description = "Name of the PostgreSQL Flexible Server"
  type        = string

  validation {
    condition     = length(var.server_name) >= 3 && length(var.server_name) <= 63
    error_message = "Server name must be between 3 and 63 characters"
  }
}

variable "database_name" {
  description = "Name of the database to be created"
  type        = string
  default     = "vcms"
}

# Authentication Configuration
variable "administrator_login" {
  description = "Administrator username for PostgreSQL server"
  type        = string
  default     = "vcmsadmin"

  validation {
    condition     = can(regex("^[a-zA-Z][a-zA-Z0-9_]{2,}$", var.administrator_login))
    error_message = "Administrator login must start with a letter, contain only alphanumeric characters or underscores, and be at least 3 characters long"
  }
}

variable "administrator_password" {
  description = "Administrator password for PostgreSQL server"
  type        = string
  sensitive   = true

  validation {
    condition     = length(var.administrator_password) >= 8
    error_message = "Administrator password must be at least 8 characters long"
  }
}

# Network Configuration
variable "subnet_id" {
  description = "ID of the subnet where database will be deployed"
  type        = string
}

variable "private_dns_zone_id" {
  description = "ID of the private DNS zone for database"
  type        = string
}

# Performance Configuration
variable "sku_name" {
  description = "SKU name for PostgreSQL server"
  type        = string
  default     = "Standard_D4s_v3"

  validation {
    condition     = contains(["Standard_D4s_v3", "Standard_D8s_v3", "Standard_D16s_v3"], var.sku_name)
    error_message = "SKU must be one of the supported sizes for production workloads"
  }
}

variable "storage_mb" {
  description = "Storage size in MB"
  type        = number
  default     = 65536  # 64 GB

  validation {
    condition     = var.storage_mb >= 32768 && var.storage_mb <= 16777216
    error_message = "Storage must be between 32GB and 16TB"
  }
}

# Backup Configuration
variable "backup_retention_days" {
  description = "Backup retention period in days"
  type        = number
  default     = 30

  validation {
    condition     = var.backup_retention_days >= 7 && var.backup_retention_days <= 35
    error_message = "Backup retention must be between 7 and 35 days"
  }
}

variable "geo_redundant_backup" {
  description = "Enable geo-redundant backups"
  type        = bool
  default     = true
}

# High Availability Configuration
variable "high_availability" {
  description = "High availability configuration"
  type = object({
    mode                     = string
    standby_availability_zone = string
  })
  default = {
    mode                     = "ZoneRedundant"
    standby_availability_zone = "2"
  }

  validation {
    condition     = var.high_availability.mode == "ZoneRedundant"
    error_message = "High availability mode must be ZoneRedundant for production deployments"
  }
}

# Security Configuration
variable "allowed_ip_ranges" {
  description = "List of IP ranges allowed to access the database"
  type        = list(string)
  default     = []
}

variable "environment" {
  description = "Deployment environment (dev, staging, prod)"
  type        = string

  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "Environment must be one of: dev, staging, prod"
  }
}

variable "enable_ssl" {
  description = "Enable SSL enforcement"
  type        = bool
  default     = true
}

variable "ssl_minimal_tls_version" {
  description = "Minimum TLS version"
  type        = string
  default     = "TLS1_2"

  validation {
    condition     = var.ssl_minimal_tls_version == "TLS1_2"
    error_message = "Only TLS 1.2 is supported for security compliance"
  }
}