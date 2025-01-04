import { NgModule } from '@angular/core'; // @angular/core ^16.0.0
import { CommonModule } from '@angular/common'; // @angular/common ^16.0.0
import { FormsModule, ReactiveFormsModule } from '@angular/forms'; // @angular/forms ^16.0.0
import { StoreModule } from '@ngrx/store'; // @ngrx/store ^16.0.0
import { EffectsModule } from '@ngrx/effects'; // @ngrx/effects ^16.0.0
import { MatDatepickerModule } from '@angular/material/datepicker'; // @angular/material/datepicker ^16.0.0
import { MatButtonModule } from '@angular/material/button'; // @angular/material/button ^16.0.0

// Feature Components
import { BerthManagementComponent } from './berth-management.component';
import { BerthTimelineComponent } from './components/berth-timeline/berth-timeline.component';

// Routing
import { BerthManagementRoutingModule } from './berth-management-routing.module';

// Shared Module
import { SharedModule } from '../../shared/shared.module';

// State Management
import { berthAllocationReducer } from './store/reducers/berth-allocation.reducer';
import { BerthAllocationEffects } from './store/effects/berth-allocation.effects';

/**
 * Feature module for berth management functionality.
 * Implements comprehensive berth planning, allocation management, and real-time updates
 * with NgRx state management and Material Design components.
 * 
 * @remarks
 * This module follows the technical specifications for:
 * - Automated berth allocation algorithms
 * - Conflict resolution
 * - Schedule optimization
 * - Real-time updates
 * - Role-based access control
 */
@NgModule({
  declarations: [
    BerthManagementComponent,
    BerthTimelineComponent
  ],
  imports: [
    // Angular Core Modules
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    
    // Feature Routing
    BerthManagementRoutingModule,
    
    // Shared Module with Common Components
    SharedModule,
    
    // NgRx State Management
    StoreModule.forFeature('berthAllocations', berthAllocationReducer),
    EffectsModule.forFeature([BerthAllocationEffects]),
    
    // Material Design Modules
    MatDatepickerModule,
    MatButtonModule
  ],
  providers: [
    // Any feature-specific providers would go here
  ],
  exports: [
    // Export main component for potential external use
    BerthManagementComponent
  ]
})
export class BerthManagementModule {
  constructor() {
    console.debug('BerthManagementModule initialized');
  }
}