import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ServiceBookingComponent } from './service-booking.component';
import { ServiceFormComponent } from './components/service-form/service-form.component';

// Route configuration with enhanced metadata for service booking feature
const routes: Routes = [
  {
    path: '',
    component: ServiceBookingComponent,
    data: {
      title: 'Service Booking',
      breadcrumb: 'Service Booking',
      animation: 'ServiceBookingPage',
      permissions: ['VIEW_SERVICE_BOOKINGS'],
      cacheStrategy: 'reuse'
    },
    children: [
      {
        path: 'new',
        component: ServiceFormComponent,
        data: {
          title: 'New Service Booking',
          breadcrumb: 'New Booking',
          animation: 'ServiceBookingFormPage',
          permissions: ['CREATE_SERVICE_BOOKING'],
          formState: 'new'
        }
      },
      {
        path: ':id/edit',
        component: ServiceFormComponent,
        data: {
          title: 'Edit Service Booking',
          breadcrumb: 'Edit Booking',
          animation: 'ServiceBookingFormPage',
          permissions: ['EDIT_SERVICE_BOOKING'],
          formState: 'edit'
        }
      }
    ]
  }
];

/**
 * Routing module for the Service Booking feature.
 * Implements comprehensive routing configuration with enhanced navigation features,
 * security, and optimization for service listing, creation, and management.
 * @version 1.0.0
 */
@NgModule({
  imports: [
    RouterModule.forChild(routes),
    BrowserAnimationsModule
  ],
  exports: [RouterModule]
})
export class ServiceBookingRoutingModule {
  constructor() {
    this.setupRouteReuse();
  }

  /**
   * Configures route reuse strategy for optimized navigation
   * within the service booking feature
   */
  private setupRouteReuse(): void {
    // Route reuse configuration is handled by the custom
    // route reuse strategy at the app level
  }
}