import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { MockStore, provideMockStore } from '@ngrx/store/testing';
import { Store } from '@ngrx/store';
import { of, throwError } from 'rxjs';

import { ClearanceComponent } from './clearance.component';
import { ClearanceStatus, ClearanceType } from '../../shared/models/clearance.model';
import * as ClearanceActions from '../../store/actions/clearance.actions';
import * as ClearanceSelectors from '../../store/selectors/clearance.selectors';
import { AppState } from '../../store/state/app.state';

describe('ClearanceComponent', () => {
  let component: ClearanceComponent;
  let fixture: ComponentFixture<ClearanceComponent>;
  let mockStore: MockStore;
  let dispatchSpy: jasmine.Spy;
  let subscriptions: any[] = [];

  const initialState: Partial<AppState> = {
    clearances: {
      entities: {},
      ids: [],
      loading: false,
      error: null,
      selectedId: null,
      lastUpdated: null,
      filterCriteria: {},
      sortCriteria: {
        field: 'submittedAt',
        direction: 'desc'
      }
    }
  };

  const mockClearances = [
    {
      id: 1,
      vesselCallId: 100,
      vesselName: 'Test Vessel',
      type: ClearanceType.CUSTOMS,
      status: ClearanceStatus.PENDING,
      referenceNumber: 'CLR-001',
      submittedBy: 'user1',
      approvedBy: null,
      remarks: null,
      submittedAt: new Date(),
      approvedAt: null,
      validUntil: null,
      createdAt: new Date(),
      updatedAt: new Date()
    }
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ClearanceComponent],
      providers: [
        provideMockStore({ initialState })
      ]
    }).compileComponents();

    mockStore = TestBed.inject(MockStore);
    dispatchSpy = spyOn(mockStore, 'dispatch');
    
    // Set up selector mocks
    mockStore.overrideSelector(ClearanceSelectors.selectAllClearances, mockClearances);
    mockStore.overrideSelector(ClearanceSelectors.selectClearanceLoading, false);
    mockStore.overrideSelector(ClearanceSelectors.selectClearanceError, null);
    mockStore.overrideSelector(ClearanceSelectors.selectPendingClearances, 
      mockClearances.filter(c => c.status === ClearanceStatus.PENDING));
    mockStore.overrideSelector(ClearanceSelectors.selectApprovedClearances,
      mockClearances.filter(c => c.status === ClearanceStatus.APPROVED));

    fixture = TestBed.createComponent(ClearanceComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    subscriptions.forEach(sub => sub.unsubscribe());
    subscriptions = [];
    mockStore.resetSelectors();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize observables on construction', () => {
    expect(component.clearances$).toBeDefined();
    expect(component.pendingClearances$).toBeDefined();
    expect(component.approvedClearances$).toBeDefined();
    expect(component.loading$).toBeDefined();
    expect(component.error$).toBeDefined();
  });

  it('should load clearances on init', () => {
    component.ngOnInit();
    
    expect(dispatchSpy).toHaveBeenCalledWith(
      ClearanceActions.loadClearances({
        page: 0,
        pageSize: 50
      })
    );
  });

  it('should track loading state correctly', fakeAsync(() => {
    mockStore.overrideSelector(ClearanceSelectors.selectClearanceLoading, true);
    mockStore.refreshState();
    
    let loadingValue: boolean | undefined;
    const sub = component.loading$.subscribe(loading => loadingValue = loading);
    subscriptions.push(sub);
    
    tick();
    expect(loadingValue).toBe(true);
    
    mockStore.overrideSelector(ClearanceSelectors.selectClearanceLoading, false);
    mockStore.refreshState();
    
    tick();
    expect(loadingValue).toBe(false);
  }));

  it('should calculate progress percentage correctly', () => {
    const mockClearancesWithMixedStatus = [
      { ...mockClearances[0] },
      { ...mockClearances[0], id: 2, status: ClearanceStatus.APPROVED },
      { ...mockClearances[0], id: 3, status: ClearanceStatus.REJECTED }
    ];

    mockStore.overrideSelector(ClearanceSelectors.selectAllClearances, mockClearancesWithMixedStatus);
    mockStore.refreshState();

    component.ngOnInit();
    fixture.detectChanges();

    expect(component.progressPercentage).toBe(67); // 2 completed out of 3 = ~67%
  });

  it('should handle errors during clearance loading', fakeAsync(() => {
    const errorMessage = 'Failed to load clearances';
    mockStore.overrideSelector(ClearanceSelectors.selectClearanceError, errorMessage);
    mockStore.refreshState();

    let actualError: string | null = null;
    const sub = component.error$.subscribe(error => actualError = error);
    subscriptions.push(sub);

    tick();
    expect(actualError).toBe(errorMessage);
  }));

  it('should update clearance status correctly', () => {
    const clearanceId = 1;
    const newStatus = ClearanceStatus.APPROVED;
    const remarks = 'Approved after document verification';

    component.updateClearanceStatus(clearanceId, newStatus, remarks);

    expect(dispatchSpy).toHaveBeenCalledWith(
      ClearanceActions.updateClearanceStatus({
        id: clearanceId,
        status: newStatus,
        remarks,
        userId: jasmine.any(Number),
        validUntil: jasmine.any(Date)
      })
    );
  });

  it('should submit clearance request correctly', () => {
    const vesselCallId = 100;
    const type = ClearanceType.CUSTOMS;
    const documents = [new File([''], 'test.pdf')];

    component.submitClearanceRequest(vesselCallId, type, documents);

    expect(dispatchSpy).toHaveBeenCalledWith(
      ClearanceActions.submitClearance({
        vesselCallId,
        type,
        submittedBy: jasmine.any(String),
        documents
      })
    );
  });

  it('should filter clearances by type correctly', () => {
    const type = ClearanceType.CUSTOMS;

    component.getClearancesByType(type);

    expect(dispatchSpy).toHaveBeenCalledWith(
      ClearanceActions.loadClearances({
        page: 0,
        pageSize: 50,
        type
      })
    );
  });

  it('should clean up subscriptions on destroy', () => {
    const destroySpy = spyOn(component['destroy$'], 'next');
    const completeSpy = spyOn(component['destroy$'], 'complete');

    component.ngOnDestroy();

    expect(destroySpy).toHaveBeenCalled();
    expect(completeSpy).toHaveBeenCalled();
  });

  it('should handle WebSocket updates correctly', fakeAsync(() => {
    // Simulate WebSocket update through store
    const updatedClearance = {
      ...mockClearances[0],
      status: ClearanceStatus.APPROVED
    };

    mockStore.overrideSelector(ClearanceSelectors.selectAllClearances, [updatedClearance]);
    mockStore.refreshState();

    tick();
    fixture.detectChanges();

    const sub = component.approvedClearances$.subscribe(clearances => {
      expect(clearances).toContain(updatedClearance);
    });
    subscriptions.push(sub);
  }));
});