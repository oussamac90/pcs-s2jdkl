import { platformBrowserDynamic } from '@angular/platform-browser-dynamic'; // @angular/platform-browser-dynamic ^16.0.0
import { enableProdMode, ApplicationRef } from '@angular/core'; // @angular/core ^16.0.0
import 'zone.js'; // ~0.13.0

import { AppModule } from './app/app.module';
import { environment } from './environments/environment';

/**
 * Maximum number of bootstrap retry attempts
 */
const BOOTSTRAP_RETRY_ATTEMPTS = 3;

/**
 * Delay between retry attempts in milliseconds
 */
const BOOTSTRAP_RETRY_DELAY = 1000;

/**
 * Handles bootstrap errors with retry logic and monitoring
 * @param error The error that occurred during bootstrap
 */
function handleBootstrapError(error: Error): void {
  console.error('Application bootstrap error:', error);

  // Log error to monitoring service in production
  if (environment.production && environment.monitoring?.enabled) {
    const errorDetails = {
      type: 'BOOTSTRAP_ERROR',
      message: error.message,
      stack: error.stack,
      timestamp: new Date().toISOString()
    };

    // Send to monitoring services if configured
    if (environment.monitoring.datadog.enabled) {
      console.warn('Datadog error tracking:', errorDetails);
    }
    if (environment.monitoring.sentry.enabled) {
      console.warn('Sentry error tracking:', errorDetails);
    }
  }
}

/**
 * Bootstraps the Angular application with enhanced error handling
 * and performance monitoring
 */
async function bootstrapApplication(): Promise<void> {
  let retryCount = 0;

  // Enable production mode if environment is production
  if (environment.production) {
    enableProdMode();
  }

  // Configure Zone.js performance tracking
  if (environment.monitoring?.enabled) {
    (window as any).Zone.enableLongStackTrace = !environment.production;
    (window as any).Zone.trackingZoneSpec = true;
  }

  async function attemptBootstrap(): Promise<void> {
    try {
      const appRef = await platformBrowserDynamic().bootstrapModule(AppModule);

      // Configure performance monitoring in production
      if (environment.production && environment.monitoring?.enabled) {
        // Track change detection cycles
        const applicationRef = appRef.injector.get(ApplicationRef);
        applicationRef.tick();

        // Monitor stable state
        applicationRef.isStable.subscribe(stable => {
          if (stable) {
            const performanceMetrics = {
              timeToBootstrap: window.performance.now(),
              memoryUsage: (window.performance as any).memory?.usedJSHeapSize,
              timestamp: new Date().toISOString()
            };
            console.debug('Application stable:', performanceMetrics);
          }
        });

        // Enable runtime checks in development
        if (!environment.production) {
          (window as any).appRef = applicationRef;
        }
      }
    } catch (error) {
      handleBootstrapError(error as Error);

      // Retry bootstrap if attempts remain
      if (retryCount < BOOTSTRAP_RETRY_ATTEMPTS) {
        retryCount++;
        console.warn(`Retrying bootstrap attempt ${retryCount} of ${BOOTSTRAP_RETRY_ATTEMPTS}`);
        await new Promise(resolve => setTimeout(resolve, BOOTSTRAP_RETRY_DELAY));
        return attemptBootstrap();
      } else {
        throw error;
      }
    }
  }

  await attemptBootstrap();
}

// Initialize application bootstrap
bootstrapApplication().catch(error => {
  console.error('Fatal application error:', error);
  // Display user-friendly error message
  document.body.innerHTML = `
    <div style="padding: 20px; text-align: center;">
      <h1>Application Error</h1>
      <p>We're sorry, but the application failed to start. Please try refreshing the page.</p>
    </div>
  `;
});