import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { catchError, map, mergeMap, tap, debounceTime, retry, takeUntil } from 'rxjs/operators';
import { of, Subject } from 'rxjs';

import { ServiceBooking } from '../../shared/models/service-booking.model';
import * as ServiceBookingActions from '../actions/service-booking.actions';
import { NotificationService } from '../../core/services/notification.service';
import { ErrorService } from '../../core/services/error.service';
import { WebSocketService } from '../../core/services/websocket.service';

@Injectable()
export class ServiceBookingEffects {
  private destroy$ = new Subject<void>();
  private readonly RETRY_ATTEMPTS = 3;
  private readonly DEBOUNCE_TIME = 300;

  constructor(
    private actions$: Actions,
    private notificationService: NotificationService,
    private errorService: ErrorService,
    private webSocketService: WebSocketService
  ) {}

  loadServiceBookings$ = createEffect(() => 
    this.actions$.pipe(
      ofType(ServiceBookingActions.loadServiceBookings),
      debounceTime(this.DEBOUNCE_TIME),
      mergeMap(action => 
        this.webSocketService.subscribe<ServiceBooking[]>('/topic/service-bookings', 'SERVICE_STATUS').pipe(
          retry(this.RETRY_ATTEMPTS),
          map(response => ServiceBookingActions.loadServiceBookingsSuccess({
            bookings: response.payload,
            totalItems: response.payload.length,
            totalPages: 1,
            timestamp: new Date().toISOString()
          })),
          catchError(error => of(ServiceBookingActions.loadServiceBookingsFailure({ error })))
        )
      )
    )
  );

  createServiceBooking$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ServiceBookingActions.createServiceBooking),
      mergeMap(action =>
        this.webSocketService.subscribe<ServiceBooking>('/topic/service-booking-create', 'SERVICE_STATUS').pipe(
          retry(this.RETRY_ATTEMPTS),
          map(response => ServiceBookingActions.createServiceBookingSuccess({
            booking: response.payload,
            timestamp: new Date().toISOString()
          })),
          tap(() => {
            this.notificationService.showNotification(
              'Service booking created successfully',
              'SUCCESS',
              { duration: 5000 }
            );
          }),
          catchError(error => {
            this.errorService.handleApiError(error);
            return of(ServiceBookingActions.createServiceBookingFailure({ error }));
          })
        )
      )
    )
  );

  updateServiceBooking$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ServiceBookingActions.updateServiceBooking),
      mergeMap(action =>
        this.webSocketService.subscribe<ServiceBooking>('/topic/service-booking-update', 'SERVICE_STATUS').pipe(
          retry(this.RETRY_ATTEMPTS),
          map(response => ServiceBookingActions.updateServiceBookingSuccess({
            booking: response.payload,
            timestamp: new Date().toISOString()
          })),
          tap(() => {
            this.notificationService.showNotification(
              'Service booking updated successfully',
              'SUCCESS',
              { duration: 5000 }
            );
          }),
          catchError(error => {
            this.errorService.handleApiError(error);
            return of(ServiceBookingActions.updateServiceBookingFailure({ error }));
          })
        )
      )
    )
  );

  deleteServiceBooking$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ServiceBookingActions.deleteServiceBooking),
      mergeMap(action =>
        this.webSocketService.subscribe<number>('/topic/service-booking-delete', 'SERVICE_STATUS').pipe(
          retry(this.RETRY_ATTEMPTS),
          map(response => ServiceBookingActions.deleteServiceBookingSuccess({
            id: action.id,
            timestamp: new Date().toISOString()
          })),
          tap(() => {
            this.notificationService.showNotification(
              'Service booking deleted successfully',
              'SUCCESS',
              { duration: 5000 }
            );
          }),
          catchError(error => {
            this.errorService.handleApiError(error);
            return of(ServiceBookingActions.deleteServiceBookingFailure({ error }));
          })
        )
      )
    )
  );

  handleWebSocketUpdates$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ServiceBookingActions.serviceBookingWebSocketUpdate),
      tap(action => {
        const statusMessage = `Service booking ${action.booking.id} status updated to ${action.booking.status}`;
        this.notificationService.showNotification(
          statusMessage,
          'INFO',
          { duration: 3000 }
        );
      })
    ),
    { dispatch: false }
  );

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.webSocketService.disconnect();
  }
}