import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DashboardComponent } from './dashboard.component';
import { AuthGuard } from '../../core/auth/auth.guard';
import { UserRole } from '../../core/auth/user.model';

/**
 * Routes configuration for the dashboard feature module.
 * Implements role-based access control and secure routing for the vessel call management dashboard.
 */
const routes: Routes = [
  {
    path: '',
    component: DashboardComponent,
    canActivate: [AuthGuard],
    data: {
      roles: [
        UserRole.PORT_AUTHORITY,
        UserRole.VESSEL_AGENT,
        UserRole.SERVICE_PROVIDER,
        UserRole.SYSTEM_ADMIN
      ],
      title: 'Dashboard - Vessel Call Management System',
      reuse: true, // Enable route reuse for performance optimization
      breadcrumb: 'Dashboard' // For navigation hierarchy
    },
    runGuardsAndResolvers: 'always', // Ensure security checks on every navigation
    children: [] // No child routes currently defined for dashboard
  }
];

/**
 * Dashboard routing module that configures secure routes with role-based access control.
 * Implements lazy loading and route protection for the dashboard feature.
 */
@NgModule({
  imports: [
    RouterModule.forChild(routes)
  ],
  exports: [
    RouterModule
  ]
})
export class DashboardRoutingModule {
  constructor() {
    // Module initialization logic if needed
  }
}