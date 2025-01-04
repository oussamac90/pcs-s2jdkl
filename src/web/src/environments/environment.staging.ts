/**
 * Staging Environment Configuration
 * Version: 1.0.0
 * 
 * This file contains environment-specific configuration for the staging/UAT deployment
 * of the Vessel Call Management System frontend application.
 */

export const environment = {
  // Environment type flag
  production: false,

  // Core API endpoints
  apiUrl: 'https://staging-api.vcms.com/api/v1',
  wsUrl: 'wss://staging-api.vcms.com/ws',

  // External system integration endpoints
  customsApiUrl: 'https://staging-customs.vcms.com/api',
  immigrationApiUrl: 'https://staging-immigration.vcms.com/api',
  vtsApiUrl: 'https://staging-vts.vcms.com/api',
  weatherApiUrl: 'https://api.openweather.org/data/2.5',

  // OAuth2 configuration
  oauth: {
    clientId: 'vcms-staging-client',
    authUrl: 'https://staging-auth.vcms.com/oauth/authorize',
    tokenUrl: 'https://staging-auth.vcms.com/oauth/token',
    scope: 'read write',
    responseType: 'code',
    grantType: 'authorization_code',
    tokenValidityMinutes: 60,
    refreshTokenValidityMinutes: 1440
  },

  // Monitoring and observability configuration
  monitoring: {
    enabled: true,
    datadog: {
      enabled: true,
      applicationId: 'staging-vcms',
      clientToken: '${DATADOG_CLIENT_TOKEN}',
      site: 'datadoghq.com',
      service: 'vcms-frontend',
      env: 'staging'
    },
    sentry: {
      enabled: true,
      dsn: '${SENTRY_DSN}',
      environment: 'staging',
      tracesSampleRate: 1.0
    }
  },

  // Cache configuration
  cacheConfig: {
    enabled: true,
    ttl: 600, // Time to live in seconds
    maxSize: 500, // Maximum number of items in cache
    strategy: 'lru' // Least Recently Used eviction strategy
  },

  // Map configuration for vessel tracking
  mapConfig: {
    provider: 'mapbox',
    apiKey: '${MAPBOX_API_KEY}',
    style: 'mapbox://styles/mapbox/streets-v11',
    center: {
      lat: 0,
      lng: 0
    },
    zoom: 2
  }
};