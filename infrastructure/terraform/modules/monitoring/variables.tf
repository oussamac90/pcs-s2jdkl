# Core Terraform functionality for variable definitions
terraform {
  required_version = "~> 1.0"
}

# Resource group name variable with validation
variable "resource_group_name" {
  description = "Name of the Azure resource group for monitoring resources"
  type        = string

  validation {
    condition     = length(var.resource_group_name) > 0
    error_message = "Resource group name cannot be empty"
  }
}

# Kubernetes namespace for monitoring components
variable "monitoring_namespace" {
  description = "Kubernetes namespace for monitoring components"
  type        = string
  default     = "monitoring"
}

# Prometheus configuration settings
variable "prometheus_config" {
  description = "Configuration settings for Prometheus metrics collection and storage"
  type = object({
    retention_period     = string
    storage_size        = string
    storage_class       = string
    scrape_interval     = string
    evaluation_interval = string
    replica_count       = number
    resource_limits = object({
      cpu    = string
      memory = string
    })
    resource_requests = object({
      cpu    = string
      memory = string
    })
  })
  default = {
    retention_period     = "15d"
    storage_size        = "50Gi"
    storage_class       = "managed-premium"
    scrape_interval     = "30s"
    evaluation_interval = "30s"
    replica_count       = 2
    resource_limits = {
      cpu    = "2000m"
      memory = "4Gi"
    }
    resource_requests = {
      cpu    = "500m"
      memory = "2Gi"
    }
  }
}

# Grafana configuration settings
variable "grafana_config" {
  description = "Configuration settings for Grafana dashboards and visualization"
  type = object({
    admin_password           = string
    storage_size            = string
    storage_class           = string
    dashboard_provider_config = string
    replica_count           = number
    resource_limits = object({
      cpu    = string
      memory = string
    })
    resource_requests = object({
      cpu    = string
      memory = string
    })
    plugins = list(string)
  })
  default = {
    admin_password           = null
    storage_size            = "10Gi"
    storage_class           = "managed-premium"
    dashboard_provider_config = "infrastructure/monitoring/grafana-dashboards/*.json"
    replica_count           = 2
    resource_limits = {
      cpu    = "1000m"
      memory = "2Gi"
    }
    resource_requests = {
      cpu    = "200m"
      memory = "512Mi"
    }
    plugins = ["grafana-piechart-panel", "grafana-worldmap-panel"]
  }
}

# AlertManager configuration settings
variable "alertmanager_config" {
  description = "Configuration settings for AlertManager notification and alert handling"
  type = object({
    storage_size   = string
    storage_class  = string
    config_file    = string
    replica_count  = number
    resource_limits = object({
      cpu    = string
      memory = string
    })
    resource_requests = object({
      cpu    = string
      memory = string
    })
    receivers = object({
      email     = bool
      slack     = bool
      pagerduty = bool
    })
  })
  default = {
    storage_size   = "10Gi"
    storage_class  = "managed-premium"
    config_file    = "infrastructure/monitoring/alertmanager/alertmanager.yml"
    replica_count  = 2
    resource_limits = {
      cpu    = "500m"
      memory = "1Gi"
    }
    resource_requests = {
      cpu    = "100m"
      memory = "256Mi"
    }
    receivers = {
      email     = true
      slack     = true
      pagerduty = true
    }
  }
}

# Loki configuration settings
variable "loki_config" {
  description = "Configuration settings for Loki log aggregation and storage"
  type = object({
    retention_period = string
    storage_size    = string
    storage_class   = string
    config_file     = string
    replica_count   = number
    resource_limits = object({
      cpu    = string
      memory = string
    })
    resource_requests = object({
      cpu    = string
      memory = string
    })
    index = object({
      period = string
      prefix = string
    })
  })
  default = {
    retention_period = "30d"
    storage_size    = "50Gi"
    storage_class   = "managed-premium"
    config_file     = "infrastructure/monitoring/loki/loki.yml"
    replica_count   = 2
    resource_limits = {
      cpu    = "1000m"
      memory = "2Gi"
    }
    resource_requests = {
      cpu    = "200m"
      memory = "512Mi"
    }
    index = {
      period = "24h"
      prefix = "vcms"
    }
  }
}

# Jaeger configuration settings
variable "jaeger_config" {
  description = "Configuration settings for Jaeger distributed tracing"
  type = object({
    storage_size       = string
    storage_class      = string
    retention_period   = string
    collector_replicas = number
    resource_limits = object({
      cpu    = string
      memory = string
    })
    resource_requests = object({
      cpu    = string
      memory = string
    })
    sampling_rate = number
  })
  default = {
    storage_size       = "20Gi"
    storage_class      = "managed-premium"
    retention_period   = "7d"
    collector_replicas = 2
    resource_limits = {
      cpu    = "1000m"
      memory = "2Gi"
    }
    resource_requests = {
      cpu    = "200m"
      memory = "512Mi"
    }
    sampling_rate = 0.1
  }
}

# Resource tags
variable "tags" {
  description = "Tags to be applied to monitoring resources"
  type        = map(string)
  default = {
    Component   = "Monitoring"
    ManagedBy   = "Terraform"
    Environment = "Production"
    System      = "VCMS"
  }
}