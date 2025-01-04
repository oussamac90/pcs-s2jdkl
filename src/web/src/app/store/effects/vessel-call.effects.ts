import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { Action } from '@ngrx/store';
import { Observable, of, timer, EMPTY } from 'rxjs';
import { catchError, map, mergeMap, tap, debounceTime, retry, switchMap, takeUntil } from 'rxjs/operators';

import * as VesselCallActions from '../actions/vessel-call.actions';
import { NotificationService } from '../../core/services/notification.service';
import { ErrorService } from '../../core/services/error.service';
import { WebSocketService, WebSocketEventType } from '../../core/services/websocket.service';
import { VesselCall, VesselCallStatus } from '../../shared/models/vessel-call.model';

@Injectable()
export class VesselCallEffects {
  // Constants for configuration
  private readonly RETRY_ATTEMPTS = 3;
  private readonly DEBOUNCE_TIME = 300;

  // Load vessel calls effect
  loadVesselCalls$ = createEffect(() => 
    this.actions$.pipe(
      ofType(VesselCallActions.loadVesselCalls),
      debounceTime(this.DEBOUNCE_TIME),
      mergeMap(action => 
        this.vesselCallService.getVesselCalls(action.filters, action.pagination).pipe(
          retry(this.RETRY_ATTEMPTS),
          map(response => VesselCallActions.loadVesselCallsSuccess({
            vesselCalls: response.data,
            totalCount: response.totalItems,
            page: action.pagination?.page || 0
          })),
          catchError(error => of(VesselCallActions.loadVesselCallsFailure({
            error: this.errorService.handleApiError(error)
          })))
        )
      )
    )
  );

  // Create vessel call effect
  createVesselCall$ = createEffect(() =>
    this.actions$.pipe(
      ofType(VesselCallActions.createVesselCall),
      mergeMap(action =>
        this.vesselCallService.createVesselCall(action.vesselCall).pipe(
          map(response => {
            this.notificationService.showSuccess(
              `Vessel call created successfully for ${response.data.vesselName}`
            );
            return VesselCallActions.createVesselCallSuccess({ vesselCall: response.data });
          }),
          catchError(error => {
            const errorResponse = this.errorService.handleApiError(error);
            this.notificationService.showError(errorResponse.message);
            return of(VesselCallActions.createVesselCallFailure({ error: errorResponse }));
          })
        )
      )
    )
  );

  // Update vessel call effect
  updateVesselCall$ = createEffect(() =>
    this.actions$.pipe(
      ofType(VesselCallActions.updateVesselCall),
      mergeMap(action =>
        this.vesselCallService.updateVesselCall(action.id, action.changes).pipe(
          map(response => {
            this.notificationService.showSuccess(
              `Vessel call updated successfully for ID: ${action.id}`
            );
            return VesselCallActions.updateVesselCallSuccess({
              id: action.id,
              changes: response.data,
              timestamp: new Date()
            });
          }),
          catchError(error => {
            const errorResponse = this.errorService.handleApiError(error);
            this.notificationService.showError(errorResponse.message);
            return of(VesselCallActions.updateVesselCallFailure({
              id: action.id,
              error: errorResponse,
              revertChanges: action.changes
            }));
          })
        )
      )
    )
  );

  // Delete vessel call effect
  deleteVesselCall$ = createEffect(() =>
    this.actions$.pipe(
      ofType(VesselCallActions.deleteVesselCall),
      mergeMap(action =>
        this.vesselCallService.deleteVesselCall(action.id).pipe(
          map(() => {
            this.notificationService.showSuccess(
              `Vessel call deleted successfully`
            );
            return VesselCallActions.deleteVesselCallSuccess({ id: action.id });
          }),
          catchError(error => {
            const errorResponse = this.errorService.handleApiError(error);
            this.notificationService.showError(errorResponse.message);
            return of(VesselCallActions.deleteVesselCallFailure({
              id: action.id,
              error: errorResponse
            }));
          })
        )
      )
    )
  );

  // WebSocket updates effect
  websocketUpdates$ = createEffect(() => {
    return this.webSocketService.subscribe<VesselCall>(
      '/topic/vessel-updates',
      WebSocketEventType.VESSEL_UPDATE
    ).pipe(
      map(message => {
        const vesselCall = message.payload;
        if (vesselCall.status === VesselCallStatus.ARRIVED) {
          this.notificationService.showSuccess(
            `Vessel ${vesselCall.vesselName} has arrived`
          );
        }
        return VesselCallActions.syncVesselCallStatus({
          id: vesselCall.id,
          status: vesselCall.status,
          timestamp: new Date(message.timestamp)
        });
      }),
      catchError(error => {
        this.errorService.handleApiError(error);
        return EMPTY;
      })
    );
  });

  constructor(
    private actions$: Actions,
    private notificationService: NotificationService,
    private errorService: ErrorService,
    private webSocketService: WebSocketService,
    private vesselCallService: VesselCallService
  ) {}
}