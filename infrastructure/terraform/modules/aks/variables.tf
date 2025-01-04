# Core Terraform functionality for variable definitions and validations
terraform {
  required_version = "~> 1.0"
}

# AKS Cluster Name
variable "cluster_name" {
  description = "Name of the AKS cluster for the Vessel Call Management System"
  type        = string

  validation {
    condition     = length(var.cluster_name) >= 3 && length(var.cluster_name) <= 63 && can(regex("^[a-zA-Z0-9-]*$", var.cluster_name))
    error_message = "Cluster name must be 3-63 characters long and contain only alphanumeric characters and hyphens"
  }
}

# Resource Group Name
variable "resource_group_name" {
  description = "Name of the Azure resource group where AKS cluster will be deployed"
  type        = string

  validation {
    condition     = length(var.resource_group_name) >= 1 && length(var.resource_group_name) <= 90
    error_message = "Resource group name must be between 1 and 90 characters"
  }
}

# Azure Region
variable "location" {
  description = "Azure region where AKS cluster will be deployed"
  type        = string
  default     = "eastus"

  validation {
    condition     = contains(["eastus", "westus", "northeurope", "westeurope"], var.location)
    error_message = "Location must be one of: eastus, westus, northeurope, westeurope"
  }
}

# Kubernetes Version
variable "kubernetes_version" {
  description = "Version of Kubernetes to use for the AKS cluster"
  type        = string
  default     = "1.25.5"

  validation {
    condition     = can(regex("^1\\.(2[4-5])\\.[0-9]+$", var.kubernetes_version))
    error_message = "Kubernetes version must be 1.24.x or 1.25.x"
  }
}

# Node Pool Configuration
variable "node_count" {
  description = "Initial number of nodes in the default node pool"
  type        = number
  default     = 3

  validation {
    condition     = var.node_count >= 1 && var.node_count <= 100
    error_message = "Node count must be between 1 and 100"
  }
}

variable "node_size" {
  description = "VM size for the nodes in the default node pool"
  type        = string
  default     = "Standard_D4s_v3"

  validation {
    condition     = contains(["Standard_D4s_v3", "Standard_D8s_v3", "Standard_D16s_v3"], var.node_size)
    error_message = "Node size must be one of: Standard_D4s_v3, Standard_D8s_v3, Standard_D16s_v3"
  }
}

# Auto-scaling Configuration
variable "min_node_count" {
  description = "Minimum number of nodes for auto-scaling"
  type        = number
  default     = 2

  validation {
    condition     = var.min_node_count >= 1 && var.min_node_count <= var.max_node_count
    error_message = "Minimum node count must be between 1 and max_node_count"
  }
}

variable "max_node_count" {
  description = "Maximum number of nodes for auto-scaling"
  type        = number
  default     = 10

  validation {
    condition     = var.max_node_count >= var.min_node_count && var.max_node_count <= 100
    error_message = "Maximum node count must be between min_node_count and 100"
  }
}

# Networking Configuration
variable "network_plugin" {
  description = "Network plugin to use for the AKS cluster (azure or kubenet)"
  type        = string
  default     = "azure"

  validation {
    condition     = contains(["azure", "kubenet"], var.network_plugin)
    error_message = "Network plugin must be either 'azure' or 'kubenet'"
  }
}

variable "network_policy" {
  description = "Network policy to use for the AKS cluster"
  type        = string
  default     = "calico"

  validation {
    condition     = contains(["calico", "azure"], var.network_policy)
    error_message = "Network policy must be either 'calico' or 'azure'"
  }
}

# Monitoring and Policy Configuration
variable "enable_monitoring" {
  description = "Enable Azure Monitor for containers with Log Analytics integration"
  type        = bool
  default     = true
}

variable "enable_policy" {
  description = "Enable Azure Policy for Kubernetes service with built-in policies"
  type        = bool
  default     = true
}

# High Availability Configuration
variable "availability_zones" {
  description = "List of availability zones for node pool deployment"
  type        = list(number)
  default     = [1, 2, 3]
}

# Resource Tagging
variable "tags" {
  description = "Tags to apply to all resources created for the AKS cluster"
  type        = map(string)
  default = {
    Project       = "VCMS"
    Environment   = "Production"
    ManagedBy     = "Terraform"
    BusinessUnit  = "PortOperations"
    CostCenter    = "IT-Infrastructure"
  }
}