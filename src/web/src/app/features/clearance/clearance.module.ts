import { NgModule } from '@angular/core';
import { StoreModule } from '@ngrx/store';
import { EffectsModule } from '@ngrx/effects';

// Feature Components
import { ClearanceComponent } from './clearance.component';
import { ClearanceWorkflowComponent } from './components/clearance-workflow/clearance-workflow.component';

// Routing
import { ClearanceRoutingModule } from './clearance-routing.module';

// Shared Module
import { SharedModule } from '../../shared/shared.module';

// State Management
import { clearanceReducer } from '../../store/reducers/clearance.reducer';
import { ClearanceEffects } from '../../store/effects/clearance.effects';

// Services
import { ClearanceService } from '../../core/services/clearance.service';
import { ClearanceWebSocketService } from '../../core/services/clearance-websocket.service';

/**
 * Feature module that encapsulates all clearance-related functionality.
 * Implements digital workflows, regulatory compliance checks, and real-time
 * status updates for vessel clearance management.
 * 
 * @remarks
 * This module follows the technical specifications for:
 * - Digital clearance workflows
 * - Regulatory compliance checks
 * - Departure approval automation
 * - Real-time status updates
 * 
 * @version 1.0.0
 */
@NgModule({
  declarations: [
    ClearanceComponent,
    ClearanceWorkflowComponent
  ],
  imports: [
    // Feature routing
    ClearanceRoutingModule,
    
    // Shared module containing common components and Material modules
    SharedModule,
    
    // NgRx store configuration for clearance state management
    StoreModule.forFeature('clearances', clearanceReducer),
    
    // NgRx effects for handling side effects in clearance operations
    EffectsModule.forFeature([ClearanceEffects])
  ],
  providers: [
    // Core services for clearance management
    ClearanceService,
    ClearanceWebSocketService
  ]
})
export class ClearanceModule {
  /**
   * Initializes the clearance feature module with required dependencies
   * and sets up state management for clearance operations.
   */
  constructor() {
    console.debug('ClearanceModule initialized');
  }
}