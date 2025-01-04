import { NgModule } from '@angular/core';

// Feature Components
import { ServiceBookingComponent } from './service-booking.component';
import { ServiceFormComponent } from './components/service-form/service-form.component';

// Routing Module
import { ServiceBookingRoutingModule } from './service-booking-routing.module';

// Shared Module with Common Dependencies
import { SharedModule } from '../../shared/shared.module';

/**
 * Feature module that encapsulates all service booking functionality for the Vessel Call Management System.
 * Implements digital booking system with resource tracking and service confirmation workflow.
 * Supports WCAG 2.1 Level AA compliance and follows Material Design patterns.
 * @version 1.0.0
 */
@NgModule({
  declarations: [
    ServiceBookingComponent,
    ServiceFormComponent
  ],
  imports: [
    // Feature routing module
    ServiceBookingRoutingModule,
    
    // Shared module containing common dependencies
    SharedModule
  ],
  exports: [
    // Export main component for use in other modules
    ServiceBookingComponent
  ]
})
export class ServiceBookingModule {
  constructor() {
    // Initialize module with optimized configuration
    this.setupModuleConfiguration();
  }

  /**
   * Configures module with optimized settings for production use
   * @private
   */
  private setupModuleConfiguration(): void {
    // Module configuration is handled through Angular's DI system
    // Additional runtime configuration can be added here if needed
  }
}