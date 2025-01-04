import { ComponentFixture, TestBed, fakeAsync, tick, flush } from '@angular/core/testing';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { MockStore, provideMockStore } from '@ngrx/store/testing';
import { By } from '@angular/platform-browser';
import { NO_ERRORS_SCHEMA } from '@angular/core';

import { BerthManagementComponent } from './berth-management.component';
import { BerthTimelineComponent } from './components/berth-timeline/berth-timeline.component';
import { WebSocketService, WebSocketEventType, ConnectionStatus } from '../../core/services/websocket.service';
import { IBerthAllocation, BerthAllocationStatus } from '../../shared/models/berth-allocation.model';
import { selectAllBerthAllocations, selectBerthAllocationsLoading, selectBerthAllocationsError } from '../../store/selectors/berth-allocation.selectors';

describe('BerthManagementComponent', () => {
  let component: BerthManagementComponent;
  let fixture: ComponentFixture<BerthManagementComponent>;
  let store: MockStore;
  let wsService: jasmine.SpyObj<WebSocketService>;

  // Mock data
  const mockBerthAllocations: IBerthAllocation[] = [
    {
      id: 1,
      vesselCallId: 101,
      vesselName: 'Test Vessel 1',
      berthId: 1,
      berthName: 'Berth A',
      startTime: new Date(2023, 10, 15, 8, 0).toISOString(),
      endTime: new Date(2023, 10, 15, 16, 0).toISOString(),
      status: BerthAllocationStatus.SCHEDULED,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    },
    {
      id: 2,
      vesselCallId: 102,
      vesselName: 'Test Vessel 2',
      berthId: 2,
      berthName: 'Berth B',
      startTime: new Date(2023, 10, 15, 10, 0).toISOString(),
      endTime: new Date(2023, 10, 15, 18, 0).toISOString(),
      status: BerthAllocationStatus.OCCUPIED,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    }
  ];

  const initialState = {
    berthAllocations: {
      entities: {},
      ids: [],
      loading: false,
      error: null,
      selectedId: null,
      lastUpdated: null,
      filterCriteria: {},
      sortCriteria: { field: 'startTime', direction: 'asc' }
    }
  };

  beforeEach(async () => {
    const wsServiceSpy = jasmine.createSpyObj('WebSocketService', ['connect', 'subscribe', 'getConnectionStatus']);
    wsServiceSpy.connect.and.returnValue(Promise.resolve());
    wsServiceSpy.getConnectionStatus.and.returnValue(of(ConnectionStatus.CONNECTED));
    wsServiceSpy.subscribe.and.returnValue(of({
      type: WebSocketEventType.BERTH_CHANGE,
      payload: mockBerthAllocations[0],
      timestamp: new Date(),
      id: '1',
      status: 'RECEIVED'
    }));

    await TestBed.configureTestingModule({
      declarations: [
        BerthManagementComponent,
        BerthTimelineComponent
      ],
      providers: [
        provideMockStore({ initialState }),
        { provide: WebSocketService, useValue: wsServiceSpy }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    store = TestBed.inject(MockStore);
    wsService = TestBed.inject(WebSocketService) as jasmine.SpyObj<WebSocketService>;
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(BerthManagementComponent);
    component = fixture.componentInstance;
    store.overrideSelector(selectAllBerthAllocations, mockBerthAllocations);
    store.overrideSelector(selectBerthAllocationsLoading, false);
    store.overrideSelector(selectBerthAllocationsError, null);
    fixture.detectChanges();
  });

  afterEach(() => {
    store.resetSelectors();
  });

  describe('Component Initialization', () => {
    it('should create the component', () => {
      expect(component).toBeTruthy();
    });

    it('should initialize with default date selection', () => {
      const today = new Date();
      expect(component.selectedDate$.value.getDate()).toBe(today.getDate());
    });

    it('should establish WebSocket connection on init', fakeAsync(() => {
      component.ngOnInit();
      tick();
      expect(wsService.connect).toHaveBeenCalled();
      expect(wsService.subscribe).toHaveBeenCalledWith('/topic/berth-updates', WebSocketEventType.BERTH_CHANGE);
    }));
  });

  describe('Berth Allocation Loading', () => {
    it('should show loading indicator when loading allocations', () => {
      store.overrideSelector(selectBerthAllocationsLoading, true);
      store.refreshState();
      fixture.detectChanges();

      const loadingElement = fixture.debugElement.query(By.css('.loading-indicator'));
      expect(loadingElement).toBeTruthy();
    });

    it('should display error message when loading fails', () => {
      const errorMessage = 'Failed to load berth allocations';
      store.overrideSelector(selectBerthAllocationsError, errorMessage);
      store.refreshState();
      fixture.detectChanges();

      const errorElement = fixture.debugElement.query(By.css('.error-message'));
      expect(errorElement.nativeElement.textContent).toContain(errorMessage);
    });
  });

  describe('Date Filtering', () => {
    it('should filter allocations by selected date', fakeAsync(() => {
      const testDate = new Date(2023, 10, 15);
      component.onDateChange(testDate);
      tick(300); // Account for debounceTime
      fixture.detectChanges();

      const filteredAllocations = component.berthAllocations$.value;
      expect(filteredAllocations.length).toBe(2);
      expect(new Date(filteredAllocations[0].startTime).getDate()).toBe(testDate.getDate());
    }));

    it('should handle invalid date selection', () => {
      const invalidDate = new Date('invalid');
      spyOn(console, 'error');
      component.onDateChange(invalidDate);
      expect(console.error).toHaveBeenCalledWith('Invalid date selected');
    });
  });

  describe('Real-time Updates', () => {
    it('should handle WebSocket updates correctly', fakeAsync(() => {
      const updatedAllocation: IBerthAllocation = {
        ...mockBerthAllocations[0],
        status: BerthAllocationStatus.OCCUPIED
      };

      wsService.subscribe.and.returnValue(of({
        type: WebSocketEventType.BERTH_CHANGE,
        payload: updatedAllocation,
        timestamp: new Date(),
        id: '1',
        status: 'RECEIVED'
      }));

      component.ngOnInit();
      tick();
      fixture.detectChanges();

      expect(component['allocationCache'].get(updatedAllocation.id)).toBeTruthy();
      expect(component['allocationCache'].get(updatedAllocation.id)?.status)
        .toBe(BerthAllocationStatus.OCCUPIED);
    }));

    it('should handle WebSocket connection errors', fakeAsync(() => {
      wsService.connect.and.returnValue(Promise.reject('Connection failed'));
      spyOn(console, 'error');
      
      component.ngOnInit();
      tick();
      
      expect(console.error).toHaveBeenCalled();
    }));
  });

  describe('Conflict Detection', () => {
    it('should detect time overlaps between allocations', fakeAsync(() => {
      const conflictingAllocation: IBerthAllocation = {
        ...mockBerthAllocations[0],
        id: 3,
        startTime: new Date(2023, 10, 15, 9, 0).toISOString(),
        endTime: new Date(2023, 10, 15, 17, 0).toISOString(),
        berthId: 1 // Same berth as first allocation
      };

      store.overrideSelector(selectAllBerthAllocations, [...mockBerthAllocations, conflictingAllocation]);
      store.refreshState();
      tick(300); // Account for debounceTime
      fixture.detectChanges();

      expect(component.conflictStatus$.value).toBe('TIME_OVERLAP');
    }));
  });

  describe('Performance', () => {
    it('should maintain responsive updates with large datasets', fakeAsync(() => {
      const largeDataset: IBerthAllocation[] = Array.from({ length: 100 }, (_, i) => ({
        ...mockBerthAllocations[0],
        id: i + 1,
        startTime: new Date(2023, 10, 15, i % 24, 0).toISOString(),
        endTime: new Date(2023, 10, 15, (i % 24) + 2, 0).toISOString()
      }));

      const startTime = performance.now();
      store.overrideSelector(selectAllBerthAllocations, largeDataset);
      store.refreshState();
      tick(300);
      fixture.detectChanges();
      const endTime = performance.now();

      expect(endTime - startTime).toBeLessThan(100); // Should process within 100ms
    }));
  });

  describe('Component Cleanup', () => {
    it('should cleanup resources on destroy', () => {
      const clearSpy = spyOn(component['allocationCache'], 'clear');
      component.ngOnDestroy();
      expect(clearSpy).toHaveBeenCalled();
    });
  });
});