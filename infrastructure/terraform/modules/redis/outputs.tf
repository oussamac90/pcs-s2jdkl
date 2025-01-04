# Output definitions for Redis cache module
# These outputs expose essential Redis instance attributes for application configuration
# and other module integrations

output "redis_id" {
  description = "The resource ID of the Redis cache instance"
  value       = azurerm_redis_cache.vcms.id
}

output "redis_name" {
  description = "The name of the Redis cache instance"
  value       = azurerm_redis_cache.vcms.name
}

output "redis_hostname" {
  description = "The hostname of the Redis cache instance"
  value       = azurerm_redis_cache.vcms.hostname
}

output "redis_ssl_port" {
  description = "The SSL port of the Redis cache instance"
  value       = azurerm_redis_cache.vcms.ssl_port
}

output "redis_connection_string" {
  description = "The primary connection string of the Redis cache instance"
  value       = azurerm_redis_cache.vcms.primary_connection_string
  sensitive   = true
}