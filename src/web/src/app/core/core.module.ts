import { NgModule, Optional, SkipSelf } from '@angular/core'; // @angular/core v16.0.0
import { CommonModule } from '@angular/common'; // @angular/common v16.0.0
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http'; // @angular/common/http v16.0.0
import { AuthInterceptor } from '@auth0/angular-jwt'; // @auth0/angular-jwt v5.1.0

// Internal service imports
import { AuthService } from './auth/auth.service';
import { ApiService } from './http/api.service';
import { WebSocketService } from './services/websocket.service';

/**
 * Error message for multiple CoreModule imports
 */
const MODULE_IMPORT_ERROR = 'CoreModule is already loaded. Import it in the AppModule only. Multiple imports of CoreModule can cause unpredictable behavior and memory leaks.';

/**
 * Core module that provides essential services and configurations for the Vessel Call Management System.
 * This module should be imported only once in the root AppModule.
 * 
 * Features:
 * - Authentication and authorization services
 * - HTTP communication with backend API
 * - Real-time WebSocket updates
 * - Security configurations
 */
@NgModule({
  imports: [
    CommonModule,
    HttpClientModule
  ],
  providers: [
    // Core services
    AuthService,
    ApiService,
    WebSocketService,

    // HTTP interceptors
    {
      provide: HTTP_INTERCEPTORS,
      useClass: AuthInterceptor,
      multi: true
    }
  ]
})
export class CoreModule {
  /**
   * Enforces singleton pattern by preventing multiple imports of CoreModule
   * @param parentModule - Reference to CoreModule if it's already loaded
   * @throws Error if attempting to import CoreModule more than once
   */
  constructor(
    @Optional() @SkipSelf() parentModule?: CoreModule
  ) {
    if (parentModule) {
      throw new Error(MODULE_IMPORT_ERROR);
    }
  }
}