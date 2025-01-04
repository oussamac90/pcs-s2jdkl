import { 
  Component, 
  Input, 
  Output, 
  EventEmitter, 
  OnInit, 
  OnDestroy, 
  ChangeDetectionStrategy, 
  ChangeDetectorRef, 
  TemplateRef 
} from '@angular/core'; // @angular/core ^16.0.0
import { Subject, Subscription } from 'rxjs'; // rxjs ^7.8.0
import { takeUntil } from 'rxjs/operators'; // rxjs ^7.8.0
import { ErrorService, ErrorSeverity } from '../../../core/services/error.service';
import { ApiErrorResponse } from '../../models/api-response.model';

/**
 * Enhanced error component that provides standardized error message display
 * with support for different severity levels, retry capabilities, and accessibility features.
 * Implements Material Design patterns and WCAG 2.1 Level AA compliance.
 * @version 1.0.0
 */
@Component({
  selector: 'app-error',
  templateUrl: './error.component.html',
  styleUrls: ['./error.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ErrorComponent implements OnInit, OnDestroy {
  // Input properties with enhanced type safety and documentation
  /** Custom title for the error message */
  @Input() errorTitle?: string;
  
  /** Main error message to display */
  @Input() errorMessage?: string;
  
  /** Detailed error information for debugging */
  @Input() errorDetails?: string[];
  
  /** Error severity level affecting display style */
  @Input() severity: ErrorSeverity = 'error';
  
  /** Flag to show/hide retry button */
  @Input() showRetry = false;
  
  /** Flag to show/hide dismiss button */
  @Input() showDismiss = true;
  
  /** Flag to prevent auto-dismissal of error */
  @Input() persistent = false;
  
  /** Custom template for error display */
  @Input() customTemplate?: TemplateRef<any>;

  // Output events for component interaction
  /** Event emitted when retry button is clicked */
  @Output() retry = new EventEmitter<void>();
  
  /** Event emitted when dismiss button is clicked */
  @Output() dismiss = new EventEmitter<void>();

  // Component state management
  /** Subject for managing subscriptions cleanup */
  private readonly destroy$ = new Subject<void>();
  
  /** Subscription for error service updates */
  private errorSubscription?: Subscription;
  
  /** State for error details expansion */
  isExpanded = false;
  
  /** ARIA live region announcement mode */
  ariaLive = 'assertive';

  /** Error icon mapping based on severity */
  readonly errorIcons: Record<ErrorSeverity, string> = {
    error: 'error',
    warning: 'warning',
    info: 'info'
  };

  /** CSS classes based on severity */
  readonly severityClasses: Record<ErrorSeverity, string> = {
    error: 'error-severity',
    warning: 'warning-severity',
    info: 'info-severity'
  };

  constructor(
    private errorService: ErrorService,
    private cdr: ChangeDetectorRef
  ) {}

  /**
   * Initializes component and sets up error subscription with enhanced error handling
   */
  ngOnInit(): void {
    // Subscribe to error service updates
    this.errorSubscription = this.errorService.error$
      .pipe(takeUntil(this.destroy$))
      .subscribe((error: ApiErrorResponse | null) => {
        if (error) {
          this.errorMessage = error.message;
          this.errorDetails = error.details;
          this.severity = this.errorService.getErrorSeverity(error);
          
          // Update ARIA live region based on severity
          this.ariaLive = this.severity === 'error' ? 'assertive' : 'polite';
          
          // Auto-clear non-persistent errors
          if (!this.persistent) {
            setTimeout(() => this.onDismiss(), 5000);
          }
          
          this.cdr.markForCheck();
        }
      });
  }

  /**
   * Cleans up subscriptions and resources on component destruction
   */
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    
    if (this.errorSubscription) {
      this.errorSubscription.unsubscribe();
    }

    if (!this.persistent) {
      this.errorService.clearError();
    }
  }

  /**
   * Handles retry button click with error clearing
   */
  onRetry(): void {
    this.retry.emit();
    this.errorService.clearError();
    this.cdr.markForCheck();
    
    // Announce retry action for screen readers
    this.announceForScreenReader('Retrying operation');
  }

  /**
   * Handles dismiss button click with accessibility updates
   */
  onDismiss(): void {
    this.dismiss.emit();
    this.errorService.clearError();
    
    // Announce dismissal for screen readers
    this.announceForScreenReader('Error dismissed');
    
    this.cdr.markForCheck();
  }

  /**
   * Toggles visibility of error details with accessibility support
   */
  toggleDetails(): void {
    this.isExpanded = !this.isExpanded;
    
    // Announce expansion state for screen readers
    this.announceForScreenReader(
      `Error details ${this.isExpanded ? 'expanded' : 'collapsed'}`
    );
    
    this.cdr.markForCheck();
  }

  /**
   * Gets the appropriate icon based on error severity
   */
  getErrorIcon(): string {
    return this.errorIcons[this.severity];
  }

  /**
   * Gets CSS classes based on severity and state
   */
  getErrorClasses(): string[] {
    return [
      'error-container',
      this.severityClasses[this.severity],
      this.isExpanded ? 'expanded' : ''
    ];
  }

  /**
   * Makes announcements for screen readers
   */
  private announceForScreenReader(message: string): void {
    const announcer = document.createElement('div');
    announcer.setAttribute('aria-live', this.ariaLive);
    announcer.classList.add('sr-only');
    announcer.textContent = message;
    
    document.body.appendChild(announcer);
    setTimeout(() => document.body.removeChild(announcer), 1000);
  }
}