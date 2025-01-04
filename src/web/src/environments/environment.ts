/**
 * Development environment configuration for the Vessel Call Management System frontend.
 * @version 1.0.0
 * @description Contains environment-specific variables, API endpoints, security settings,
 * and monitoring configuration for local development environment.
 */

export const environment = {
  // Environment type flag
  production: false,

  // Core API endpoints
  apiUrl: 'http://localhost:8080/api/v1',
  wsUrl: 'ws://localhost:8080/ws',

  // External service API endpoints
  customsApiUrl: 'http://localhost:8081/api',
  immigrationApiUrl: 'http://localhost:8082/api',
  vtsApiUrl: 'http://localhost:8083/api',
  weatherApiUrl: 'https://api.openweather.org/data/2.5',

  // API keys and tokens
  weatherApiKey: 'dev_weather_api_key',
  mapboxToken: 'dev_mapbox_token',

  // OAuth2 configuration
  oauth: {
    clientId: 'vcms-dev-client',
    clientSecret: 'dev-secret',
    authUrl: 'http://localhost:8084/oauth/authorize',
    tokenUrl: 'http://localhost:8084/oauth/token',
    scope: 'read write',
    responseType: 'code',
    grantType: 'authorization_code',
    refreshTokenEnabled: true
  },

  // Cache configuration
  cacheConfig: {
    enabled: true,
    ttl: 300, // Time to live in seconds
    maxSize: 100, // Maximum number of cached items
    storageType: 'memory',
    debugEnabled: true
  },

  // Monitoring and observability settings
  monitoring: {
    enabled: true,
    logLevel: 'debug',
    datadog: {
      enabled: false,
      apiKey: 'dev_datadog_api_key',
      appKey: 'dev_datadog_app_key',
      service: 'vcms-frontend-dev'
    },
    sentry: {
      enabled: false,
      dsn: 'dev_sentry_dsn',
      environment: 'development',
      tracesSampleRate: 1.0
    }
  },

  // Security configuration
  security: {
    corsEnabled: true,
    allowedOrigins: ['http://localhost:4200'],
    csrfEnabled: true,
    contentSecurityPolicy: {
      enabled: true,
      reportOnly: true
    },
    sslEnabled: false
  },

  // Feature flags
  features: {
    vesselTracking: true,
    weatherForecast: true,
    berthOptimization: true,
    realTimeAlerts: true,
    debugTools: true
  }
};