# Azure Database for PostgreSQL Flexible Server deployment configuration
# Provider versions
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
}

# Local variables for common tags
locals {
  tags = {
    Environment    = var.environment
    Service       = "Database"
    ManagedBy     = "Terraform"
    Application   = "VCMS"
    SecurityLevel = "High"
    BackupEnabled = "True"
  }
}

# Generate secure random password for PostgreSQL administrator
resource "random_password" "postgres_password" {
  length           = 32
  special          = true
  upper            = true
  lower            = true
  number           = true
  min_special      = 2
  min_upper        = 2
  min_lower        = 2
  min_numeric      = 2
  override_special = "!#$%&*()-_=+[]{}<>:?"
}

# PostgreSQL Flexible Server deployment
resource "azurerm_postgresql_flexible_server" "main" {
  name                   = var.server_name
  resource_group_name    = var.resource_group_name
  location               = var.location
  version                = "14"
  delegated_subnet_id    = var.subnet_id
  private_dns_zone_id    = var.private_dns_zone_id
  administrator_login    = var.administrator_login
  administrator_password = random_password.postgres_password.result
  zone                  = "1"
  storage_mb            = var.storage_mb
  sku_name              = var.sku_name
  
  backup_retention_days        = var.backup_retention_days
  geo_redundant_backup_enabled = var.geo_redundant_backup

  high_availability {
    mode                      = var.high_availability.mode
    standby_availability_zone = var.high_availability.standby_availability_zone
  }

  maintenance_window {
    day_of_week  = 0  # Sunday
    start_hour   = 2  # 2 AM
    start_minute = 0
  }

  authentication {
    active_directory_auth_enabled = true
    password_auth_enabled         = true
  }

  tags = local.tags
}

# VCMS database creation
resource "azurerm_postgresql_flexible_server_database" "vcms" {
  name      = var.database_name
  server_id = azurerm_postgresql_flexible_server.main.id
  charset   = "UTF8"
  collation = "en_US.utf8"
}

# PostgreSQL server configurations
resource "azurerm_postgresql_flexible_server_configuration" "ssl_settings" {
  name      = "ssl_min_protocol_version"
  server_id = azurerm_postgresql_flexible_server.main.id
  value     = "TLSv1.2"
}

resource "azurerm_postgresql_flexible_server_configuration" "connection_throttling" {
  name      = "connection_throttling"
  server_id = azurerm_postgresql_flexible_server.main.id
  value     = "on"
}

resource "azurerm_postgresql_flexible_server_configuration" "log_checkpoints" {
  name      = "log_checkpoints"
  server_id = azurerm_postgresql_flexible_server.main.id
  value     = "on"
}

resource "azurerm_postgresql_flexible_server_configuration" "log_connections" {
  name      = "log_connections"
  server_id = azurerm_postgresql_flexible_server.main.id
  value     = "on"
}

resource "azurerm_postgresql_flexible_server_configuration" "log_disconnections" {
  name      = "log_disconnections"
  server_id = azurerm_postgresql_flexible_server.main.id
  value     = "on"
}

# Outputs for reference in other modules
output "server_id" {
  description = "The ID of the PostgreSQL Flexible Server"
  value       = azurerm_postgresql_flexible_server.main.id
}

output "server_fqdn" {
  description = "The FQDN of the PostgreSQL Flexible Server"
  value       = azurerm_postgresql_flexible_server.main.fqdn
}

output "administrator_login" {
  description = "The administrator login for the PostgreSQL Flexible Server"
  value       = azurerm_postgresql_flexible_server.main.administrator_login
  sensitive   = true
}