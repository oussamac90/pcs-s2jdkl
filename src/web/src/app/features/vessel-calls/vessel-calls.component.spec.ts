import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { Store } from '@ngrx/store';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { MatSnackBarModule } from '@angular/material/snack-bar';

import { VesselCallsComponent } from './vessel-calls.component';
import { WebSocketService, WebSocketEventType, ConnectionStatus } from '../../core/services/websocket.service';
import { NotificationService, NotificationType } from '../../core/services/notification.service';
import { VesselCall, VesselCallStatus } from '../../shared/models/vessel-call.model';
import { loadVesselCalls, selectVesselCall, deleteVesselCall } from '../../store/actions/vessel-call.actions';

describe('VesselCallsComponent', () => {
  let component: VesselCallsComponent;
  let fixture: ComponentFixture<VesselCallsComponent>;
  let store: jasmine.SpyObj<Store>;
  let webSocketService: jasmine.SpyObj<WebSocketService>;
  let notificationService: jasmine.SpyObj<NotificationService>;

  // Mock data
  const mockVesselCalls: VesselCall[] = [
    {
      id: 1,
      vesselId: 101,
      vesselName: 'Test Vessel 1',
      imoNumber: 'IMO123456',
      callSign: 'TEST1',
      status: VesselCallStatus.PLANNED,
      eta: new Date('2023-11-15T08:00:00Z'),
      etd: new Date('2023-11-15T16:00:00Z'),
      ata: null,
      atd: null,
      createdAt: new Date(),
      updatedAt: new Date()
    }
  ];

  const mockWebSocketMessage = {
    type: WebSocketEventType.VESSEL_UPDATE,
    payload: mockVesselCalls[0],
    timestamp: new Date(),
    id: 'msg-1',
    status: 'RECEIVED'
  };

  beforeEach(async () => {
    // Create spies for dependencies
    store = jasmine.createSpyObj('Store', ['dispatch', 'pipe']);
    webSocketService = jasmine.createSpyObj('WebSocketService', ['connect', 'disconnect', 'subscribe', 'getConnectionStatus']);
    notificationService = jasmine.createSpyObj('NotificationService', ['showNotification']);

    // Configure spy behavior
    store.pipe.and.returnValue(of(mockVesselCalls));
    webSocketService.connect.and.returnValue(Promise.resolve());
    webSocketService.subscribe.and.returnValue(of(mockWebSocketMessage));
    webSocketService.getConnectionStatus.and.returnValue(new BehaviorSubject(ConnectionStatus.CONNECTED));

    await TestBed.configureTestingModule({
      declarations: [VesselCallsComponent],
      imports: [MatSnackBarModule],
      providers: [
        { provide: Store, useValue: store },
        { provide: WebSocketService, useValue: webSocketService },
        { provide: NotificationService, useValue: notificationService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(VesselCallsComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with default values', () => {
    fixture.detectChanges();
    expect(component.currentPage).toBe(1);
    expect(component.pageSize).toBe(10);
    expect(component.vesselCallStatuses).toEqual(Object.values(VesselCallStatus));
  });

  it('should load initial data on init', fakeAsync(() => {
    fixture.detectChanges();
    tick();

    expect(store.dispatch).toHaveBeenCalledWith(
      loadVesselCalls({
        filters: {
          dateRange: jasmine.any(Object)
        },
        pagination: {
          page: 1,
          pageSize: 10
        }
      })
    );
  }));

  it('should handle websocket updates correctly', fakeAsync(() => {
    fixture.detectChanges();
    tick();

    const wsSubscription = webSocketService.subscribe('/topic/vessel-updates', WebSocketEventType.VESSEL_UPDATE);
    wsSubscription.subscribe(() => {
      expect(notificationService.showNotification).toHaveBeenCalledWith(
        jasmine.stringContaining('Test Vessel 1'),
        NotificationType.INFO
      );
    });
  }));

  it('should meet performance requirements', fakeAsync(() => {
    const startTime = performance.now();
    fixture.detectChanges();
    tick();
    const endTime = performance.now();
    
    // Verify response time is under 3 seconds
    expect(endTime - startTime).toBeLessThan(3000);
  }));

  it('should handle vessel selection', () => {
    component.onVesselSelect(mockVesselCalls[0]);
    expect(store.dispatch).toHaveBeenCalledWith(
      selectVesselCall({ id: mockVesselCalls[0].id })
    );
  });

  it('should handle vessel deletion with confirmation', fakeAsync(() => {
    spyOn(window, 'confirm').and.returnValue(true);
    component.onDeleteVesselCall(1);
    tick();

    expect(store.dispatch).toHaveBeenCalledWith(
      deleteVesselCall({ id: 1 })
    );
  }));

  it('should handle filter changes', fakeAsync(() => {
    const filter = {
      status: VesselCallStatus.PLANNED,
      dateRange: {
        start: new Date(),
        end: new Date()
      }
    };

    component.onFilterChange(filter);
    tick(300); // Account for debounceTime

    expect(store.dispatch).toHaveBeenCalledWith(
      loadVesselCalls({
        filters: filter,
        pagination: {
          page: 1,
          pageSize: 10
        }
      })
    );
  }));

  it('should handle pagination changes', () => {
    component.onPageChange(2);
    expect(component.currentPage).toBe(2);
  });

  it('should handle websocket connection errors', fakeAsync(() => {
    webSocketService.connect.and.returnValue(Promise.reject('Connection failed'));
    fixture.detectChanges();
    tick();

    expect(notificationService.showNotification).toHaveBeenCalledWith(
      jasmine.stringContaining('Failed to establish real-time connection'),
      NotificationType.WARNING,
      jasmine.any(Object)
    );
  }));

  it('should be accessible', () => {
    fixture.detectChanges();
    
    // Verify ARIA attributes
    const container = fixture.debugElement.query(By.css('.vessel-calls-container'));
    expect(container.attributes['role']).toBe('region');
    
    const list = fixture.debugElement.query(By.css('.vessel-calls-list'));
    expect(list.attributes['role']).toBe('list');
  });

  it('should cleanup on destroy', () => {
    fixture.detectChanges();
    fixture.destroy();

    expect(webSocketService.disconnect).toHaveBeenCalled();
  });

  it('should handle error states', fakeAsync(() => {
    store.pipe.and.returnValue(of('API Error'));
    fixture.detectChanges();
    tick();

    expect(notificationService.showNotification).toHaveBeenCalledWith(
      'API Error',
      NotificationType.ERROR,
      { persistent: true }
    );
  }));

  it('should validate date range filters', () => {
    const invalidFilter = {
      dateRange: {
        start: new Date('2023-11-15'),
        end: new Date('2023-11-14') // End before start
      }
    };

    component.onFilterChange(invalidFilter);
    expect(notificationService.showNotification).toHaveBeenCalledWith(
      'Invalid date range selected',
      NotificationType.WARNING
    );
  });
});