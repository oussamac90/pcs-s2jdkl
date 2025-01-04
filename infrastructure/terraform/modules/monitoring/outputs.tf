# Core monitoring endpoints
output "prometheus_url" {
  description = "URL endpoint for accessing Prometheus server"
  value       = helm_release.prometheus.status.url
  sensitive   = false
}

output "grafana_url" {
  description = "URL endpoint for accessing Grafana dashboards"
  value       = helm_release.grafana.status.url
  sensitive   = false
}

output "alertmanager_url" {
  description = "URL endpoint for accessing AlertManager"
  value       = helm_release.prometheus.status.alertmanager_url
  sensitive   = false
}

output "loki_url" {
  description = "URL endpoint for accessing Loki log aggregation"
  value       = helm_release.loki.status.url
  sensitive   = false
}

# Monitoring configuration
output "monitoring_namespace" {
  description = "Kubernetes namespace where monitoring components are deployed"
  value       = var.monitoring_namespace
  sensitive   = false
}

output "grafana_admin_password" {
  description = "Admin password for Grafana dashboard access"
  value       = var.grafana_config.admin_password
  sensitive   = true
}

output "prometheus_retention_period" {
  description = "Data retention period configured for Prometheus metrics"
  value       = var.prometheus_config.retention_period
  sensitive   = false
}

output "loki_retention_period" {
  description = "Data retention period configured for Loki logs"
  value       = var.loki_config.retention_period
  sensitive   = false
}

# Monitoring thresholds and alerts
output "monitoring_thresholds" {
  description = "System health monitoring thresholds for alerts"
  value = {
    cpu = {
      warning  = "70"
      critical = "85"
    }
    memory = {
      warning  = "75"
      critical = "90"
    }
    disk = {
      warning  = "75"
      critical = "90"
    }
    response_time = {
      warning  = "2"
      critical = "5"
    }
    error_rate = {
      warning  = "1"
      critical = "5"
    }
  }
  sensitive = false
}

output "alert_notification_channels" {
  description = "Configured alert notification channels"
  value       = var.alert_channels
  sensitive   = true
}

# Health check endpoints
output "health_check_endpoints" {
  description = "Health check endpoints for monitoring components"
  value = {
    prometheus    = "${helm_release.prometheus.status.url}/-/healthy"
    grafana      = "${helm_release.grafana.status.url}/api/health"
    alertmanager = "${helm_release.prometheus.status.alertmanager_url}/-/healthy"
    loki         = "${helm_release.loki.status.url}/ready"
  }
  sensitive = false
}

# Resource quotas
output "monitoring_resource_quotas" {
  description = "Resource quotas configured for monitoring namespace"
  value = {
    cpu     = "16"
    memory  = "32Gi"
    pods    = "50"
  }
  sensitive = false
}