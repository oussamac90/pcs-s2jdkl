# Output definitions for the Azure Kubernetes Service (AKS) module
# These outputs expose essential cluster information and credentials for secure access

output "cluster_name" {
  description = "The name of the AKS cluster for resource identification and management"
  value       = azurerm_kubernetes_cluster.aks.name
  sensitive   = false
}

output "kube_config" {
  description = "Raw kubeconfig file content containing cluster access credentials and configuration. This is sensitive and should be handled securely"
  value       = azurerm_kubernetes_cluster.aks.kube_config_raw
  sensitive   = true
}

output "node_resource_group" {
  description = "The auto-generated Azure resource group name containing all AKS cluster node resources"
  value       = azurerm_kubernetes_cluster.aks.node_resource_group
  sensitive   = false
}

output "cluster_identity" {
  description = "The principal ID of the system-assigned managed identity for the AKS cluster, used for RBAC and resource access"
  value       = azurerm_kubernetes_cluster.aks.identity[0].principal_id
  sensitive   = false
}

output "cluster_endpoint" {
  description = "The HTTPS endpoint URL of the Kubernetes API server for cluster management and access"
  value       = azurerm_kubernetes_cluster.aks.kube_config[0].host
  sensitive   = true
}