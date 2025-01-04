import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

// Material Modules
import { MatCardModule } from '@angular/material/card'; // @angular/material/card ^16.0.0
import { MatButtonModule } from '@angular/material/button'; // @angular/material/button ^16.0.0
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner'; // @angular/material/progress-spinner ^16.0.0
import { MatIconModule } from '@angular/material/icon'; // @angular/material/icon ^16.0.0
import { MatTooltipModule } from '@angular/material/tooltip'; // @angular/material/tooltip ^16.0.0
import { MatSnackBarModule } from '@angular/material/snack-bar'; // @angular/material/snack-bar ^16.0.0
import { MatDialogModule } from '@angular/material/dialog'; // @angular/material/dialog ^16.0.0

// Custom Components
import { ErrorComponent } from './components/error/error.component';
import { LoadingComponent } from './components/loading/loading.component';

// Directives
import { ClickOutsideDirective } from './directives/click-outside.directive';

// Pipes
import { DateFormatPipe } from './pipes/date-format.pipe';

/**
 * SharedModule centralizes common functionality, components, directives and pipes
 * used across the Vessel Call Management System frontend application.
 * Implements WCAG 2.1 Level AA compliance and Material Design integration.
 */
@NgModule({
  declarations: [
    // Components
    ErrorComponent,
    LoadingComponent,
    // Directives
    ClickOutsideDirective,
    // Pipes
    DateFormatPipe
  ],
  imports: [
    // Angular Modules
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    
    // Material Design Modules
    MatCardModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatIconModule,
    MatTooltipModule,
    MatSnackBarModule,
    MatDialogModule
  ],
  exports: [
    // Angular Modules
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    
    // Custom Components
    ErrorComponent,
    LoadingComponent,
    
    // Directives
    ClickOutsideDirective,
    
    // Pipes
    DateFormatPipe,
    
    // Material Design Modules
    MatCardModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatIconModule,
    MatTooltipModule,
    MatSnackBarModule,
    MatDialogModule
  ]
})
export class SharedModule {}