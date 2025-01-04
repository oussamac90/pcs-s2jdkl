import { Injectable } from '@angular/core'; // @angular/core v16.x
import { BehaviorSubject, Observable } from 'rxjs'; // rxjs v7.x
import { debounceTime, delay } from 'rxjs/operators'; // rxjs v7.x

/**
 * Service that manages application-wide loading state with enhanced error handling 
 * and performance optimization. Supports the system's <3 second response time requirement
 * by providing visual feedback during operations.
 */
@Injectable({
  providedIn: 'root'
})
export class LoadingService {
  /**
   * Subject that tracks the current loading state
   * @private
   */
  private loadingSubject = new BehaviorSubject<boolean>(false);

  /**
   * Counter for nested loading operations
   * @private
   */
  private loadingStack = 0;

  /**
   * Minimum time in milliseconds to show loading indicator for UX consistency
   * @private
   * @readonly
   */
  private readonly MINIMUM_DISPLAY_TIME = 300;

  /**
   * Debounce time in milliseconds to prevent flickering for rapid state changes
   * @private
   * @readonly
   */
  private readonly DEBOUNCE_TIME = 100;

  /**
   * Observable of the current loading state with debounce and minimum display time
   * Implements performance optimization through debouncing and enforces minimum display
   * time for better user experience
   */
  isLoading$: Observable<boolean> = this.loadingSubject.pipe(
    debounceTime(this.DEBOUNCE_TIME),
    delay(state => state ? 0 : this.MINIMUM_DISPLAY_TIME)
  );

  constructor() {
    console.debug('LoadingService initialized');
  }

  /**
   * Shows the loading indicator and increments the loading stack.
   * Supports nested loading operations through stack-based tracking.
   */
  show(): void {
    this.loadingStack++;
    if (this.loadingStack > 0) {
      this.loadingSubject.next(true);
      console.debug(`Loading started (stack: ${this.loadingStack})`);
    }
  }

  /**
   * Hides the loading indicator and decrements the loading stack.
   * Only hides the indicator when all loading operations are complete.
   */
  hide(): void {
    this.loadingStack = Math.max(0, this.loadingStack - 1);
    if (this.loadingStack === 0) {
      this.loadingSubject.next(false);
      console.debug('Loading completed');
    } else {
      console.debug(`Loading continues (stack: ${this.loadingStack})`);
    }
  }

  /**
   * Resets the loading state and stack.
   * Useful for error recovery or application state reset.
   */
  reset(): void {
    this.loadingStack = 0;
    this.loadingSubject.next(false);
    console.debug('Loading state reset');
  }
}