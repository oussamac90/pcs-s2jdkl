import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { Store, select } from '@ngrx/store';
import { Observable, BehaviorSubject, Subject, combineLatest } from 'rxjs';
import { map, distinctUntilChanged, debounceTime, catchError, takeUntil, filter } from 'rxjs/operators';

import { BerthTimelineComponent } from './components/berth-timeline/berth-timeline.component';
import { IBerthAllocation, BerthAllocationStatus } from '../../shared/models/berth-allocation.model';
import { BerthWebSocketService } from '@app/services';
import { 
  selectAllBerthAllocations,
  selectBerthAllocationsLoading,
  selectBerthAllocationsError,
  selectBerthAllocationsByTimeRange
} from '../../store/selectors/berth-allocation.selectors';
import { AppState } from '../../store/state/app.state';

/**
 * Enum for tracking conflict status in berth allocations
 */
enum BerthConflictStatus {
  NONE = 'NONE',
  TIME_OVERLAP = 'TIME_OVERLAP',
  RESOURCE_CONFLICT = 'RESOURCE_CONFLICT'
}

/**
 * Main component for berth management functionality
 * Handles real-time updates, conflict detection, and timeline visualization
 */
@Component({
  selector: 'app-berth-management',
  templateUrl: './berth-management.component.html',
  styleUrls: ['./berth-management.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BerthManagementComponent implements OnInit, OnDestroy {
  // Observable streams for reactive data management
  berthAllocations$: Observable<IBerthAllocation[]>;
  selectedDate$ = new BehaviorSubject<Date>(new Date());
  isLoading$: Observable<boolean>;
  error$: Observable<string | null>;
  conflictStatus$ = new BehaviorSubject<BerthConflictStatus>(BerthConflictStatus.NONE);

  // Component state
  private isDragging = false;
  private readonly destroy$ = new Subject<void>();
  private readonly allocationCache = new Map<number, IBerthAllocation>();
  private readonly updateDebounceTime = 300; // ms

  constructor(
    private store: Store<AppState>,
    private wsService: BerthWebSocketService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.initializeDataStreams();
    this.setupWebSocketConnection();
    this.setupConflictDetection();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.allocationCache.clear();
  }

  /**
   * Initializes all data streams with proper error handling and performance optimization
   */
  private initializeDataStreams(): void {
    // Initialize loading and error streams
    this.isLoading$ = this.store.pipe(
      select(selectBerthAllocationsLoading),
      distinctUntilChanged()
    );

    this.error$ = this.store.pipe(
      select(selectBerthAllocationsError),
      distinctUntilChanged()
    );

    // Initialize berth allocations stream with date filtering
    this.berthAllocations$ = combineLatest([
      this.store.pipe(select(selectAllBerthAllocations)),
      this.selectedDate$
    ]).pipe(
      debounceTime(this.updateDebounceTime),
      map(([allocations, selectedDate]) => this.filterAllocationsByDate(allocations, selectedDate)),
      catchError(error => {
        console.error('Error loading berth allocations:', error);
        return [];
      }),
      takeUntil(this.destroy$)
    );
  }

  /**
   * Sets up WebSocket connection for real-time updates
   */
  private setupWebSocketConnection(): void {
    this.wsService.connect()
      .pipe(
        filter(message => message.type === 'BERTH_UPDATE'),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (message) => this.handleWebSocketUpdate(message),
        error: (error) => console.error('WebSocket error:', error)
      });
  }

  /**
   * Configures conflict detection for berth allocations
   */
  private setupConflictDetection(): void {
    this.berthAllocations$
      .pipe(
        debounceTime(this.updateDebounceTime),
        takeUntil(this.destroy$)
      )
      .subscribe(allocations => {
        this.detectConflicts(allocations);
        this.cdr.detectChanges();
      });
  }

  /**
   * Handles date selection changes
   * @param date Selected date for berth planning
   */
  onDateChange(date: Date): void {
    if (!date || isNaN(date.getTime())) {
      console.error('Invalid date selected');
      return;
    }
    this.selectedDate$.next(date);
  }

  /**
   * Processes berth allocation updates
   * @param allocation Updated berth allocation
   */
  onAllocationUpdate(allocation: IBerthAllocation): void {
    if (!this.isValidAllocation(allocation)) {
      console.error('Invalid allocation update:', allocation);
      return;
    }

    this.allocationCache.set(allocation.id, allocation);
    this.detectConflicts([...this.allocationCache.values()]);
    this.cdr.detectChanges();
  }

  /**
   * Filters allocations by selected date
   * @param allocations List of all berth allocations
   * @param date Selected date for filtering
   */
  private filterAllocationsByDate(allocations: IBerthAllocation[], date: Date): IBerthAllocation[] {
    const startOfDay = new Date(date);
    startOfDay.setHours(0, 0, 0, 0);
    const endOfDay = new Date(date);
    endOfDay.setHours(23, 59, 59, 999);

    return allocations.filter(allocation => {
      const startTime = new Date(allocation.startTime);
      const endTime = new Date(allocation.endTime);
      return startTime <= endOfDay && endTime >= startOfDay;
    });
  }

  /**
   * Detects conflicts between berth allocations
   * @param allocations List of allocations to check for conflicts
   */
  private detectConflicts(allocations: IBerthAllocation[]): void {
    const conflicts = allocations.some((a1, index) => {
      return allocations.some((a2, index2) => {
        if (index === index2) return false;
        return this.hasTimeOverlap(a1, a2) && a1.berthId === a2.berthId;
      });
    });

    this.conflictStatus$.next(
      conflicts ? BerthConflictStatus.TIME_OVERLAP : BerthConflictStatus.NONE
    );
  }

  /**
   * Checks for time overlap between two allocations
   * @param a1 First allocation
   * @param a2 Second allocation
   */
  private hasTimeOverlap(a1: IBerthAllocation, a2: IBerthAllocation): boolean {
    const start1 = new Date(a1.startTime);
    const end1 = new Date(a1.endTime);
    const start2 = new Date(a2.startTime);
    const end2 = new Date(a2.endTime);

    return start1 < end2 && end1 > start2;
  }

  /**
   * Validates berth allocation data
   * @param allocation Allocation to validate
   */
  private isValidAllocation(allocation: IBerthAllocation): boolean {
    return !!(
      allocation &&
      allocation.id &&
      allocation.berthId &&
      allocation.startTime &&
      allocation.endTime &&
      new Date(allocation.startTime) < new Date(allocation.endTime)
    );
  }

  /**
   * Handles WebSocket update messages
   * @param message WebSocket message containing allocation updates
   */
  private handleWebSocketUpdate(message: any): void {
    if (message.data && this.isValidAllocation(message.data)) {
      this.onAllocationUpdate(message.data);
    }
  }
}