import { Component, OnInit, ChangeDetectionStrategy, ViewChild, ElementRef, ChangeDetectorRef, OnDestroy } from '@angular/core';
import { Store, select } from '@ngrx/store';
import { Observable, BehaviorSubject, fromEvent, merge, Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, map, takeUntil, filter } from 'rxjs/operators';

import { IBerthAllocation, BerthAllocationStatus } from '../../../../shared/models/berth-allocation.model';
import { selectAllBerthAllocations } from '../../../../store/selectors/berth-allocation.selectors';
import { AppState } from '../../../../store/state/app.state';

interface TimelinePosition {
  left: number;
  width: number;
  top: number;
  height: number;
  conflicts: boolean;
}

@Component({
  selector: 'app-berth-timeline',
  templateUrl: './berth-timeline.component.html',
  styleUrls: ['./berth-timeline.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BerthTimelineComponent implements OnInit, OnDestroy {
  // Observables for reactive data management
  berthAllocations$: Observable<IBerthAllocation[]>;
  selectedDate$ = new BehaviorSubject<Date>(new Date());
  loading$ = new BehaviorSubject<boolean>(false);
  error$ = new BehaviorSubject<string | null>(null);

  // ViewChild references
  @ViewChild('timelineContainer', { static: true }) timelineContainer!: ElementRef;

  // Timeline configuration
  private readonly timelineStartHour = 0;
  private readonly timelineEndHour = 24;
  private readonly hourWidth = 100; // pixels per hour
  private readonly berthHeight = 60; // pixels per berth row
  private readonly dragThreshold = 5; // pixels for drag detection

  // State management
  private isDragging = false;
  private dragStartX = 0;
  private dragStartY = 0;
  private selectedAllocation: IBerthAllocation | null = null;
  private readonly destroy$ = new Subject<void>();

  // Performance optimization
  private readonly positionCache = new Map<number, TimelinePosition>();
  private readonly debounceTime = 100; // ms

  constructor(
    private store: Store<AppState>,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.initializeTimelineData();
    this.setupEventListeners();
    this.setupWebSocketConnection();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.clearCache();
  }

  private initializeTimelineData(): void {
    this.loading$.next(true);

    this.berthAllocations$ = this.store.pipe(
      select(selectAllBerthAllocations),
      map(allocations => this.filterAllocationsByDate(allocations)),
      debounceTime(this.debounceTime),
      takeUntil(this.destroy$)
    );

    this.berthAllocations$.subscribe({
      next: () => {
        this.loading$.next(false);
        this.clearCache();
        this.cdr.detectChanges();
      },
      error: (error) => {
        this.error$.next(error.message);
        this.loading$.next(false);
      }
    });
  }

  private setupEventListeners(): void {
    if (!this.timelineContainer) return;

    const element = this.timelineContainer.nativeElement;

    // Mouse events for drag and drop
    const mouseDown$ = fromEvent<MouseEvent>(element, 'mousedown');
    const mouseMove$ = fromEvent<MouseEvent>(document, 'mousemove');
    const mouseUp$ = fromEvent<MouseEvent>(document, 'mouseup');

    mouseDown$.pipe(
      filter(event => this.isAllocationElement(event.target as HTMLElement)),
      takeUntil(this.destroy$)
    ).subscribe(event => this.handleDragStart(event));

    mouseMove$.pipe(
      filter(() => this.isDragging),
      takeUntil(this.destroy$)
    ).subscribe(event => this.handleDragMove(event));

    mouseUp$.pipe(
      filter(() => this.isDragging),
      takeUntil(this.destroy$)
    ).subscribe(event => this.handleDragEnd(event));
  }

  private setupWebSocketConnection(): void {
    // WebSocket implementation would go here
    // Using a placeholder for the example
  }

  private filterAllocationsByDate(allocations: IBerthAllocation[]): IBerthAllocation[] {
    const selectedDate = this.selectedDate$.value;
    const startOfDay = new Date(selectedDate.setHours(0, 0, 0, 0));
    const endOfDay = new Date(selectedDate.setHours(23, 59, 59, 999));

    return allocations.filter(allocation => {
      const startTime = new Date(allocation.startTime);
      const endTime = new Date(allocation.endTime);
      return startTime <= endOfDay && endTime >= startOfDay;
    });
  }

  calculateTimelinePosition(allocation: IBerthAllocation): TimelinePosition {
    if (this.positionCache.has(allocation.id)) {
      return this.positionCache.get(allocation.id)!;
    }

    const startTime = new Date(allocation.startTime);
    const endTime = new Date(allocation.endTime);

    const left = this.calculateTimePosition(startTime);
    const width = this.calculateTimePosition(endTime) - left;
    const top = (allocation.berthId - 1) * this.berthHeight;
    const height = this.berthHeight - 2; // 2px gap between rows

    const position: TimelinePosition = {
      left,
      width,
      top,
      height,
      conflicts: this.detectConflicts(allocation)
    };

    this.positionCache.set(allocation.id, position);
    return position;
  }

  private calculateTimePosition(time: Date): number {
    const hours = time.getHours() + (time.getMinutes() / 60);
    return (hours - this.timelineStartHour) * this.hourWidth;
  }

  private detectConflicts(allocation: IBerthAllocation): boolean {
    return false; // Implement conflict detection logic
  }

  private handleDragStart(event: MouseEvent): void {
    if (!(event.target instanceof HTMLElement)) return;

    const allocationId = parseInt(event.target.dataset.allocationId || '', 10);
    if (isNaN(allocationId)) return;

    this.isDragging = true;
    this.dragStartX = event.clientX;
    this.dragStartY = event.clientY;
    this.selectedAllocation = this.findAllocationById(allocationId);
    event.preventDefault();
  }

  private handleDragMove(event: MouseEvent): void {
    if (!this.isDragging || !this.selectedAllocation) return;

    const deltaX = event.clientX - this.dragStartX;
    if (Math.abs(deltaX) < this.dragThreshold) return;

    const position = this.calculateTimelinePosition(this.selectedAllocation);
    const newPosition = {
      ...position,
      left: position.left + deltaX
    };

    this.updateAllocationPosition(this.selectedAllocation, newPosition);
    this.cdr.detectChanges();
  }

  private handleDragEnd(event: MouseEvent): void {
    if (!this.isDragging || !this.selectedAllocation) return;

    const finalPosition = this.calculateTimelinePosition(this.selectedAllocation);
    if (this.isValidPosition(finalPosition)) {
      this.saveAllocationUpdate(this.selectedAllocation, finalPosition);
    }

    this.isDragging = false;
    this.selectedAllocation = null;
    this.clearCache();
    this.cdr.detectChanges();
  }

  private findAllocationById(id: number): IBerthAllocation | null {
    let allocation: IBerthAllocation | null = null;
    this.berthAllocations$.pipe(
      map(allocations => allocations.find(a => a.id === id))
    ).subscribe(result => allocation = result || null);
    return allocation;
  }

  private isAllocationElement(element: HTMLElement): boolean {
    return element.classList.contains('berth-allocation');
  }

  private isValidPosition(position: TimelinePosition): boolean {
    return position.left >= 0 && 
           position.left + position.width <= this.timelineEndHour * this.hourWidth &&
           !position.conflicts;
  }

  private updateAllocationPosition(allocation: IBerthAllocation, position: TimelinePosition): void {
    this.positionCache.set(allocation.id, position);
  }

  private saveAllocationUpdate(allocation: IBerthAllocation, position: TimelinePosition): void {
    // Implement save logic to backend
  }

  private clearCache(): void {
    this.positionCache.clear();
  }
}