import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { Observable, of, Subject, timer } from 'rxjs';
import { catchError, map, mergeMap, tap, debounceTime, retry, takeUntil } from 'rxjs/operators';

import { IBerthAllocation, BerthAllocationStatus } from '../../shared/models/berth-allocation.model';
import { ApiService } from '../../core/http/api.service';
import { WebSocketService, WebSocketEventType } from '../../core/services/websocket.service';
import { LoggingService } from '@core/logging';

import * as BerthAllocationActions from '../actions/berth-allocation.actions';

@Injectable()
export class BerthAllocationEffects {
  private readonly API_ENDPOINT = '/api/v1/berth-allocations';
  private destroy$ = new Subject<void>();
  private readonly RETRY_ATTEMPTS = 3;
  private readonly DEBOUNCE_TIME = 300;

  loadBerthAllocations$ = createEffect(() => 
    this.actions$.pipe(
      ofType(BerthAllocationActions.loadBerthAllocations),
      debounceTime(this.DEBOUNCE_TIME),
      mergeMap(({ filters }) => 
        this.apiService.get<IBerthAllocation[]>(this.API_ENDPOINT, filters).pipe(
          retry(this.RETRY_ATTEMPTS),
          tap(() => this.loggingService.debug('Loading berth allocations', { filters })),
          map(response => BerthAllocationActions.loadBerthAllocationsSuccess({ allocations: response.data })),
          catchError(error => {
            this.loggingService.error('Failed to load berth allocations', error);
            return of(BerthAllocationActions.loadBerthAllocationsFailure({ error }));
          })
        )
      )
    )
  );

  createBerthAllocation$ = createEffect(() =>
    this.actions$.pipe(
      ofType(BerthAllocationActions.createBerthAllocation),
      mergeMap(({ allocation }) =>
        this.apiService.post<IBerthAllocation>(this.API_ENDPOINT, allocation).pipe(
          retry(this.RETRY_ATTEMPTS),
          tap(() => this.loggingService.debug('Creating berth allocation', { allocation })),
          map(response => BerthAllocationActions.createBerthAllocationSuccess({ allocation: response.data })),
          catchError(error => {
            this.loggingService.error('Failed to create berth allocation', error);
            return of(BerthAllocationActions.createBerthAllocationFailure({ error }));
          })
        )
      )
    )
  );

  updateBerthAllocation$ = createEffect(() =>
    this.actions$.pipe(
      ofType(BerthAllocationActions.updateBerthAllocation),
      mergeMap(({ id, changes }) =>
        this.apiService.put<IBerthAllocation>(`${this.API_ENDPOINT}/${id}`, changes).pipe(
          retry(this.RETRY_ATTEMPTS),
          tap(() => this.loggingService.debug('Updating berth allocation', { id, changes })),
          map(response => BerthAllocationActions.updateBerthAllocationSuccess({ allocation: response.data })),
          catchError(error => {
            this.loggingService.error('Failed to update berth allocation', error);
            return of(BerthAllocationActions.updateBerthAllocationFailure({ error }));
          })
        )
      )
    )
  );

  deleteBerthAllocation$ = createEffect(() =>
    this.actions$.pipe(
      ofType(BerthAllocationActions.deleteBerthAllocation),
      mergeMap(({ id }) =>
        this.apiService.delete<void>(`${this.API_ENDPOINT}/${id}`).pipe(
          retry(this.RETRY_ATTEMPTS),
          tap(() => this.loggingService.debug('Deleting berth allocation', { id })),
          map(() => BerthAllocationActions.deleteBerthAllocationSuccess({ id })),
          catchError(error => {
            this.loggingService.error('Failed to delete berth allocation', error);
            return of(BerthAllocationActions.deleteBerthAllocationFailure({ error }));
          })
        )
      )
    )
  );

  handleRealTimeUpdates$ = createEffect(() => 
    this.actions$.pipe(
      ofType(BerthAllocationActions.initializeWebSocket),
      mergeMap(() => 
        this.wsService.connect().then(() => 
          this.wsService.subscribe<IBerthAllocation>(
            '/topic/berth-allocations',
            WebSocketEventType.BERTH_CHANGE
          ).pipe(
            takeUntil(this.destroy$),
            map(message => {
              const allocation = message.payload;
              switch (allocation.status) {
                case BerthAllocationStatus.SCHEDULED:
                  return BerthAllocationActions.berthAllocationScheduled({ allocation });
                case BerthAllocationStatus.OCCUPIED:
                  return BerthAllocationActions.berthAllocationOccupied({ allocation });
                case BerthAllocationStatus.COMPLETED:
                  return BerthAllocationActions.berthAllocationCompleted({ allocation });
                case BerthAllocationStatus.CANCELLED:
                  return BerthAllocationActions.berthAllocationCancelled({ allocation });
                default:
                  return BerthAllocationActions.berthAllocationUpdated({ allocation });
              }
            }),
            catchError(error => {
              this.loggingService.error('WebSocket error in berth allocations', error);
              return of(BerthAllocationActions.webSocketError({ error }));
            })
          )
        )
      )
    )
  );

  constructor(
    private actions$: Actions,
    private apiService: ApiService,
    private wsService: WebSocketService,
    private loggingService: LoggingService
  ) {}

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}