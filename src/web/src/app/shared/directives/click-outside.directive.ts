import {
  Directive,
  ElementRef,
  Output,
  EventEmitter,
  HostListener,
  OnDestroy,
  Optional,
  Inject
} from '@angular/core'; // @angular/core v16.x

/**
 * Configuration interface for ClickOutsideDirective
 */
interface ClickOutsideConfig {
  enabled?: boolean;
  excludeSelectors?: string[];
  emitDelay?: number;
}

/**
 * Enhanced directive that detects clicks and touch events outside of an element.
 * Supports mobile devices, Shadow DOM traversal, and configurable options.
 * 
 * @example
 * <div [clickOutside]="onClickOutside()" [enabled]="true" [excludeSelectors]="['.ignore-click']">
 *   Content
 * </div>
 */
@Directive({
  selector: '[clickOutside]'
})
export class ClickOutsideDirective implements OnDestroy {
  /**
   * Event emitted when a click occurs outside the element
   */
  @Output() clickOutside = new EventEmitter<void>();

  /**
   * Controls whether the directive is active
   */
  private _enabled = true;
  get enabled(): boolean {
    return this._enabled;
  }
  set enabled(value: boolean) {
    this._enabled = value;
  }

  /**
   * Selectors to exclude from click outside detection
   */
  private _excludeSelectors: string[] = [];
  get excludeSelectors(): string[] {
    return this._excludeSelectors;
  }
  set excludeSelectors(value: string[]) {
    this._excludeSelectors = value || [];
  }

  /**
   * Delay in milliseconds before emitting the clickOutside event
   */
  private _emitDelay = 0;
  get emitDelay(): number {
    return this._emitDelay;
  }
  set emitDelay(value: number) {
    this._emitDelay = value || 0;
  }

  /**
   * Timer reference for delayed emission
   */
  private delayTimeout?: number;

  /**
   * Creates an instance of ClickOutsideDirective.
   * @param elementRef Reference to the host element
   * @param config Optional configuration object
   */
  constructor(
    private elementRef: ElementRef,
    @Optional() @Inject('CLICK_OUTSIDE_CONFIG') private config?: ClickOutsideConfig
  ) {
    if (config) {
      this.enabled = config.enabled ?? true;
      this.excludeSelectors = config.excludeSelectors ?? [];
      this.emitDelay = config.emitDelay ?? 0;
    }
  }

  /**
   * Handles document click and touch events
   * @param event The mouse or touch event
   */
  @HostListener('document:click', ['$event'])
  @HostListener('document:touchstart', ['$event'])
  onClick(event: Event): void {
    if (!this.enabled) {
      return;
    }

    const target = event.target as HTMLElement | null;
    if (!target) {
      return;
    }

    // Handle Shadow DOM traversal
    const path = event.composedPath?.() || this.getEventPath(event);
    if (path.includes(this.elementRef.nativeElement)) {
      return;
    }

    // Check exclude selectors
    if (this.isExcluded(target)) {
      return;
    }

    // Clear any existing timeout
    if (this.delayTimeout) {
      window.clearTimeout(this.delayTimeout);
    }

    // Emit with configured delay
    if (this.emitDelay > 0) {
      this.delayTimeout = window.setTimeout(() => {
        this.clickOutside.emit();
      }, this.emitDelay);
    } else {
      this.clickOutside.emit();
    }
  }

  /**
   * Checks if an element matches any of the exclude selectors
   * @param element Element to check
   * @returns True if element should be excluded
   */
  private isExcluded(element: HTMLElement): boolean {
    return this.excludeSelectors.some(selector => {
      try {
        return element.matches(selector) || 
               element.closest(selector) !== null;
      } catch {
        console.warn(`Invalid selector in clickOutside directive: ${selector}`);
        return false;
      }
    });
  }

  /**
   * Gets the event path for browsers that don't support composedPath
   * @param event The event to get the path for
   * @returns Array of elements in the event path
   */
  private getEventPath(event: Event): EventTarget[] {
    const path: EventTarget[] = [];
    let currentTarget: HTMLElement | null = event.target as HTMLElement;
    
    while (currentTarget) {
      path.push(currentTarget);
      currentTarget = currentTarget.parentElement;
    }

    if (document) {
      path.push(document);
    }
    if (window) {
      path.push(window);
    }

    return path;
  }

  /**
   * Cleanup when directive is destroyed
   */
  ngOnDestroy(): void {
    if (this.delayTimeout) {
      window.clearTimeout(this.delayTimeout);
    }
    this.clickOutside.complete();
  }
}