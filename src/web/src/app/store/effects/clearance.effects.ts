import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { of, throwError } from 'rxjs';
import { catchError, map, mergeMap, retry, timeout, takeUntil } from 'rxjs/operators';
import * as ClearanceActions from '../actions/clearance.actions';
import { ApiService } from '../../core/http/api.service';
import { NotificationService } from '../../core/services/notification.service';
import { ErrorService } from '../../core/services/error.service';
import { Subject } from 'rxjs';

@Injectable()
export class ClearanceEffects {
  private destroy$ = new Subject<void>();
  private readonly TIMEOUT_DURATION = 30000; // 30 seconds
  private readonly MAX_RETRIES = 3;

  loadClearances$ = createEffect(() => 
    this.actions$.pipe(
      ofType(ClearanceActions.loadClearances),
      mergeMap(action => {
        const requestId = `load_clearances_${Date.now()}`;
        this.notificationService.showProgress('Loading clearances...', { priority: 2 });

        return this.apiService.get<any>(
          `/clearances`, 
          {
            page: action.page.toString(),
            pageSize: action.pageSize.toString(),
            ...(action.vesselId && { vesselId: action.vesselId.toString() }),
            ...(action.type && { type: action.type }),
            ...(action.status && { status: action.status })
          }
        ).pipe(
          timeout(this.TIMEOUT_DURATION),
          retry({ count: this.MAX_RETRIES, delay: 1000 }),
          map(response => {
            this.notificationService.showSuccess('Clearances loaded successfully');
            return ClearanceActions.loadClearancesSuccess({
              clearances: response.data,
              total: response.totalItems,
              page: action.page,
              pageSize: action.pageSize
            });
          }),
          catchError(error => {
            const transformedError = this.errorService.transformError(error);
            this.notificationService.showError(
              `Failed to load clearances: ${transformedError.message}`,
              { persistent: true }
            );
            return of(ClearanceActions.loadClearancesFailure({ error: transformedError }));
          }),
          takeUntil(this.destroy$)
        );
      })
    )
  );

  updateClearanceStatus$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ClearanceActions.updateClearanceStatus),
      mergeMap(action => {
        const requestId = `update_clearance_${action.id}_${Date.now()}`;
        this.notificationService.showProgress(
          'Updating clearance status...',
          { priority: 4 }
        );

        return this.apiService.put<any>(
          `/clearances/${action.id}/status`,
          {
            status: action.status,
            remarks: action.remarks,
            userId: action.userId,
            validUntil: action.validUntil
          }
        ).pipe(
          timeout(this.TIMEOUT_DURATION),
          retry({ count: this.MAX_RETRIES, delay: 1000 }),
          map(response => {
            this.notificationService.showSuccess(
              `Clearance status updated to ${action.status}`,
              { persistent: true }
            );
            return ClearanceActions.updateClearanceStatusSuccess({
              clearance: response.data
            });
          }),
          catchError(error => {
            const transformedError = this.errorService.transformError(error);
            this.notificationService.showPersistentError(
              `Failed to update clearance status: ${transformedError.message}`
            );
            return of(ClearanceActions.updateClearanceStatusFailure({ error: transformedError }));
          }),
          takeUntil(this.destroy$)
        );
      })
    )
  );

  submitClearance$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ClearanceActions.submitClearance),
      mergeMap(action => {
        const requestId = `submit_clearance_${Date.now()}`;
        this.notificationService.showProgress(
          'Submitting clearance request...',
          { priority: 4 }
        );

        // Create FormData for file upload
        const formData = new FormData();
        formData.append('vesselCallId', action.vesselCallId.toString());
        formData.append('type', action.type);
        formData.append('submittedBy', action.submittedBy);
        if (action.remarks) {
          formData.append('remarks', action.remarks);
        }
        if (action.documents) {
          action.documents.forEach((file, index) => {
            formData.append(`documents[${index}]`, file);
          });
        }

        return this.apiService.post<any>(
          '/clearances',
          formData
        ).pipe(
          timeout(this.TIMEOUT_DURATION),
          retry({ count: this.MAX_RETRIES, delay: 1000 }),
          map(response => {
            this.notificationService.showSuccess(
              'Clearance request submitted successfully',
              { persistent: true }
            );
            return ClearanceActions.submitClearanceSuccess({
              clearance: response.data
            });
          }),
          catchError(error => {
            const transformedError = this.errorService.transformError(error);
            this.notificationService.showPersistentError(
              `Failed to submit clearance request: ${transformedError.message}`
            );
            return of(ClearanceActions.submitClearanceFailure({ error: transformedError }));
          }),
          takeUntil(this.destroy$)
        );
      })
    )
  );

  constructor(
    private actions$: Actions,
    private apiService: ApiService,
    private notificationService: NotificationService,
    private errorService: ErrorService
  ) {}

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}