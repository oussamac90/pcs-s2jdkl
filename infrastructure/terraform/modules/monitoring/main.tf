# Configure required providers
terraform {
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 3.0"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.0"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.0"
    }
  }
}

# Create monitoring namespace with resource quotas
resource "kubernetes_namespace" "monitoring" {
  metadata {
    name   = var.monitoring_namespace
    labels = var.tags
  }

  spec {
    resource_quota {
      hard = {
        cpu     = "16"
        memory  = "32Gi"
        pods    = "50"
      }
    }
  }
}

# Deploy Prometheus stack using Helm
resource "helm_release" "prometheus" {
  name       = "prometheus"
  repository = "https://prometheus-community.github.io/helm-charts"
  chart      = "kube-prometheus-stack"
  namespace  = kubernetes_namespace.monitoring.metadata[0].name

  values = [file(var.prometheus_config.config_file)]

  set {
    name  = "prometheus.retention"
    value = var.prometheus_config.retention_period
  }

  set {
    name  = "prometheus.replicaCount"
    value = var.prometheus_config.replica_count
  }

  set {
    name  = "prometheus.storage.size"
    value = var.prometheus_config.storage_size
  }

  set {
    name  = "prometheus.storageClass"
    value = var.prometheus_config.storage_class
  }

  set {
    name  = "prometheus.resources.limits.cpu"
    value = var.prometheus_config.resource_limits.cpu
  }

  set {
    name  = "prometheus.resources.limits.memory"
    value = var.prometheus_config.resource_limits.memory
  }

  set {
    name  = "prometheus.resources.requests.cpu"
    value = var.prometheus_config.resource_requests.cpu
  }

  set {
    name  = "prometheus.resources.requests.memory"
    value = var.prometheus_config.resource_requests.memory
  }
}

# Deploy Grafana using Helm
resource "helm_release" "grafana" {
  name       = "grafana"
  repository = "https://grafana.github.io/helm-charts"
  chart      = "grafana"
  namespace  = kubernetes_namespace.monitoring.metadata[0].name

  values = [file(var.grafana_config.dashboard_provider_config)]

  set_sensitive {
    name  = "adminPassword"
    value = var.grafana_config.admin_password
  }

  set {
    name  = "persistence.enabled"
    value = "true"
  }

  set {
    name  = "persistence.size"
    value = var.grafana_config.storage_size
  }

  set {
    name  = "persistence.storageClass"
    value = var.grafana_config.storage_class
  }

  set {
    name  = "replicaCount"
    value = var.grafana_config.replica_count
  }

  set {
    name  = "resources.limits.cpu"
    value = var.grafana_config.resource_limits.cpu
  }

  set {
    name  = "resources.limits.memory"
    value = var.grafana_config.resource_limits.memory
  }

  set {
    name  = "resources.requests.cpu"
    value = var.grafana_config.resource_requests.cpu
  }

  set {
    name  = "resources.requests.memory"
    value = var.grafana_config.resource_requests.memory
  }

  dynamic "set" {
    for_each = var.grafana_config.plugins
    content {
      name  = "plugins[${set.key}]"
      value = set.value
    }
  }
}

# Deploy Loki using Helm
resource "helm_release" "loki" {
  name       = "loki"
  repository = "https://grafana.github.io/helm-charts"
  chart      = "loki-stack"
  namespace  = kubernetes_namespace.monitoring.metadata[0].name

  values = [file(var.loki_config.config_file)]

  set {
    name  = "loki.persistence.enabled"
    value = "true"
  }

  set {
    name  = "loki.persistence.size"
    value = var.loki_config.storage_size
  }

  set {
    name  = "loki.persistence.storageClass"
    value = var.loki_config.storage_class
  }

  set {
    name  = "loki.config.retention_period"
    value = var.loki_config.retention_period
  }

  set {
    name  = "loki.replicas"
    value = var.loki_config.replica_count
  }

  set {
    name  = "loki.resources.limits.cpu"
    value = var.loki_config.resource_limits.cpu
  }

  set {
    name  = "loki.resources.limits.memory"
    value = var.loki_config.resource_limits.memory
  }

  set {
    name  = "loki.resources.requests.cpu"
    value = var.loki_config.resource_requests.cpu
  }

  set {
    name  = "loki.resources.requests.memory"
    value = var.loki_config.resource_requests.memory
  }
}

# Deploy AlertManager using Helm
resource "helm_release" "alertmanager" {
  name       = "alertmanager"
  repository = "https://prometheus-community.github.io/helm-charts"
  chart      = "alertmanager"
  namespace  = kubernetes_namespace.monitoring.metadata[0].name

  values = [file(var.alertmanager_config.config_file)]

  set {
    name  = "persistence.enabled"
    value = "true"
  }

  set {
    name  = "persistence.size"
    value = var.alertmanager_config.storage_size
  }

  set {
    name  = "persistence.storageClass"
    value = var.alertmanager_config.storage_class
  }

  set {
    name  = "replicaCount"
    value = var.alertmanager_config.replica_count
  }

  set {
    name  = "resources.limits.cpu"
    value = var.alertmanager_config.resource_limits.cpu
  }

  set {
    name  = "resources.limits.memory"
    value = var.alertmanager_config.resource_limits.memory
  }

  set {
    name  = "resources.requests.cpu"
    value = var.alertmanager_config.resource_requests.cpu
  }

  set {
    name  = "resources.requests.memory"
    value = var.alertmanager_config.resource_requests.memory
  }
}

# Deploy Jaeger using Helm
resource "helm_release" "jaeger" {
  name       = "jaeger"
  repository = "https://jaegertracing.github.io/helm-charts"
  chart      = "jaeger"
  namespace  = kubernetes_namespace.monitoring.metadata[0].name

  values = [file(var.jaeger_config.config_file)]

  set {
    name  = "persistence.enabled"
    value = "true"
  }

  set {
    name  = "persistence.size"
    value = var.jaeger_config.storage_size
  }

  set {
    name  = "persistence.storageClass"
    value = var.jaeger_config.storage_class
  }

  set {
    name  = "collector.replicaCount"
    value = var.jaeger_config.collector_replicas
  }

  set {
    name  = "resources.limits.cpu"
    value = var.jaeger_config.resource_limits.cpu
  }

  set {
    name  = "resources.limits.memory"
    value = var.jaeger_config.resource_limits.memory
  }

  set {
    name  = "resources.requests.cpu"
    value = var.jaeger_config.resource_requests.cpu
  }

  set {
    name  = "resources.requests.memory"
    value = var.jaeger_config.resource_requests.memory
  }

  set {
    name  = "sampling.rate"
    value = var.jaeger_config.sampling_rate
  }
}

# Output monitoring endpoints
output "prometheus_url" {
  value = "http://prometheus.${kubernetes_namespace.monitoring.metadata[0].name}.svc.cluster.local:9090"
}

output "grafana_url" {
  value = "http://grafana.${kubernetes_namespace.monitoring.metadata[0].name}.svc.cluster.local:3000"
}

output "alertmanager_url" {
  value = "http://alertmanager.${kubernetes_namespace.monitoring.metadata[0].name}.svc.cluster.local:9093"
}

output "jaeger_url" {
  value = "http://jaeger-query.${kubernetes_namespace.monitoring.metadata[0].name}.svc.cluster.local:16686"
}