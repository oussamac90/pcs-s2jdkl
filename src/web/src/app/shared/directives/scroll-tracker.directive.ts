import { 
  Directive, 
  ElementRef, 
  EventEmitter, 
  HostListener, 
  Input, 
  Output, 
  OnDestroy, 
  OnInit, 
  NgZone 
} from '@angular/core'; // @angular/core v16.x
import { 
  Subject, 
  fromEvent, 
  debounceTime, 
  takeUntil, 
  distinctUntilChanged 
} from 'rxjs'; // rxjs v7.x

/**
 * Directive that provides optimized scroll tracking functionality with cross-device support.
 * Emits events for infinite scrolling, scroll position tracking, and scroll direction.
 * 
 * @example
 * <div appScrollTracker
 *      [scrollThreshold]="90"
 *      [debounceTime]="100"
 *      (scrolledToBottom)="onScrolledToBottom()"
 *      (scrollPosition)="onScrollPositionChange($event)"
 *      (scrollDirection)="onScrollDirectionChange($event)">
 *   <!-- Content -->
 * </div>
 */
@Directive({
  selector: '[appScrollTracker]'
})
export class ScrollTrackerDirective implements OnInit, OnDestroy {
  // Input properties with default values
  @Input() scrollThreshold = 90; // Threshold percentage for bottom detection
  @Input() debounceTime = 100; // Debounce time in milliseconds

  // Event emitters for scroll notifications
  @Output() scrolledToBottom = new EventEmitter<void>();
  @Output() scrollPosition = new EventEmitter<number>();
  @Output() scrollDirection = new EventEmitter<'up' | 'down'>();

  // Subject for cleanup
  private destroy$ = new Subject<void>();
  
  // Track last scroll position for direction detection
  private lastScrollPosition = 0;
  
  // Track scroll handler for cleanup
  private scrollHandler: any;

  constructor(
    private el: ElementRef,
    private ngZone: NgZone
  ) {}

  ngOnInit(): void {
    // Run scroll tracking outside Angular zone for better performance
    this.ngZone.runOutsideAngular(() => {
      // Create optimized scroll event stream
      this.scrollHandler = fromEvent(this.el.nativeElement, 'scroll')
        .pipe(
          debounceTime(this.debounceTime),
          distinctUntilChanged(),
          takeUntil(this.destroy$)
        )
        .subscribe(() => {
          // Use requestAnimationFrame for smooth tracking
          requestAnimationFrame(() => {
            this.handleScroll();
          });
        });
    });
  }

  ngOnDestroy(): void {
    // Cleanup subscriptions and event listeners
    if (this.scrollHandler) {
      this.scrollHandler.unsubscribe();
    }
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Host listener for scroll events with performance optimization
   */
  @HostListener('scroll', ['$event'])
  onScroll(): void {
    // Use requestAnimationFrame for smooth scroll tracking
    requestAnimationFrame(() => {
      this.handleScroll();
    });
  }

  /**
   * Handles scroll events and emits appropriate notifications
   */
  private handleScroll(): void {
    const element = this.el.nativeElement;
    const currentScrollPosition = this.getScrollTop(element);

    // Emit scroll position
    this.ngZone.run(() => {
      this.scrollPosition.emit(currentScrollPosition);
    });

    // Determine and emit scroll direction
    if (currentScrollPosition !== this.lastScrollPosition) {
      const direction = currentScrollPosition > this.lastScrollPosition ? 'down' : 'up';
      this.ngZone.run(() => {
        this.scrollDirection.emit(direction);
      });
    }
    this.lastScrollPosition = currentScrollPosition;

    // Check if scrolled to bottom threshold
    if (this.isScrolledToBottom()) {
      this.ngZone.run(() => {
        this.scrolledToBottom.emit();
      });
    }
  }

  /**
   * Checks if the element is scrolled to the threshold with cross-browser support
   */
  private isScrolledToBottom(): boolean {
    const element = this.el.nativeElement;
    const scrollTop = this.getScrollTop(element);
    const scrollHeight = this.getScrollHeight(element);
    const clientHeight = this.getClientHeight(element);
    
    // Calculate threshold position
    const threshold = (scrollHeight - clientHeight) * (this.scrollThreshold / 100);
    
    return scrollTop >= threshold;
  }

  /**
   * Gets scroll top position with cross-browser support
   */
  private getScrollTop(element: HTMLElement): number {
    return element.scrollTop;
  }

  /**
   * Gets scroll height with cross-browser support
   */
  private getScrollHeight(element: HTMLElement): number {
    return Math.max(
      element.scrollHeight,
      element.clientHeight,
      element.offsetHeight
    );
  }

  /**
   * Gets client height with cross-browser support
   */
  private getClientHeight(element: HTMLElement): number {
    return element.clientHeight;
  }
}