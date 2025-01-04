import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { Store, select, createSelector } from '@ngrx/store';
import { Observable, Subject, BehaviorSubject, combineLatest } from 'rxjs';
import { takeUntil, distinctUntilChanged, debounceTime, catchError } from 'rxjs/operators';
import { ServiceBooking, ServiceType, ServiceStatus } from '../../shared/models/service-booking.model';
import { NotificationService } from '../../core/services/notification.service';
import { WebSocketService, WebSocketEventType } from '../../core/services/websocket.service';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';

// Selector for service bookings state
const getServiceBookings = createSelector(
  (state: any) => state.serviceBookings,
  (serviceBookings) => serviceBookings.items
);

@Component({
  selector: 'app-service-booking',
  templateUrl: './service-booking.component.html',
  styleUrls: ['./service-booking.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ServiceBookingComponent implements OnInit, OnDestroy {
  // Observables
  serviceBookings$: Observable<ServiceBooking[]>;
  loading$: Observable<boolean>;
  error$: Observable<string>;
  
  // Subjects for cleanup and state management
  private destroy$ = new Subject<void>();
  private selectedBooking$ = new BehaviorSubject<ServiceBooking | null>(null);
  
  // Component state
  bookingForm: FormGroup;
  showForm = false;
  currentPage = 0;
  pageSize = 10;
  sortField = 'serviceTime';
  sortDirection = 'asc';
  
  // Enums for template
  ServiceType = ServiceType;
  ServiceStatus = ServiceStatus;

  constructor(
    private store: Store<any>,
    private notificationService: NotificationService,
    private webSocketService: WebSocketService,
    private cdr: ChangeDetectorRef,
    private translateService: TranslateService,
    private fb: FormBuilder
  ) {
    this.initializeForm();
    this.initializeSelectors();
  }

  private initializeForm(): void {
    this.bookingForm = this.fb.group({
      serviceType: ['', [Validators.required]],
      quantity: [1, [Validators.required, Validators.min(1)]],
      serviceTime: ['', [Validators.required]],
      vesselCallId: ['', [Validators.required]],
      remarks: [''],
      status: [ServiceStatus.REQUESTED]
    });
  }

  private initializeSelectors(): void {
    this.serviceBookings$ = this.store.pipe(
      select(getServiceBookings),
      distinctUntilChanged()
    );

    this.loading$ = this.store.pipe(
      select((state: any) => state.serviceBookings.loading)
    );

    this.error$ = this.store.pipe(
      select((state: any) => state.serviceBookings.error)
    );
  }

  ngOnInit(): void {
    this.setupWebSocketConnection();
    this.loadServiceBookings();
    this.setupErrorHandling();
  }

  private setupWebSocketConnection(): void {
    this.webSocketService.connect().then(() => {
      this.webSocketService
        .subscribe<ServiceBooking>('/topic/service-updates', WebSocketEventType.SERVICE_STATUS)
        .pipe(takeUntil(this.destroy$))
        .subscribe(message => {
          this.handleServiceUpdate(message.payload);
          this.cdr.markForCheck();
        });
    }).catch(error => {
      this.notificationService.showNotification(
        this.translateService.instant('SERVICE_BOOKING.WS_CONNECTION_ERROR'),
        'error'
      );
    });
  }

  private handleServiceUpdate(updatedBooking: ServiceBooking): void {
    this.store.dispatch({
      type: '[Service Booking] Update Status',
      payload: updatedBooking
    });

    const message = this.translateService.instant(
      `SERVICE_BOOKING.STATUS_UPDATED.${updatedBooking.status}`,
      { id: updatedBooking.id }
    );
    this.notificationService.showNotification(message, 'info');
  }

  private loadServiceBookings(): void {
    this.store.dispatch({
      type: '[Service Booking] Load Services',
      payload: {
        page: this.currentPage,
        pageSize: this.pageSize,
        sortField: this.sortField,
        sortDirection: this.sortDirection
      }
    });
  }

  private setupErrorHandling(): void {
    this.error$.pipe(
      takeUntil(this.destroy$),
      distinctUntilChanged()
    ).subscribe(error => {
      if (error) {
        this.notificationService.showNotification(error, 'error');
      }
    });
  }

  onCreateBooking(): void {
    if (this.bookingForm.valid) {
      const booking = {
        ...this.bookingForm.value,
        serviceTime: new Date(this.bookingForm.value.serviceTime).toISOString()
      };

      this.store.dispatch({
        type: '[Service Booking] Create Booking',
        payload: booking
      });

      this.showForm = false;
      this.bookingForm.reset();
    }
  }

  onEditBooking(booking: ServiceBooking): void {
    this.selectedBooking$.next(booking);
    this.bookingForm.patchValue({
      serviceType: booking.serviceType,
      quantity: booking.quantity,
      serviceTime: booking.serviceTime,
      vesselCallId: booking.vesselCallId,
      remarks: booking.remarks,
      status: booking.status
    });
    this.showForm = true;
  }

  onDeleteBooking(id: number): void {
    if (confirm(this.translateService.instant('SERVICE_BOOKING.CONFIRM_DELETE'))) {
      this.store.dispatch({
        type: '[Service Booking] Delete Booking',
        payload: id
      });
    }
  }

  onPageChange(event: any): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadServiceBookings();
  }

  onSortChange(event: any): void {
    this.sortField = event.active;
    this.sortDirection = event.direction;
    this.loadServiceBookings();
  }

  canEditBooking(booking: ServiceBooking): boolean {
    return booking.status === ServiceStatus.REQUESTED || 
           booking.status === ServiceStatus.CONFIRMED;
  }

  canDeleteBooking(booking: ServiceBooking): boolean {
    return booking.status === ServiceStatus.REQUESTED;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.selectedBooking$.complete();
  }
}