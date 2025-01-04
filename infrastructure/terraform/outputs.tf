# Output definitions for the Vessel Call Management System infrastructure
# Terraform version requirements aligned with providers.tf
terraform {
  required_version = ">= 1.0.0"
}

# Resource Group Output
output "resource_group_name" {
  description = "The name of the Azure resource group containing all VCMS infrastructure"
  value       = azurerm_resource_group.main.name
}

# AKS Cluster Outputs
output "aks_cluster_name" {
  description = "The name of the AKS cluster for application deployment"
  value       = module.aks.cluster_name
}

output "aks_cluster_fqdn" {
  description = "The FQDN of the AKS cluster endpoint"
  value       = module.aks.cluster_fqdn
}

output "aks_node_resource_group" {
  description = "The auto-generated resource group name for AKS cluster nodes"
  value       = module.aks.node_resource_group
}

output "aks_kube_config" {
  description = "Kubernetes configuration for AKS cluster access"
  value       = module.aks.kube_config
  sensitive   = true
}

# Database Outputs
output "database_server_name" {
  description = "The name of the PostgreSQL server instance"
  value       = module.database.server_name
}

output "database_connection_string" {
  description = "PostgreSQL database connection string for application configuration"
  value       = module.database.connection_string
  sensitive   = true
}

# Redis Cache Outputs
output "redis_cache_hostname" {
  description = "The hostname of the Redis cache instance"
  value       = azurerm_redis_cache.main.hostname
}

output "redis_connection_string" {
  description = "Redis connection string for application configuration"
  value       = azurerm_redis_cache.main.primary_connection_string
  sensitive   = true
}

# Monitoring Outputs
output "application_insights_instrumentation_key" {
  description = "Application Insights instrumentation key for application monitoring"
  value       = azurerm_application_insights.main.instrumentation_key
  sensitive   = true
}

output "log_analytics_workspace_id" {
  description = "Log Analytics workspace ID for centralized logging"
  value       = azurerm_log_analytics_workspace.main.workspace_id
}

# Network Outputs
output "vnet_name" {
  description = "The name of the virtual network"
  value       = azurerm_virtual_network.main.name
}

output "subnet_ids" {
  description = "Map of subnet names to their IDs"
  value = {
    aks    = azurerm_subnet.aks.id
    db     = azurerm_subnet.database.id
    redis  = azurerm_subnet.redis.id
  }
}

# Environment Information
output "environment" {
  description = "The deployment environment name (dev, staging, prod)"
  value       = var.environment
}

output "location" {
  description = "The Azure region where resources are deployed"
  value       = var.location
}

# Tags Output
output "resource_tags" {
  description = "Common tags applied to all resources"
  value       = local.tags
}