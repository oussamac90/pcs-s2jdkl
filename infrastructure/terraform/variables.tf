# Core environment variables
variable "environment" {
  description = "Deployment environment (dev, staging, prod)"
  type        = string
  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "Environment must be one of: dev, staging, prod"
  }
}

variable "location" {
  description = "Azure region for resource deployment"
  type        = string
  default     = "eastus"
  validation {
    condition     = can(regex("^[a-z]+[a-z0-9]+$", var.location))
    error_message = "Location must be a valid Azure region name"
  }
}

variable "resource_prefix" {
  description = "Prefix for all resource names"
  type        = string
  default     = "vcms"
  validation {
    condition     = can(regex("^[a-z0-9]+$", var.resource_prefix))
    error_message = "Resource prefix must contain only lowercase letters and numbers"
  }
}

# AKS cluster configuration
variable "aks_config" {
  description = "Comprehensive AKS cluster configuration"
  type = object({
    kubernetes_version = string
    default_node_pool = object({
      name                = string
      vm_size            = string
      node_count         = number
      min_count          = number
      max_count          = number
      os_disk_size_gb    = number
      type               = string
      enable_auto_scaling = bool
      zones              = list(string)
    })
    network_profile = object({
      network_plugin     = string
      network_policy     = string
      service_cidr       = string
      dns_service_ip     = string
      docker_bridge_cidr = string
    })
    identity = object({
      type = string
    })
  })
  default = {
    kubernetes_version = "1.25"
    default_node_pool = {
      name                = "systempool"
      vm_size            = "Standard_D4s_v3"
      node_count         = 3
      min_count          = 2
      max_count          = 10
      os_disk_size_gb    = 128
      type               = "VirtualMachineScaleSets"
      enable_auto_scaling = true
      zones              = ["1", "2", "3"]
    }
    network_profile = {
      network_plugin     = "azure"
      network_policy     = "calico"
      service_cidr       = "10.0.0.0/16"
      dns_service_ip     = "10.0.0.10"
      docker_bridge_cidr = "172.17.0.1/16"
    }
    identity = {
      type = "SystemAssigned"
    }
  }
}

# Database configuration
variable "database_config" {
  description = "Azure Database for PostgreSQL configuration"
  type = object({
    sku_name                = string
    storage_mb              = number
    backup_retention_days   = number
    geo_redundant_backup    = bool
    auto_grow_enabled      = bool
    high_availability      = object({
      mode                     = string
      standby_availability_zone = string
    })
    ssl_enforcement_enabled    = bool
    ssl_minimal_tls_version    = string
  })
  default = {
    sku_name                = "GP_Gen5_4"
    storage_mb              = 102400
    backup_retention_days   = 35
    geo_redundant_backup    = true
    auto_grow_enabled      = true
    high_availability      = {
      mode                     = "ZoneRedundant"
      standby_availability_zone = "2"
    }
    ssl_enforcement_enabled    = true
    ssl_minimal_tls_version    = "TLS1_2"
  }
}

# Redis configuration
variable "redis_config" {
  description = "Azure Cache for Redis configuration"
  type = object({
    sku_name              = string
    family               = string
    capacity             = number
    enable_non_ssl_port  = bool
    minimum_tls_version  = string
    shard_count          = number
    patch_schedule       = object({
      day_of_week     = string
      start_hour_utc  = number
    })
    redis_configuration = object({
      maxmemory_reserved                = string
      maxfragmentationmemory_reserved  = string
      maxmemory_delta                  = string
    })
  })
  default = {
    sku_name              = "Premium"
    family               = "P"
    capacity             = 1
    enable_non_ssl_port  = false
    minimum_tls_version  = "1.2"
    shard_count          = 2
    patch_schedule       = {
      day_of_week     = "Sunday"
      start_hour_utc  = 2
    }
    redis_configuration = {
      maxmemory_reserved                = "50"
      maxfragmentationmemory_reserved  = "50"
      maxmemory_delta                  = "50"
    }
  }
}

# Monitoring configuration
variable "monitoring_config" {
  description = "Azure monitoring and observability configuration"
  type = object({
    log_analytics_retention_days = number
    enable_container_insights   = bool
    enable_prometheus          = bool
    metrics_retention_days     = number
    alert_thresholds          = object({
      cpu_threshold           = number
      memory_threshold        = number
      disk_threshold         = number
      response_time_threshold = number
    })
    diagnostic_settings       = object({
      enabled               = bool
      retention_policy_days = number
    })
  })
  default = {
    log_analytics_retention_days = 30
    enable_container_insights   = true
    enable_prometheus          = true
    metrics_retention_days     = 90
    alert_thresholds          = {
      cpu_threshold           = 80
      memory_threshold        = 85
      disk_threshold         = 85
      response_time_threshold = 2000
    }
    diagnostic_settings       = {
      enabled               = true
      retention_policy_days = 90
    }
  }
}

# Network configuration
variable "network_config" {
  description = "Network configuration for all components"
  type = object({
    vnet_address_space    = list(string)
    subnet_prefixes       = object({
      aks   = string
      db    = string
      redis = string
    })
    service_endpoints     = list(string)
    network_security_rules = object({
      allow_https = object({
        priority  = number
        direction = string
        access    = string
        protocol  = string
        port     = number
      })
    })
  })
  default = {
    vnet_address_space    = ["10.0.0.0/8"]
    subnet_prefixes       = {
      aks   = "10.1.0.0/16"
      db    = "10.2.0.0/16"
      redis = "10.3.0.0/16"
    }
    service_endpoints     = ["Microsoft.Sql", "Microsoft.AzureCosmosDB", "Microsoft.KeyVault"]
    network_security_rules = {
      allow_https = {
        priority  = 100
        direction = "Inbound"
        access    = "Allow"
        protocol  = "Tcp"
        port     = 443
      }
    }
  }
}

# Resource tagging
variable "tags" {
  description = "Common tags to be applied to all resources"
  type        = map(string)
  default = {
    Project            = "VCMS"
    Environment        = "var.environment"
    ManagedBy         = "Terraform"
    BusinessUnit      = "PortOperations"
    DataClassification = "Confidential"
    CostCenter        = "IT-Infrastructure"
  }
}