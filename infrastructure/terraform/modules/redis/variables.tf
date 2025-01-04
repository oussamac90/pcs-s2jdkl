# Core Terraform functionality for variable definitions and validation rules
terraform {
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 3.0"
    }
  }
}

# Resource Group Configuration
variable "resource_group_name" {
  description = "Name of the resource group where Redis cache will be deployed"
  type        = string
  
  validation {
    condition     = length(var.resource_group_name) >= 3 && length(var.resource_group_name) <= 63
    error_message = "Resource group name must be between 3 and 63 characters"
  }
}

variable "location" {
  description = "Azure region where Redis cache will be deployed"
  type        = string
}

# Environment Configuration
variable "environment" {
  description = "Deployment environment (dev, staging, prod)"
  type        = string
  
  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "Environment must be one of: dev, staging, prod"
  }
}

# Redis Instance Configuration
variable "redis_name" {
  description = "Name of the Redis cache instance"
  type        = string
  
  validation {
    condition     = length(var.redis_name) >= 3 && length(var.redis_name) <= 63
    error_message = "Redis cache name must be between 3 and 63 characters"
  }
}

variable "redis_sku" {
  description = "The SKU of Redis cache to deploy (Basic, Standard, Premium)"
  type        = string
  default     = "Premium"
  
  validation {
    condition     = contains(["Basic", "Standard", "Premium"], var.redis_sku)
    error_message = "Redis SKU must be one of: Basic, Standard, Premium"
  }
}

variable "redis_family" {
  description = "The family for the Redis cache SKU (C for Basic/Standard, P for Premium)"
  type        = string
  default     = "P"
  
  validation {
    condition     = contains(["C", "P"], var.redis_family)
    error_message = "Redis family must be either C or P"
  }
}

variable "redis_capacity" {
  description = "The size of the Redis cache instance (0-6 for Basic/Standard, 1-4 for Premium)"
  type        = number
  default     = 1
  
  validation {
    condition     = (var.redis_sku == "Premium" ? var.redis_capacity >= 1 && var.redis_capacity <= 4 : var.redis_capacity >= 0 && var.redis_capacity <= 6)
    error_message = "Invalid capacity for selected SKU"
  }
}

# Security Configuration
variable "enable_non_ssl_port" {
  description = "Enable the non-SSL port (6379) for Redis cache access"
  type        = bool
  default     = false
}

variable "minimum_tls_version" {
  description = "The minimum TLS version for Redis cache connections"
  type        = string
  default     = "1.2"
  
  validation {
    condition     = contains(["1.0", "1.1", "1.2"], var.minimum_tls_version)
    error_message = "TLS version must be one of: 1.0, 1.1, 1.2"
  }
}

variable "subnet_id" {
  description = "The ID of the subnet where the Redis cache should be deployed (Premium SKU only)"
  type        = string
  default     = null
}

# Redis Configuration Options
variable "redis_configuration" {
  description = "Additional Redis configuration options including memory management and persistence settings"
  type        = map(string)
  default = {
    maxmemory_reserved                 = "50"
    maxfragmentationmemory_reserved    = "50"
    maxmemory_delta                    = "50"
    maxmemory_policy                   = "volatile-lru"
    notify_keyspace_events             = "KEA"
    enable_authentication              = "true"
  }
}

# Maintenance Configuration
variable "patch_schedule" {
  description = "Redis cache patching schedule configuration"
  type = list(object({
    day_of_week    = string
    start_hour_utc = number
  }))
  default = [
    {
      day_of_week    = "Sunday"
      start_hour_utc = 2
    }
  ]
  
  validation {
    condition = alltrue([
      for s in var.patch_schedule :
      contains(["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"], s.day_of_week) &&
      s.start_hour_utc >= 0 && s.start_hour_utc <= 23
    ])
    error_message = "Invalid patch schedule configuration"
  }
}