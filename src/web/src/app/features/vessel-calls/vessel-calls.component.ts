import { Component, OnInit, OnDestroy, ChangeDetectionStrategy } from '@angular/core';
import { Store, select } from '@ngrx/store';
import { Observable, Subject, BehaviorSubject, combineLatest } from 'rxjs';
import { takeUntil, filter, debounceTime, distinctUntilChanged, catchError, retry } from 'rxjs/operators';

import { VesselCall, VesselCallStatus, VesselCallFilter } from '../../shared/models/vessel-call.model';
import { loadVesselCalls, deleteVesselCall, selectVesselCall, updateVesselCallFilter } from '../../store/actions/vessel-call.actions';
import { selectAllVesselCalls, selectVesselCallsLoading, selectVesselCallError, selectVesselCallFilter } from '../../store/selectors/vessel-call.selectors';
import { NotificationService } from '../../core/services/notification.service';
import { WebSocketService, WebSocketEventType } from '../../core/services/websocket.service';

@Component({
  selector: 'app-vessel-calls',
  templateUrl: './vessel-calls.component.html',
  styleUrls: ['./vessel-calls.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class VesselCallsComponent implements OnInit, OnDestroy {
  // Observables for reactive data management
  vesselCalls$: Observable<VesselCall[]>;
  loading$: Observable<boolean>;
  error$: Observable<string | null>;
  totalItems$: Observable<number>;

  // Subjects for component lifecycle and filter management
  private destroy$ = new Subject<void>();
  private filterSubject$ = new BehaviorSubject<VesselCallFilter>({});

  // Component state
  vesselCallStatuses = Object.values(VesselCallStatus);
  currentPage = 1;
  pageSize = 10;
  defaultDateRange = {
    start: new Date(new Date().setDate(new Date().getDate() - 7)),
    end: new Date(new Date().setDate(new Date().getDate() + 7))
  };

  constructor(
    private store: Store,
    private notificationService: NotificationService,
    private webSocketService: WebSocketService
  ) {
    this.initializeSelectors();
  }

  private initializeSelectors(): void {
    // Initialize store selectors with memoization
    this.vesselCalls$ = this.store.pipe(select(selectAllVesselCalls));
    this.loading$ = this.store.pipe(select(selectVesselCallsLoading));
    this.error$ = this.store.pipe(select(selectVesselCallError));
    
    // Calculate total items for pagination
    this.totalItems$ = this.vesselCalls$.pipe(
      map(calls => calls.length)
    );
  }

  ngOnInit(): void {
    this.initializeWebSocket();
    this.setupFilterSubscription();
    this.loadInitialData();
    this.setupErrorHandling();
    this.setupAccessibility();
  }

  private initializeWebSocket(): void {
    // Connect to WebSocket for real-time updates
    this.webSocketService.connect()
      .then(() => {
        this.subscribeToVesselUpdates();
      })
      .catch(error => {
        this.notificationService.showNotification(
          'Failed to establish real-time connection. Retrying...',
          'WARNING',
          { persistent: true }
        );
      });
  }

  private subscribeToVesselUpdates(): void {
    this.webSocketService
      .subscribe<VesselCall>('/topic/vessel-updates', WebSocketEventType.VESSEL_UPDATE)
      .pipe(
        takeUntil(this.destroy$),
        retry(3)
      )
      .subscribe({
        next: (message) => {
          this.handleVesselUpdate(message.payload);
        },
        error: (error) => {
          this.notificationService.showNotification(
            'Real-time update connection lost. Retrying...',
            'ERROR'
          );
        }
      });
  }

  private handleVesselUpdate(vesselCall: VesselCall): void {
    // Optimistic update handling
    this.store.dispatch(updateVesselCallFilter({
      id: vesselCall.id,
      changes: vesselCall,
      optimistic: true
    }));
  }

  private setupFilterSubscription(): void {
    this.filterSubject$
      .pipe(
        takeUntil(this.destroy$),
        debounceTime(300),
        distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr))
      )
      .subscribe(filter => {
        this.store.dispatch(loadVesselCalls({
          filters: filter,
          pagination: {
            page: this.currentPage,
            pageSize: this.pageSize
          }
        }));
      });
  }

  private loadInitialData(): void {
    // Initialize with default filters
    this.onFilterChange({
      status: undefined,
      dateRange: this.defaultDateRange
    });
  }

  private setupErrorHandling(): void {
    this.error$
      .pipe(
        takeUntil(this.destroy$),
        filter(error => !!error)
      )
      .subscribe(error => {
        this.notificationService.showNotification(
          error,
          'ERROR',
          { persistent: true }
        );
      });
  }

  private setupAccessibility(): void {
    // Set up ARIA live regions and keyboard navigation
    document.querySelector('.vessel-calls-container')?.setAttribute('role', 'region');
    document.querySelector('.vessel-calls-list')?.setAttribute('role', 'list');
  }

  onFilterChange(filter: VesselCallFilter): void {
    // Validate filter criteria
    if (filter.dateRange) {
      const { start, end } = filter.dateRange;
      if (start > end) {
        this.notificationService.showNotification(
          'Invalid date range selected',
          'WARNING'
        );
        return;
      }
    }

    this.filterSubject$.next(filter);
    this.currentPage = 1; // Reset pagination on filter change
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.onFilterChange(this.filterSubject$.value);
  }

  onVesselSelect(vesselCall: VesselCall): void {
    this.store.dispatch(selectVesselCall({ id: vesselCall.id }));
  }

  onDeleteVesselCall(id: number): void {
    if (confirm('Are you sure you want to delete this vessel call?')) {
      this.store.dispatch(deleteVesselCall({ id }));
    }
  }

  ngOnDestroy(): void {
    // Clean up subscriptions and connections
    this.destroy$.next();
    this.destroy$.complete();
    this.webSocketService.disconnect();
  }
}