import { NgModule } from '@angular/core';
import { StoreModule } from '@ngrx/store';
import { EffectsModule } from '@ngrx/effects';
import { WebSocketModule } from '@angular/websocket';

// Feature Components
import { VesselCallsComponent } from './vessel-calls.component';
import { VesselCallFormComponent } from './components/vessel-call-form/vessel-call-form.component';

// Routing
import { VesselCallsRoutingModule } from './vessel-calls-routing.module';

// Shared Module
import { SharedModule } from '../../shared/shared.module';

// State Management
import { vesselCallsReducer } from '../../store/reducers/vessel-call.reducer';
import { VesselCallEffects } from '../../store/effects/vessel-call.effects';

/**
 * Feature module that encapsulates all vessel calls functionality including
 * components, routing, state management, and real-time updates.
 * 
 * Implements requirements:
 * - Pre-Arrival Management
 * - User Interface Design
 * - Responsive Design
 * 
 * @version 1.0.0
 */
@NgModule({
  declarations: [
    VesselCallsComponent,
    VesselCallFormComponent
  ],
  imports: [
    // Core Angular and Shared Modules
    SharedModule,
    VesselCallsRoutingModule,
    
    // State Management
    StoreModule.forFeature('vesselCalls', vesselCallsReducer, {
      initialState: {
        entities: {},
        ids: [],
        loading: false,
        error: null,
        selectedId: null,
        lastUpdated: null,
        filterCriteria: {},
        sortCriteria: {
          field: 'eta',
          direction: 'asc'
        }
      }
    }),
    EffectsModule.forFeature([VesselCallEffects]),
    
    // Real-time Updates
    WebSocketModule.config({
      url: 'ws://localhost:8080/ws',
      reconnectAttempts: 5,
      reconnectInterval: 5000,
      heartbeatInterval: 30000
    })
  ],
  exports: [
    // Export main component for use in app routing
    VesselCallsComponent
  ],
  providers: [
    // Any feature-specific services would go here
  ]
})
export class VesselCallsModule {
  /**
   * Module name for debugging and logging purposes
   */
  static readonly moduleName = 'VesselCallsModule';

  /**
   * Flag indicating if the module has been loaded
   * Used for lazy loading optimization
   */
  static isLoaded = false;

  constructor() {
    VesselCallsModule.isLoaded = true;
    
    // Log module initialization in development
    if (process.env['NODE_ENV'] !== 'production') {
      console.debug(`${VesselCallsModule.moduleName} initialized`);
    }
  }
}