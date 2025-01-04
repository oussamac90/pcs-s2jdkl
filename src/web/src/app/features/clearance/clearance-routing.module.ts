import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ClearanceComponent } from './clearance.component';
import { ClearanceWorkflowComponent } from './components/clearance-workflow/clearance-workflow.component';

/**
 * Routes configuration for the Clearance feature module.
 * Implements hierarchical routing with lazy loading support.
 */
const routes: Routes = [
  {
    path: '',
    component: ClearanceComponent,
    children: [
      {
        path: '',
        redirectTo: 'list',
        pathMatch: 'full'
      },
      {
        path: 'list',
        component: ClearanceComponent,
        data: {
          title: 'Clearance List',
          roles: ['PORT_AUTHORITY', 'VESSEL_AGENT', 'CUSTOMS_OFFICER'],
          animation: 'clearanceList'
        }
      },
      {
        path: ':id/workflow',
        component: ClearanceWorkflowComponent,
        data: {
          title: 'Clearance Workflow',
          roles: ['PORT_AUTHORITY', 'CUSTOMS_OFFICER'],
          animation: 'clearanceWorkflow',
          reuse: true
        }
      },
      {
        path: 'new',
        component: ClearanceWorkflowComponent,
        data: {
          title: 'New Clearance Request',
          roles: ['VESSEL_AGENT'],
          animation: 'newClearance'
        }
      }
    ]
  }
];

/**
 * Routing module for the Clearance feature that configures all routes
 * related to clearance management with security, data resolution,
 * and animation support.
 */
@NgModule({
  imports: [
    RouterModule.forChild(routes),
    BrowserAnimationsModule
  ],
  exports: [RouterModule]
})
export class ClearanceRoutingModule {
  private navigationTrackingEnabled: boolean = false;

  constructor() {
    this.enableNavigationTracking(true);
  }

  /**
   * Enables tracking of route navigation for analytics
   * @param enabled Flag to enable/disable tracking
   */
  enableNavigationTracking(enabled: boolean): void {
    this.navigationTrackingEnabled = enabled;
  }
}