import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AuthGuard } from '@auth0/angular-jwt';
import { RoleGuard } from '@auth0/angular-jwt';
import { FormDeactivateGuard } from '@angular/router';

import { VesselCallsComponent } from './vessel-calls.component';
import { VesselCallFormComponent } from './components/vessel-call-form/vessel-call-form.component';

/**
 * Routes configuration for the Vessel Calls feature module.
 * Implements role-based access control and form protection.
 */
const routes: Routes = [
  {
    path: '',
    component: VesselCallsComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: {
      title: 'Vessel Calls',
      roles: ['PORT_AUTHORITY', 'VESSEL_AGENT'],
      breadcrumb: 'Vessel Calls'
    },
    children: [
      {
        path: 'new',
        component: VesselCallFormComponent,
        canActivate: [RoleGuard],
        canDeactivate: [FormDeactivateGuard],
        data: {
          title: 'New Vessel Call',
          roles: ['PORT_AUTHORITY', 'VESSEL_AGENT'],
          breadcrumb: 'New'
        }
      },
      {
        path: ':id/edit',
        component: VesselCallFormComponent,
        canActivate: [RoleGuard],
        canDeactivate: [FormDeactivateGuard],
        data: {
          title: 'Edit Vessel Call',
          roles: ['PORT_AUTHORITY'],
          breadcrumb: 'Edit'
        }
      }
    ]
  }
];

/**
 * Routing module for the Vessel Calls feature.
 * Provides secure navigation with role-based access control and form protection.
 * @version 1.0.0
 */
@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class VesselCallsRoutingModule { }