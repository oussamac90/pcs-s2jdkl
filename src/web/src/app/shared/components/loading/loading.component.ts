import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core'; // @angular/core v16.x
import { Subscription, catchError, timeout } from 'rxjs'; // rxjs v7.x
import { ThemePalette } from '@angular/material/core'; // @angular/material/core v16.x
import { LoadingService } from '../../../core/services/loading.service';

/**
 * A reusable loading indicator component that provides visual feedback during
 * application operations with Material Design spinner and accessibility support.
 * Implements system response time requirements and WCAG 2.1 Level AA compliance.
 */
@Component({
  selector: 'app-loading',
  templateUrl: './loading.component.html',
  styleUrls: ['./loading.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LoadingComponent implements OnInit, OnDestroy {
  /**
   * Diameter of the spinner in pixels
   * @default 50
   */
  diameter = 50;

  /**
   * Theme color of the spinner
   * @default 'primary'
   */
  color: ThemePalette = 'primary';

  /**
   * Whether to show loading text
   * @default true
   */
  showText = true;

  /**
   * Customizable loading text for accessibility
   * @default 'Loading...'
   */
  loadingText = 'Loading...';

  /**
   * Duration in ms before timing out loading state
   * @default 30000 (30 seconds)
   */
  timeoutDuration = 30000;

  /**
   * Current visibility state of loading indicator
   * @private
   */
  visible = false;

  /**
   * Error state flag for loading timeout
   * @private
   */
  hasError = false;

  /**
   * Subscription to loading service
   * @private
   */
  private subscription: Subscription = new Subscription();

  constructor(
    private loadingService: LoadingService,
    private cdr: ChangeDetectorRef
  ) {}

  /**
   * Initializes the loading state subscription with error handling
   * and sets up accessibility attributes
   */
  ngOnInit(): void {
    this.subscription = this.loadingService.isLoading$
      .pipe(
        timeout(this.timeoutDuration),
        catchError(error => {
          this.handleLoadingError(error);
          return [];
        })
      )
      .subscribe(isLoading => {
        this.visible = isLoading;
        this.hasError = false;
        
        // Update ARIA attributes for accessibility
        if (this.visible) {
          document.body.setAttribute('aria-busy', 'true');
          document.body.setAttribute('aria-live', 'polite');
        } else {
          document.body.removeAttribute('aria-busy');
          document.body.removeAttribute('aria-live');
        }
        
        this.cdr.markForCheck();
      });
  }

  /**
   * Cleans up subscriptions and resets states on component destruction
   */
  ngOnDestroy(): void {
    if (this.subscription) {
      this.subscription.unsubscribe();
    }
    
    // Reset ARIA attributes
    document.body.removeAttribute('aria-busy');
    document.body.removeAttribute('aria-live');
    
    this.visible = false;
    this.hasError = false;
  }

  /**
   * Handles loading timeout or error states
   * @param error - The error that occurred
   * @private
   */
  private handleLoadingError(error: Error): void {
    console.error('Loading timeout or error:', error);
    this.hasError = true;
    this.visible = false;
    this.cdr.markForCheck();
  }
}