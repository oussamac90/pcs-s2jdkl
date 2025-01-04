# PostgreSQL Server Resource ID
output "server_id" {
  description = "The ID of the PostgreSQL Flexible Server for resource dependencies and references"
  value       = azurerm_postgresql_flexible_server.main.id
  sensitive   = false
}

# Server FQDN for Application Connection
output "server_fqdn" {
  description = "The Fully Qualified Domain Name (FQDN) of the PostgreSQL Flexible Server for application connectivity"
  value       = azurerm_postgresql_flexible_server.main.fqdn
  sensitive   = false
}

# Database Name
output "database_name" {
  description = "The name of the VCMS database for application configuration"
  value       = azurerm_postgresql_flexible_server_database.vcms.name
  sensitive   = false
}

# Administrator Login
output "administrator_login" {
  description = "The administrator username for the PostgreSQL Flexible Server"
  value       = azurerm_postgresql_flexible_server.main.administrator_login
  sensitive   = true
}

# High Availability Status
output "high_availability_mode" {
  description = "The high availability mode configured for the PostgreSQL Flexible Server"
  value       = azurerm_postgresql_flexible_server.main.high_availability[0].mode
  sensitive   = false
}

# Server Version
output "server_version" {
  description = "The version of PostgreSQL running on the Flexible Server"
  value       = azurerm_postgresql_flexible_server.main.version
  sensitive   = false
}

# Backup Configuration
output "backup_configuration" {
  description = "The backup configuration details for the PostgreSQL Flexible Server"
  value = {
    retention_days        = azurerm_postgresql_flexible_server.main.backup_retention_days
    geo_redundant_backup = azurerm_postgresql_flexible_server.main.geo_redundant_backup_enabled
  }
  sensitive = false
}

# Server Configuration Status
output "server_configurations" {
  description = "The status of critical server configurations"
  value = {
    ssl_enforcement = azurerm_postgresql_flexible_server_configuration.ssl_settings.value
    connection_throttling = azurerm_postgresql_flexible_server_configuration.connection_throttling.value
    logging_enabled = azurerm_postgresql_flexible_server_configuration.log_connections.value
  }
  sensitive = false
}

# Connection String Components
output "connection_string_components" {
  description = "Components required to build the connection string (excluding sensitive data)"
  value = {
    host = azurerm_postgresql_flexible_server.main.fqdn
    port = "5432"
    database = azurerm_postgresql_flexible_server_database.vcms.name
  }
  sensitive = false
}