import { ComponentFixture, TestBed, fakeAsync, tick, flush } from '@angular/core/testing';
import { provideMockStore, MockStore } from '@ngrx/store/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { DashboardComponent } from './dashboard.component';
import { WebSocketService, ConnectionStatus, WebSocketEventType } from '../../core/services/websocket.service';
import { VesselCall, VesselCallStatus } from '../../shared/models/vessel-call.model';
import { IBerthAllocation, BerthAllocationStatus } from '../../shared/models/berth-allocation.model';

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;
  let store: MockStore;
  let wsService: jasmine.SpyObj<WebSocketService>;

  const mockVesselCalls: VesselCall[] = [
    {
      id: 1,
      vesselId: 101,
      vesselName: 'Maersk Liner',
      imoNumber: 'IMO123456',
      callSign: 'MLINER',
      status: VesselCallStatus.AT_BERTH,
      eta: new Date('2023-11-15T08:00:00Z'),
      etd: new Date('2023-11-15T16:00:00Z'),
      ata: new Date('2023-11-15T08:30:00Z'),
      atd: null,
      createdAt: new Date(),
      updatedAt: new Date()
    },
    {
      id: 2,
      vesselId: 102,
      vesselName: 'MSC Pearl',
      imoNumber: 'IMO789012',
      callSign: 'MPEARL',
      status: VesselCallStatus.ARRIVED,
      eta: new Date('2023-11-15T10:00:00Z'),
      etd: new Date('2023-11-15T18:00:00Z'),
      ata: null,
      atd: null,
      createdAt: new Date(),
      updatedAt: new Date()
    }
  ];

  const mockBerthAllocations: IBerthAllocation[] = [
    {
      id: 1,
      vesselCallId: 1,
      vesselName: 'Maersk Liner',
      berthId: 12,
      berthName: 'Berth 12',
      startTime: '2023-11-15T08:00:00Z',
      endTime: '2023-11-15T16:00:00Z',
      status: BerthAllocationStatus.OCCUPIED,
      createdAt: '2023-11-15T07:00:00Z',
      updatedAt: '2023-11-15T07:00:00Z'
    }
  ];

  const mockInitialState = {
    vesselCalls: [],
    berthAllocations: [],
    wsConnected: false
  };

  beforeEach(async () => {
    const wsServiceSpy = jasmine.createSpyObj('WebSocketService', [
      'connect',
      'disconnect',
      'subscribe',
      'getConnectionStatus'
    ]);

    wsServiceSpy.connect.and.returnValue(Promise.resolve());
    wsServiceSpy.getConnectionStatus.and.returnValue(of(ConnectionStatus.CONNECTED));
    wsServiceSpy.subscribe.and.returnValue(of({
      type: WebSocketEventType.VESSEL_UPDATE,
      payload: mockVesselCalls[0],
      timestamp: new Date(),
      id: '1',
      status: 'RECEIVED'
    }));

    await TestBed.configureTestingModule({
      declarations: [DashboardComponent],
      imports: [NoopAnimationsModule],
      providers: [
        provideMockStore({ initialState: mockInitialState }),
        { provide: WebSocketService, useValue: wsServiceSpy }
      ]
    }).compileComponents();

    store = TestBed.inject(MockStore);
    wsService = TestBed.inject(WebSocketService) as jasmine.SpyObj<WebSocketService>;
    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    store.resetSelectors();
    fixture.destroy();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
    expect(wsService.connect).toHaveBeenCalled();
  });

  it('should initialize WebSocket connection on component init', fakeAsync(() => {
    component.ngOnInit();
    tick();

    expect(wsService.connect).toHaveBeenCalled();
    expect(wsService.subscribe).toHaveBeenCalledWith(
      '/topic/vessel-updates',
      WebSocketEventType.VESSEL_UPDATE
    );
    expect(wsService.subscribe).toHaveBeenCalledWith(
      '/topic/berth-updates',
      WebSocketEventType.BERTH_CHANGE
    );
  }));

  it('should load and display vessel calls from store', fakeAsync(() => {
    store.setState({ ...mockInitialState, vesselCalls: mockVesselCalls });
    component.ngOnInit();
    tick();
    fixture.detectChanges();

    expect(component.activeCalls.length).toBe(2);
    expect(component.activeCalls[0].vesselName).toBe('Maersk Liner');
    expect(component.activeCalls[1].vesselName).toBe('MSC Pearl');
  }));

  it('should calculate berth utilization correctly', fakeAsync(() => {
    store.setState({
      ...mockInitialState,
      vesselCalls: mockVesselCalls,
      berthAllocations: mockBerthAllocations
    });
    component.ngOnInit();
    tick();
    fixture.detectChanges();

    const utilization = component.berthUtilization$.value;
    expect(utilization).toBe(100); // One berth fully occupied
  }));

  it('should handle WebSocket vessel updates', fakeAsync(() => {
    const updatedVessel = {
      ...mockVesselCalls[0],
      status: VesselCallStatus.DEPARTED
    };

    wsService.subscribe.and.returnValue(of({
      type: WebSocketEventType.VESSEL_UPDATE,
      payload: updatedVessel,
      timestamp: new Date(),
      id: '2',
      status: 'RECEIVED'
    }));

    component.ngOnInit();
    tick();
    fixture.detectChanges();

    expect(wsService.subscribe).toHaveBeenCalledWith(
      '/topic/vessel-updates',
      WebSocketEventType.VESSEL_UPDATE
    );
  }));

  it('should handle WebSocket connection errors', fakeAsync(() => {
    wsService.connect.and.returnValue(Promise.reject('Connection failed'));
    wsService.getConnectionStatus.and.returnValue(of(ConnectionStatus.ERROR));

    component.ngOnInit();
    tick();
    fixture.detectChanges();

    expect(component.connectionStatus$.value).toBe(ConnectionStatus.ERROR);
  }));

  it('should cleanup subscriptions on destroy', fakeAsync(() => {
    component.ngOnInit();
    tick();
    
    component.ngOnDestroy();
    expect(wsService.disconnect).toHaveBeenCalled();
  }));

  it('should calculate expected arrivals correctly', fakeAsync(() => {
    const futureVessel: VesselCall = {
      ...mockVesselCalls[0],
      id: 3,
      status: VesselCallStatus.PLANNED,
      eta: new Date(Date.now() + (2 * 60 * 60 * 1000)) // 2 hours from now
    };

    store.setState({
      ...mockInitialState,
      vesselCalls: [...mockVesselCalls, futureVessel]
    });

    component.ngOnInit();
    tick();
    fixture.detectChanges();

    const metrics = component.metrics$.value;
    expect(metrics.expectedArrivals).toBe(1);
  }));

  it('should calculate average waiting time', fakeAsync(() => {
    const vesselWithWait = {
      ...mockVesselCalls[0],
      eta: new Date('2023-11-15T08:00:00Z'),
      ata: new Date('2023-11-15T09:00:00Z') // 1 hour waiting time
    };

    store.setState({
      ...mockInitialState,
      vesselCalls: [vesselWithWait]
    });

    component.ngOnInit();
    tick();
    fixture.detectChanges();

    const metrics = component.metrics$.value;
    expect(metrics.averageWaitingTime).toBe(3600000); // 1 hour in milliseconds
  }));
});