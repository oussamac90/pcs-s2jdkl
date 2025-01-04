# Terraform configuration block with version constraints and required providers
terraform {
  required_version = ">= 1.0.0"

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
    random = {
      source  = "hashicorp/random"
      version = "~> 3.0"
    }
  }
}

# Azure Resource Manager provider configuration with enhanced security features
provider "azurerm" {
  features {
    key_vault {
      purge_soft_delete_on_destroy               = false
      recover_soft_deleted_key_vaults            = true
      purge_soft_deleted_secrets_on_destroy      = false
    }
    resource_group {
      prevent_deletion_if_contains_resources     = true
    }
    virtual_machine {
      delete_os_disk_on_deletion                 = true
      graceful_shutdown                          = true
    }
    log_analytics_workspace {
      permanently_delete_on_destroy              = false
    }
  }

  # Enable OpenID Connect authentication
  use_oidc = true

  # Enable Azure AD authentication for storage accounts
  storage_use_azuread = true

  # Prevent automatic provider registration
  skip_provider_registration = false

  # Authentication configuration
  tenant_id       = var.tenant_id
  subscription_id = var.subscription_id
  environment     = var.environment
}

# Kubernetes provider configuration for AKS cluster management
provider "kubernetes" {
  host = data.azurerm_kubernetes_cluster.aks.kube_config.0.host

  client_certificate     = base64decode(data.azurerm_kubernetes_cluster.aks.kube_config.0.client_certificate)
  client_key            = base64decode(data.azurerm_kubernetes_cluster.aks.kube_config.0.client_key)
  cluster_ca_certificate = base64decode(data.azurerm_kubernetes_cluster.aks.kube_config.0.cluster_ca_certificate)

  # Azure AD authentication configuration using kubelogin
  exec {
    api_version = "client.authentication.k8s.io/v1beta1"
    command     = "kubelogin"
    args = [
      "get-token",
      "--login",
      "spn",
      "--environment",
      "AzurePublicCloud",
      "--tenant-id",
      var.tenant_id,
      "--server-id",
      var.aks_server_id
    ]
  }
}

# Helm provider configuration for application deployments
provider "helm" {
  kubernetes {
    host = data.azurerm_kubernetes_cluster.aks.kube_config.0.host

    client_certificate     = base64decode(data.azurerm_kubernetes_cluster.aks.kube_config.0.client_certificate)
    client_key            = base64decode(data.azurerm_kubernetes_cluster.aks.kube_config.0.client_key)
    cluster_ca_certificate = base64decode(data.azurerm_kubernetes_cluster.aks.kube_config.0.cluster_ca_certificate)
  }

  # Helm repository authentication
  registry {
    url      = var.helm_repository_url
    username = var.helm_repository_username
    password = var.helm_repository_password
  }
}