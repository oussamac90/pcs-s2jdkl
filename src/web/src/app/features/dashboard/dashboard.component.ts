import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { Store, select, createSelector } from '@ngrx/store';
import { Subject, BehaviorSubject, Observable, combineLatest } from 'rxjs';
import { takeUntil, distinctUntilChanged, debounceTime, retry } from 'rxjs/operators';

import { VesselCall, VesselCallStatus } from '../../shared/models/vessel-call.model';
import { IBerthAllocation, BerthAllocationStatus } from '../../shared/models/berth-allocation.model';
import { WebSocketService, WebSocketEventType, ConnectionStatus } from '../../core/services/websocket.service';

interface DashboardMetrics {
  totalActiveCalls: number;
  berthUtilization: number;
  expectedArrivals: number;
  averageWaitingTime: number;
}

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DashboardComponent implements OnInit, OnDestroy {
  // Observable streams
  private destroy$ = new Subject<void>();
  private connectionStatus$ = new BehaviorSubject<ConnectionStatus>(ConnectionStatus.DISCONNECTED);
  public berthUtilization$ = new BehaviorSubject<number>(0);
  public metrics$ = new BehaviorSubject<DashboardMetrics>({
    totalActiveCalls: 0,
    berthUtilization: 0,
    expectedArrivals: 0,
    averageWaitingTime: 0
  });

  // Component state
  public activeCalls: VesselCall[] = [];
  public berthAllocations: IBerthAllocation[] = [];
  public expectedArrivals: VesselCall[] = [];
  public connectionStatus = ConnectionStatus;

  // Memoized selectors
  private readonly selectActiveCalls = createSelector(
    (state: any) => state.vesselCalls,
    (calls: VesselCall[]) => calls.filter(call => 
      call.status !== VesselCallStatus.DEPARTED && 
      call.status !== VesselCallStatus.CANCELLED
    )
  );

  private readonly selectBerthAllocations = createSelector(
    (state: any) => state.berthAllocations,
    (allocations: IBerthAllocation[]) => allocations.filter(allocation =>
      allocation.status === BerthAllocationStatus.SCHEDULED ||
      allocation.status === BerthAllocationStatus.OCCUPIED
    )
  );

  constructor(
    private store: Store<any>,
    private wsService: WebSocketService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.initializeWebSocketConnection();
    this.initializeDataSubscriptions();
    this.initializeMetricsCalculation();
  }

  private async initializeWebSocketConnection(): Promise<void> {
    try {
      await this.wsService.connect();
      this.setupWebSocketSubscriptions();
    } catch (error) {
      console.error('WebSocket connection failed:', error);
      // Retry connection with exponential backoff
      this.retryConnection();
    }
  }

  private setupWebSocketSubscriptions(): void {
    // Subscribe to vessel updates
    this.wsService.subscribe<VesselCall>('/topic/vessel-updates', WebSocketEventType.VESSEL_UPDATE)
      .pipe(
        takeUntil(this.destroy$),
        debounceTime(100) // Buffer rapid updates
      )
      .subscribe(message => {
        this.handleVesselUpdate(message.payload);
        this.cdr.markForCheck();
      });

    // Subscribe to berth changes
    this.wsService.subscribe<IBerthAllocation>('/topic/berth-updates', WebSocketEventType.BERTH_CHANGE)
      .pipe(
        takeUntil(this.destroy$),
        debounceTime(100)
      )
      .subscribe(message => {
        this.handleBerthUpdate(message.payload);
        this.cdr.markForCheck();
      });

    // Monitor connection status
    this.wsService.getConnectionStatus()
      .pipe(takeUntil(this.destroy$))
      .subscribe(status => {
        this.connectionStatus$.next(status);
        this.cdr.markForCheck();
      });
  }

  private initializeDataSubscriptions(): void {
    // Subscribe to store selectors
    this.store.pipe(
      select(this.selectActiveCalls),
      takeUntil(this.destroy$),
      distinctUntilChanged()
    ).subscribe(calls => {
      this.activeCalls = calls;
      this.calculateMetrics();
      this.cdr.markForCheck();
    });

    this.store.pipe(
      select(this.selectBerthAllocations),
      takeUntil(this.destroy$),
      distinctUntilChanged()
    ).subscribe(allocations => {
      this.berthAllocations = allocations;
      this.calculateMetrics();
      this.cdr.markForCheck();
    });
  }

  private initializeMetricsCalculation(): void {
    // Calculate metrics every 5 minutes or on data changes
    combineLatest([
      this.store.pipe(select(this.selectActiveCalls)),
      this.store.pipe(select(this.selectBerthAllocations))
    ]).pipe(
      takeUntil(this.destroy$),
      debounceTime(300000) // 5 minutes
    ).subscribe(() => {
      this.calculateMetrics();
    });
  }

  private calculateMetrics(): void {
    const metrics: DashboardMetrics = {
      totalActiveCalls: this.activeCalls.length,
      berthUtilization: this.calculateBerthUtilization(),
      expectedArrivals: this.calculateExpectedArrivals(),
      averageWaitingTime: this.calculateAverageWaitingTime()
    };

    this.metrics$.next(metrics);
    this.berthUtilization$.next(metrics.berthUtilization);
  }

  private calculateBerthUtilization(): number {
    if (!this.berthAllocations.length) return 0;

    const occupiedBerths = this.berthAllocations.filter(
      allocation => allocation.status === BerthAllocationStatus.OCCUPIED
    ).length;

    return (occupiedBerths / this.berthAllocations.length) * 100;
  }

  private calculateExpectedArrivals(): number {
    return this.activeCalls.filter(
      call => call.status === VesselCallStatus.PLANNED &&
      new Date(call.eta).getTime() <= Date.now() + (24 * 60 * 60 * 1000) // Next 24 hours
    ).length;
  }

  private calculateAverageWaitingTime(): number {
    const vesselWaitingTimes = this.activeCalls
      .filter(call => call.ata && call.eta)
      .map(call => new Date(call.ata!).getTime() - new Date(call.eta).getTime());

    if (!vesselWaitingTimes.length) return 0;
    return vesselWaitingTimes.reduce((acc, time) => acc + time, 0) / vesselWaitingTimes.length;
  }

  private handleVesselUpdate(vesselCall: VesselCall): void {
    const index = this.activeCalls.findIndex(call => call.id === vesselCall.id);
    if (index !== -1) {
      this.activeCalls[index] = vesselCall;
    } else {
      this.activeCalls.push(vesselCall);
    }
    this.calculateMetrics();
  }

  private handleBerthUpdate(allocation: IBerthAllocation): void {
    const index = this.berthAllocations.findIndex(a => a.id === allocation.id);
    if (index !== -1) {
      this.berthAllocations[index] = allocation;
    } else {
      this.berthAllocations.push(allocation);
    }
    this.calculateMetrics();
  }

  private retryConnection(): void {
    this.wsService.getConnectionStatus()
      .pipe(
        retry({
          count: 5,
          delay: (error, retryCount) => {
            const delayMs = Math.min(1000 * Math.pow(2, retryCount), 30000);
            return new Observable(subscriber => {
              setTimeout(() => subscriber.next(), delayMs);
            });
          }
        }),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (status) => {
          if (status === ConnectionStatus.CONNECTED) {
            this.setupWebSocketSubscriptions();
          }
        },
        error: (error) => console.error('WebSocket connection failed after retries:', error)
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.berthUtilization$.complete();
    this.metrics$.complete();
    this.connectionStatus$.complete();
  }
}