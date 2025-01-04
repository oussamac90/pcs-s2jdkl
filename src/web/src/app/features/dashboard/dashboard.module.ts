import { NgModule } from '@angular/core';
import { DashboardComponent } from './dashboard.component';
import { DashboardRoutingModule } from './dashboard-routing.module';
import { SharedModule } from '../../shared/shared.module';

/**
 * Feature module that configures and exports the dashboard functionality
 * of the Vessel Call Management System. Implements lazy loading for optimized
 * performance and integrates real-time vessel monitoring capabilities.
 * 
 * @version 1.0.0
 */
@NgModule({
  declarations: [
    DashboardComponent
  ],
  imports: [
    // Core feature routing with lazy loading support
    DashboardRoutingModule,
    
    // Shared module containing Material Design components and common utilities
    SharedModule
  ]
})
export class DashboardModule {
  constructor() {
    // Module initialization logic if needed
    console.debug('DashboardModule initialized');
  }
}