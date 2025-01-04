# Main Terraform configuration for Vessel Call Management System infrastructure

# Random string for unique resource naming
resource "random_string" "suffix" {
  length  = 6
  special = false
  upper   = false
}

# Local variables for resource naming and tagging
locals {
  resource_group_name = "${var.environment}-vcms-rg-${random_string.suffix.result}"
  tags = merge(var.tags, {
    Environment        = var.environment
    Project           = "VCMS"
    ManagedBy         = "Terraform"
    CostCenter        = "Maritime-Ops"
    DataClassification = "Confidential"
    DisasterRecovery  = "Required"
  })
}

# Resource group with deletion lock
resource "azurerm_resource_group" "main" {
  name     = local.resource_group_name
  location = var.location
  tags     = local.tags

  lifecycle {
    prevent_destroy = true
  }
}

# Resource group lock to prevent accidental deletion
resource "azurerm_management_lock" "resource_group" {
  name       = "${local.resource_group_name}-lock"
  scope      = azurerm_resource_group.main.id
  lock_level = "CanNotDelete"
  notes      = "Protected resource group for VCMS production infrastructure"
}

# Virtual Network for all components
resource "azurerm_virtual_network" "main" {
  name                = "${var.environment}-vcms-vnet"
  resource_group_name = azurerm_resource_group.main.name
  location           = azurerm_resource_group.main.location
  address_space      = var.network_config.vnet_address_space
  tags               = local.tags

  # DNS servers configuration
  dns_servers = []
}

# Subnets for different components
resource "azurerm_subnet" "aks" {
  name                 = "aks-subnet"
  resource_group_name  = azurerm_resource_group.main.name
  virtual_network_name = azurerm_virtual_network.main.name
  address_prefixes     = [var.network_config.subnet_prefixes.aks]
  service_endpoints    = var.network_config.service_endpoints

  delegation {
    name = "aks-delegation"
    service_delegation {
      name = "Microsoft.ContainerService/managedClusters"
      actions = [
        "Microsoft.Network/virtualNetworks/subnets/join/action"
      ]
    }
  }
}

# AKS Cluster deployment
module "aks" {
  source = "./modules/aks"

  environment         = var.environment
  location           = var.location
  resource_group_name = azurerm_resource_group.main.name
  subnet_id          = azurerm_subnet.aks.id
  kubernetes_version = var.aks_config.kubernetes_version
  
  default_node_pool  = var.aks_config.default_node_pool
  network_profile   = var.aks_config.network_profile
  identity          = var.aks_config.identity

  monitoring_config = var.monitoring_config
  tags             = local.tags
}

# Database subnet with service endpoints
resource "azurerm_subnet" "database" {
  name                 = "database-subnet"
  resource_group_name  = azurerm_resource_group.main.name
  virtual_network_name = azurerm_virtual_network.main.name
  address_prefixes     = [var.network_config.subnet_prefixes.db]
  service_endpoints    = ["Microsoft.Sql"]

  delegation {
    name = "fs"
    service_delegation {
      name = "Microsoft.DBforPostgreSQL/flexibleServers"
    }
  }
}

# PostgreSQL Database deployment
module "database" {
  source = "./modules/database"

  environment         = var.environment
  location           = var.location
  resource_group_name = azurerm_resource_group.main.name
  subnet_id          = azurerm_subnet.database.id
  
  database_config     = var.database_config
  monitoring_config   = var.monitoring_config
  tags               = local.tags
}

# Redis subnet configuration
resource "azurerm_subnet" "redis" {
  name                 = "redis-subnet"
  resource_group_name  = azurerm_resource_group.main.name
  virtual_network_name = azurerm_virtual_network.main.name
  address_prefixes     = [var.network_config.subnet_prefixes.redis]
  service_endpoints    = ["Microsoft.Cache"]
}

# Redis Cache deployment
resource "azurerm_redis_cache" "main" {
  name                = "${var.environment}-vcms-redis-${random_string.suffix.result}"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  
  sku_name            = var.redis_config.sku_name
  family              = var.redis_config.family
  capacity            = var.redis_config.capacity
  
  enable_non_ssl_port = var.redis_config.enable_non_ssl_port
  minimum_tls_version = var.redis_config.minimum_tls_version
  
  subnet_id           = azurerm_subnet.redis.id
  
  redis_configuration {
    maxmemory_reserved              = var.redis_config.redis_configuration.maxmemory_reserved
    maxfragmentationmemory_reserved = var.redis_config.redis_configuration.maxfragmentationmemory_reserved
    maxmemory_delta                 = var.redis_config.redis_configuration.maxmemory_delta
  }

  tags = local.tags
}

# Log Analytics Workspace for monitoring
resource "azurerm_log_analytics_workspace" "main" {
  name                = "${var.environment}-vcms-law-${random_string.suffix.result}"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  sku                = "PerGB2018"
  retention_in_days   = var.monitoring_config.log_analytics_retention_days

  tags = local.tags
}

# Application Insights for monitoring
resource "azurerm_application_insights" "main" {
  name                = "${var.environment}-vcms-ai-${random_string.suffix.result}"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  application_type    = "web"
  workspace_id        = azurerm_log_analytics_workspace.main.id

  tags = local.tags
}

# Outputs for other configurations
output "resource_group_name" {
  value = azurerm_resource_group.main.name
}

output "aks_cluster_name" {
  value = module.aks.cluster_name
}

output "postgresql_server_name" {
  value = module.database.server_name
}

output "redis_cache_name" {
  value = azurerm_redis_cache.main.name
}

output "application_insights_instrumentation_key" {
  value     = azurerm_application_insights.main.instrumentation_key
  sensitive = true
}