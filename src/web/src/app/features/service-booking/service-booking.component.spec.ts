import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { MockStore, provideMockStore } from '@ngrx/store/testing';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { axe, toHaveNoViolations } from 'jest-axe';

import { ServiceBookingComponent } from './service-booking.component';
import { WebSocketService, WebSocketEventType } from '../../core/services/websocket.service';
import { NotificationService, NotificationType } from '../../core/services/notification.service';
import { ServiceBooking, ServiceType, ServiceStatus } from '../../shared/models/service-booking.model';

describe('ServiceBookingComponent', () => {
  let component: ServiceBookingComponent;
  let fixture: ComponentFixture<ServiceBookingComponent>;
  let store: MockStore;
  let webSocketService: jasmine.SpyObj<WebSocketService>;
  let notificationService: jasmine.SpyObj<NotificationService>;
  let translateService: jasmine.SpyObj<TranslateService>;
  let dialog: jasmine.SpyObj<MatDialog>;

  const initialState = {
    serviceBookings: {
      items: [],
      loading: false,
      error: null
    }
  };

  const mockServiceBookings: ServiceBooking[] = [
    {
      id: 1,
      vesselCallId: 100,
      vesselName: 'Test Vessel',
      serviceType: ServiceType.PILOTAGE,
      status: ServiceStatus.REQUESTED,
      quantity: 1,
      serviceTime: '2023-11-15T08:00:00Z',
      remarks: 'Test booking',
      createdAt: '2023-11-14T10:00:00Z',
      updatedAt: '2023-11-14T10:00:00Z'
    }
  ];

  beforeEach(async () => {
    webSocketService = jasmine.createSpyObj('WebSocketService', ['connect', 'subscribe']);
    notificationService = jasmine.createSpyObj('NotificationService', ['showNotification']);
    translateService = jasmine.createSpyObj('TranslateService', ['instant']);
    dialog = jasmine.createSpyObj('MatDialog', ['open']);

    webSocketService.connect.and.returnValue(Promise.resolve());
    webSocketService.subscribe.and.returnValue(of());
    translateService.instant.and.returnValue('translated text');

    await TestBed.configureTestingModule({
      declarations: [ServiceBookingComponent],
      imports: [
        ReactiveFormsModule,
        NoopAnimationsModule
      ],
      providers: [
        FormBuilder,
        provideMockStore({ initialState }),
        { provide: WebSocketService, useValue: webSocketService },
        { provide: NotificationService, useValue: notificationService },
        { provide: TranslateService, useValue: translateService },
        { provide: MatDialog, useValue: dialog }
      ]
    }).compileComponents();

    store = TestBed.inject(MockStore);
    fixture = TestBed.createComponent(ServiceBookingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    store?.resetSelectors();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with empty service bookings', () => {
    const serviceBookings$ = new BehaviorSubject<ServiceBooking[]>([]);
    store.overrideSelector('getServiceBookings', []);
    
    component.serviceBookings$.subscribe(bookings => {
      expect(bookings).toEqual([]);
    });
  });

  it('should load service bookings on init', fakeAsync(() => {
    store.overrideSelector('getServiceBookings', mockServiceBookings);
    const dispatchSpy = spyOn(store, 'dispatch');

    component.ngOnInit();
    tick();

    expect(dispatchSpy).toHaveBeenCalledWith(
      jasmine.objectContaining({
        type: '[Service Booking] Load Services'
      })
    );
  }));

  it('should handle WebSocket service updates', fakeAsync(() => {
    const mockUpdate = {
      type: WebSocketEventType.SERVICE_STATUS,
      payload: {
        id: 1,
        status: ServiceStatus.CONFIRMED
      }
    };

    webSocketService.subscribe.and.returnValue(of(mockUpdate));
    const dispatchSpy = spyOn(store, 'dispatch');

    component.ngOnInit();
    tick();

    expect(dispatchSpy).toHaveBeenCalledWith(
      jasmine.objectContaining({
        type: '[Service Booking] Update Status',
        payload: mockUpdate.payload
      })
    );
  }));

  it('should create new service booking', () => {
    const mockBooking = {
      serviceType: ServiceType.PILOTAGE,
      quantity: 1,
      serviceTime: '2023-11-15T08:00:00Z',
      vesselCallId: 100,
      remarks: 'Test booking'
    };

    component.bookingForm.patchValue(mockBooking);
    const dispatchSpy = spyOn(store, 'dispatch');

    component.onCreateBooking();

    expect(dispatchSpy).toHaveBeenCalledWith(
      jasmine.objectContaining({
        type: '[Service Booking] Create Booking',
        payload: jasmine.objectContaining(mockBooking)
      })
    );
  });

  it('should handle booking deletion', fakeAsync(() => {
    translateService.instant.and.returnValue('Confirm delete?');
    spyOn(window, 'confirm').and.returnValue(true);
    const dispatchSpy = spyOn(store, 'dispatch');

    component.onDeleteBooking(1);
    tick();

    expect(dispatchSpy).toHaveBeenCalledWith(
      jasmine.objectContaining({
        type: '[Service Booking] Delete Booking',
        payload: 1
      })
    );
  }));

  it('should validate form before submission', () => {
    const dispatchSpy = spyOn(store, 'dispatch');
    component.onCreateBooking();
    expect(dispatchSpy).not.toHaveBeenCalled();
    expect(component.bookingForm.valid).toBeFalse();
  });

  it('should handle pagination changes', () => {
    const mockPageEvent = { pageIndex: 1, pageSize: 10 };
    const dispatchSpy = spyOn(store, 'dispatch');

    component.onPageChange(mockPageEvent);

    expect(component.currentPage).toBe(1);
    expect(component.pageSize).toBe(10);
    expect(dispatchSpy).toHaveBeenCalled();
  });

  it('should handle sort changes', () => {
    const mockSortEvent = { active: 'serviceTime', direction: 'desc' };
    const dispatchSpy = spyOn(store, 'dispatch');

    component.onSortChange(mockSortEvent);

    expect(component.sortField).toBe('serviceTime');
    expect(component.sortDirection).toBe('desc');
    expect(dispatchSpy).toHaveBeenCalled();
  });

  it('should properly clean up on destroy', () => {
    const webSocketSpy = spyOn<any>(component['webSocketService'], 'unsubscribe');
    
    component.ngOnDestroy();

    expect(webSocketSpy).toHaveBeenCalled();
  });

  it('should handle WebSocket connection errors', fakeAsync(() => {
    webSocketService.connect.and.returnValue(Promise.reject('Connection failed'));

    component.ngOnInit();
    tick();

    expect(notificationService.showNotification).toHaveBeenCalledWith(
      jasmine.any(String),
      NotificationType.ERROR
    );
  }));

  it('should meet accessibility standards', async () => {
    expect.extend(toHaveNoViolations);
    const results = await axe(fixture.nativeElement);
    expect(results).toHaveNoViolations();
  });

  it('should enforce business rules for service booking', () => {
    const booking = mockServiceBookings[0];
    
    expect(component.canEditBooking(booking)).toBeTrue();
    
    const completedBooking = { ...booking, status: ServiceStatus.COMPLETED };
    expect(component.canEditBooking(completedBooking)).toBeFalse();
  });

  it('should handle form state properly', () => {
    component.onEditBooking(mockServiceBookings[0]);
    
    expect(component.showForm).toBeTrue();
    expect(component.bookingForm.get('serviceType').value).toBe(ServiceType.PILOTAGE);
    expect(component.bookingForm.get('quantity').value).toBe(1);
  });
});