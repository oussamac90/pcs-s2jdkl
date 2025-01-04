/**
 * Production environment configuration for Vessel Call Management System
 * @version 1.0.0
 * @license Proprietary
 */

export const environment = {
  // Production flag
  production: true,

  // Core API endpoints
  apiUrl: 'https://api.vcms.com/api/v1',
  wsUrl: 'wss://api.vcms.com/ws',

  // External system API endpoints
  customsApiUrl: 'https://customs.vcms.com/api',
  immigrationApiUrl: 'https://immigration.vcms.com/api',
  vtsApiUrl: 'https://vts.vcms.com/api',
  weatherApiUrl: 'https://api.openweather.org/data/2.5',

  // API keys and tokens
  weatherApiKey: '${WEATHER_API_KEY}',
  mapboxToken: '${MAPBOX_TOKEN}',

  // OAuth2 configuration
  oauth: {
    clientId: 'vcms-prod-client',
    authUrl: 'https://auth.vcms.com/oauth/authorize',
    tokenUrl: 'https://auth.vcms.com/oauth/token',
    scope: 'read write',
    responseType: 'code',
    grantType: 'authorization_code',
    tokenValiditySeconds: 3600
  },

  // Cache configuration
  cacheConfig: {
    ttl: 1800, // 30 minutes in seconds
    maxSize: 1000, // Maximum number of cached items
    cleanupInterval: 300 // Cleanup every 5 minutes
  },

  // Monitoring configuration
  monitoring: {
    enabled: true,
    datadogEnabled: true,
    datadogConfig: {
      applicationId: 'vcms-prod',
      clientToken: '${DATADOG_CLIENT_TOKEN}',
      site: 'datadoghq.com',
      service: 'vcms-frontend'
    },
    sentryEnabled: true,
    sentryConfig: {
      dsn: '${SENTRY_DSN}',
      environment: 'production',
      tracesSampleRate: 0.1
    }
  },

  // Logging configuration
  logLevel: 'error',

  // API timeout configurations (in milliseconds)
  apiTimeouts: {
    default: 30000, // 30 seconds
    long: 60000 // 60 seconds for long-running operations
  },

  // WebSocket configuration
  wsConfig: {
    reconnectInterval: 5000, // 5 seconds
    maxRetries: 5
  }
};