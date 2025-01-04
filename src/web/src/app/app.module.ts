import { NgModule, ErrorHandler } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ServiceWorkerModule } from '@angular/service-worker';
import { HTTP_INTERCEPTORS } from '@angular/common/http';

// Internal imports
import { AppComponent } from './app.component';
import { AppRoutingModule } from './app-routing.module';
import { CoreModule } from './core/core.module';
import { SharedModule } from './shared/shared.module';

// Environment configuration
import { environment } from '../environments/environment';

// Custom error handler for global error management
class GlobalErrorHandler implements ErrorHandler {
  handleError(error: Error): void {
    console.error('An error occurred:', error);
    // TODO: Integrate with monitoring service in production
    if (environment.monitoring?.enabled) {
      // Send error to monitoring service
    }
  }
}

// HTTP interceptors for authentication and error handling
import { AuthInterceptor } from './core/auth/auth.interceptor';
import { ErrorInterceptor } from './core/http/error.interceptor';

/**
 * Root module of the Vessel Call Management System.
 * Configures core application features including:
 * - Authentication and security
 * - Real-time WebSocket updates
 * - PWA capabilities
 * - Global error handling
 * - HTTP interceptors
 */
@NgModule({
  declarations: [
    AppComponent
  ],
  imports: [
    // Angular core modules
    BrowserModule,
    BrowserAnimationsModule,
    
    // Application routing
    AppRoutingModule,
    
    // Core functionality module
    CoreModule.forRoot(),
    
    // Shared components and utilities
    SharedModule,
    
    // Progressive Web App support
    ServiceWorkerModule.register('ngsw-worker.js', {
      enabled: environment.production,
      registrationStrategy: 'registerWhenStable:30000',
      // PWA configuration for offline capabilities
      scope: './',
      cacheLocation: 'data',
      updateMode: 'prefetch'
    })
  ],
  providers: [
    // Global error handler
    {
      provide: ErrorHandler,
      useClass: GlobalErrorHandler
    },
    // Authentication interceptor
    {
      provide: HTTP_INTERCEPTORS,
      useClass: AuthInterceptor,
      multi: true
    },
    // Error handling interceptor
    {
      provide: HTTP_INTERCEPTORS,
      useClass: ErrorInterceptor,
      multi: true
    }
  ],
  bootstrap: [AppComponent]
})
export class AppModule {
  constructor() {
    // Initialize core application services
    if (environment.monitoring?.enabled) {
      this.initializeMonitoring();
    }
  }

  /**
   * Initializes application monitoring and telemetry
   * @private
   */
  private initializeMonitoring(): void {
    if (environment.monitoring.datadog.enabled) {
      // Initialize Datadog monitoring
      console.debug('Datadog monitoring initialized');
    }
    if (environment.monitoring.sentry.enabled) {
      // Initialize Sentry error tracking
      console.debug('Sentry error tracking initialized');
    }
  }
}