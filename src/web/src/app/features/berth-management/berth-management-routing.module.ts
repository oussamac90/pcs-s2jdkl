import { NgModule } from '@angular/core'; // @angular/core v16.x
import { RouterModule, Routes } from '@angular/router'; // @angular/router v16.x

import { BerthManagementComponent } from './berth-management.component';
import { AuthGuard } from '../../core/auth/auth.guard';
import { UserRole } from '../../core/auth/user.model';

/**
 * Routes configuration for the berth management feature module.
 * Implements role-based access control and secure route protection.
 */
const routes: Routes = [
  {
    path: '',
    component: BerthManagementComponent,
    canActivate: [AuthGuard],
    data: {
      roles: [UserRole.PORT_AUTHORITY, UserRole.SYSTEM_ADMIN],
      title: 'Berth Management',
      audit: true,
      permissions: ['VIEW_BERTH', 'MANAGE_BERTH'],
      breadcrumb: 'Berth Management'
    }
  }
];

/**
 * Routing module for berth management feature.
 * Configures secure routes with role-based access control.
 */
@NgModule({
  imports: [
    RouterModule.forChild(routes)
  ],
  exports: [
    RouterModule
  ]
})
export class BerthManagementRoutingModule { }