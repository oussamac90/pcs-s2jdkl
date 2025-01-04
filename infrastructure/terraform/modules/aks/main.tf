# Configure required providers with specific versions
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
  required_version = ">= 1.0"
}

# Generate a random suffix for unique naming
resource "random_string" "suffix" {
  length  = 6
  special = false
  upper   = false
}

# Production-grade AKS cluster configuration
resource "azurerm_kubernetes_cluster" "aks" {
  name                = "${var.cluster_name}-${random_string.suffix.result}"
  location            = var.location
  resource_group_name = var.resource_group_name
  dns_prefix         = "${var.cluster_name}-dns"
  kubernetes_version = var.kubernetes_version

  # Default node pool configuration with high availability
  default_node_pool {
    name                         = "default"
    node_count                   = var.node_count
    vm_size                      = var.node_size
    enable_auto_scaling         = true
    min_count                   = var.min_node_count
    max_count                   = var.max_node_count
    type                        = "VirtualMachineScaleSets"
    zones                       = var.availability_zones
    only_critical_addons_enabled = false
    
    # Node configuration and labels
    node_labels = {
      "environment" = var.tags["Environment"]
      "workload"    = "general"
      "managed-by"  = "terraform"
    }

    # Node taints for workload isolation
    node_taints = []

    # Enable OS disk encryption
    os_disk_type    = "Managed"
    os_disk_size_gb = 128
  }

  # Identity configuration
  identity {
    type = "SystemAssigned"
  }

  # Network profile with advanced networking features
  network_profile {
    network_plugin     = var.network_plugin
    network_policy     = var.network_policy
    load_balancer_sku = "standard"
    outbound_type     = "loadBalancer"
    
    # Pod networking configuration
    pod_cidr          = "10.244.0.0/16"
    service_cidr      = "10.0.0.0/16"
    dns_service_ip    = "10.0.0.10"
    docker_bridge_cidr = "172.17.0.1/16"
  }

  # Azure AD integration and RBAC
  azure_active_directory_role_based_access_control {
    managed                = true
    azure_rbac_enabled    = true
    admin_group_object_ids = []  # Configure admin group IDs in variables
  }

  # Addon profiles for monitoring and security
  monitor_metrics {}

  oms_agent {
    log_analytics_workspace_id = var.enable_monitoring ? azurerm_log_analytics_workspace.aks[0].id : null
  }

  azure_policy {
    enabled = var.enable_policy
  }

  key_vault_secrets_provider {
    secret_rotation_enabled  = true
    secret_rotation_interval = "2m"
  }

  # Auto-scaler profile configuration
  auto_scaler_profile {
    balance_similar_node_groups      = true
    expander                        = "random"
    max_graceful_termination_sec    = "600"
    max_node_provisioning_time      = "15m"
    max_unready_nodes               = 3
    max_unready_percentage          = 45
    new_pod_scale_up_delay          = "10s"
    scale_down_delay_after_add      = "10m"
    scale_down_delay_after_delete   = "10s"
    scale_down_delay_after_failure  = "3m"
    scan_interval                   = "10s"
    scale_down_unneeded            = "10m"
    scale_down_unready             = "20m"
    scale_down_utilization_threshold = "0.5"
  }

  # Maintenance window configuration
  maintenance_window {
    allowed {
      day   = "Sunday"
      hours = [21, 22, 23]
    }
  }

  # Resource tags
  tags = merge(var.tags, {
    "cluster-name" = var.cluster_name
    "created-by"   = "terraform"
  })
}

# Create Log Analytics workspace if monitoring is enabled
resource "azurerm_log_analytics_workspace" "aks" {
  count               = var.enable_monitoring ? 1 : 0
  name                = "${var.cluster_name}-logs-${random_string.suffix.result}"
  location            = var.location
  resource_group_name = var.resource_group_name
  sku                = "PerGB2018"
  retention_in_days   = 30

  tags = var.tags
}

# Output configurations
output "cluster_name" {
  description = "The name of the AKS cluster"
  value       = azurerm_kubernetes_cluster.aks.name
}

output "kube_config" {
  description = "Kubeconfig for cluster access"
  value       = azurerm_kubernetes_cluster.aks.kube_config_raw
  sensitive   = true
}

output "cluster_identity" {
  description = "The managed identity of the AKS cluster"
  value       = azurerm_kubernetes_cluster.aks.identity[0].principal_id
}