# Configure required providers for Redis module
terraform {
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 3.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.0"
    }
  }
}

# Local variables for resource naming and tagging
locals {
  redis_name = "${var.environment}-vcms-redis-${random_string.suffix.result}"
  tags = {
    Environment        = var.environment
    Project           = "VCMS"
    Component         = "Cache"
    ManagedBy         = "Terraform"
    CostCenter        = "IT-Infrastructure"
    SecurityLevel     = "High"
    DataClassification = "Confidential"
  }
}

# Generate random suffix for Redis cache name
resource "random_string" "suffix" {
  length  = 6
  special = false
  upper   = false
}

# Azure Cache for Redis instance
resource "azurerm_redis_cache" "vcms" {
  name                = local.redis_name
  location            = var.location
  resource_group_name = var.resource_group_name
  
  # Performance configuration
  capacity            = var.redis_capacity
  family              = var.redis_family
  sku_name            = var.redis_sku
  
  # Security configuration
  enable_non_ssl_port = var.enable_non_ssl_port
  minimum_tls_version = var.minimum_tls_version
  
  # Network configuration
  public_network_access_enabled = false
  subnet_id                    = var.subnet_id
  
  # Redis configuration
  redis_configuration {
    maxmemory_reserved              = "2"
    maxmemory_delta                = "2"
    maxmemory_policy               = "volatile-lru"
    maxfragmentationmemory_reserved = "2"
  }

  # Patch schedule
  dynamic "patch_schedule" {
    for_each = var.patch_schedule
    content {
      day_of_week    = patch_schedule.value.day_of_week
      start_hour_utc = patch_schedule.value.start_hour_utc
    }
  }

  tags = local.tags

  # Lifecycle policy to prevent accidental deletion
  lifecycle {
    prevent_destroy = true
  }
}

# Outputs for Redis cache resource attributes
output "redis_id" {
  value       = azurerm_redis_cache.vcms.id
  description = "The ID of the Redis cache instance"
}

output "redis_name" {
  value       = azurerm_redis_cache.vcms.name
  description = "The name of the Redis cache instance"
}

output "redis_hostname" {
  value       = azurerm_redis_cache.vcms.hostname
  description = "The hostname of the Redis cache instance"
  sensitive   = true
}

output "redis_ssl_port" {
  value       = azurerm_redis_cache.vcms.ssl_port
  description = "The SSL port of the Redis cache instance"
}

output "redis_primary_access_key" {
  value       = azurerm_redis_cache.vcms.primary_access_key
  description = "The primary access key for the Redis cache instance"
  sensitive   = true
}

output "redis_primary_connection_string" {
  value       = azurerm_redis_cache.vcms.primary_connection_string
  description = "The primary connection string for the Redis cache instance"
  sensitive   = true
}

output "redis_secondary_connection_string" {
  value       = azurerm_redis_cache.vcms.secondary_connection_string
  description = "The secondary connection string for the Redis cache instance"
  sensitive   = true
}