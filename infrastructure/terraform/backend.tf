# Backend configuration for Vessel Call Management System Terraform state
# Version: azurerm ~> 3.0

terraform {
  backend "azurerm" {
    resource_group_name  = "${var.environment}-vcms-rg"
    storage_account_name = "${var.environment}vcmstfstate"
    container_name      = "tfstate"
    key                = "terraform.tfstate"
    use_azuread_auth   = true
    subscription_id    = "${var.subscription_id}"
    tenant_id         = "${var.tenant_id}"

    # Security settings
    min_tls_version          = "TLS1_2"
    enable_https_traffic_only = true

    # State locking configuration
    use_microsoft_graph  = true # Use Microsoft Graph API for enhanced security
    blob_properties {
      versioning_enabled = true
      delete_retention_policy {
        days = 30 # Retain deleted state files for 30 days
      }
    }
  }
}

# Local backend configuration for development environments
# This block is conditionally used when running terraform init -backend=false
terraform {
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 3.0"
    }
  }
  required_version = ">= 1.0.0"
}

# Backend configuration validation
locals {
  # Ensure storage account name follows Azure naming conventions
  storage_account_name_valid = can(regex("^[a-z0-9]{3,24}$", "${var.environment}vcmstfstate"))
  
  # Validate environment-specific resource group naming
  resource_group_name_valid = can(regex("^[a-zA-Z0-9-_]{1,90}$", "${var.environment}-vcms-rg"))
}