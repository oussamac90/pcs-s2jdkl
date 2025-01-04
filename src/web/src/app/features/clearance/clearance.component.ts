import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { Store } from '@ngrx/store';
import { Observable, Subject } from 'rxjs';
import { takeUntil, map, distinctUntilChanged } from 'rxjs/operators';

import { Clearance, ClearanceStatus, ClearanceType } from '../../shared/models/clearance.model';
import * as ClearanceActions from '../../store/actions/clearance.actions';
import * as ClearanceSelectors from '../../store/selectors/clearance.selectors';
import { AppState } from '../../store/state/app.state';

@Component({
  selector: 'app-clearance',
  templateUrl: './clearance.component.html',
  styleUrls: ['./clearance.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ClearanceComponent implements OnInit, OnDestroy {
  // Observables for reactive data management
  clearances$: Observable<Clearance[]>;
  pendingClearances$: Observable<Clearance[]>;
  approvedClearances$: Observable<Clearance[]>;
  loading$: Observable<boolean>;
  error$: Observable<string | null>;

  // Component state
  progressPercentage: number = 0;
  readonly ClearanceStatus = ClearanceStatus;
  readonly ClearanceType = ClearanceType;

  // Subscription cleanup
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly store: Store<AppState>,
    private readonly cdr: ChangeDetectorRef
  ) {
    // Initialize observables from store selectors
    this.clearances$ = this.store.select(ClearanceSelectors.selectAllClearances);
    this.pendingClearances$ = this.store.select(ClearanceSelectors.selectPendingClearances);
    this.approvedClearances$ = this.store.select(ClearanceSelectors.selectApprovedClearances);
    this.loading$ = this.store.select(ClearanceSelectors.selectClearanceLoading);
    this.error$ = this.store.select(ClearanceSelectors.selectClearanceError);
  }

  ngOnInit(): void {
    // Load initial clearance data
    this.store.dispatch(ClearanceActions.loadClearances({
      page: 0,
      pageSize: 50
    }));

    // Track clearance progress
    this.clearances$
      .pipe(
        takeUntil(this.destroy$),
        distinctUntilChanged(),
        map(clearances => this.calculateProgress(clearances))
      )
      .subscribe(progress => {
        this.progressPercentage = progress;
        this.cdr.markForCheck();
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Updates the status of a clearance with comprehensive error handling
   * @param clearanceId The ID of the clearance to update
   * @param newStatus The new status to set
   * @param remarks Optional remarks for the status update
   */
  updateClearanceStatus(clearanceId: number, newStatus: ClearanceStatus, remarks?: string): void {
    this.store.dispatch(ClearanceActions.updateClearanceStatus({
      id: clearanceId,
      status: newStatus,
      remarks,
      userId: this.getCurrentUserId(), // Implement based on auth service
      validUntil: this.calculateValidityPeriod(newStatus)
    }));
  }

  /**
   * Calculates the overall progress of clearance workflow
   * @param clearances Array of clearances to analyze
   * @returns Progress percentage as a number between 0 and 100
   */
  private calculateProgress(clearances: Clearance[]): number {
    if (!clearances.length) return 0;

    const totalClearances = clearances.length;
    const completedClearances = clearances.filter(
      c => c.status === ClearanceStatus.APPROVED || c.status === ClearanceStatus.REJECTED
    ).length;

    return Math.round((completedClearances / totalClearances) * 100);
  }

  /**
   * Retrieves clearances filtered by type
   * @param type The clearance type to filter by
   */
  getClearancesByType(type: ClearanceType): void {
    this.store.dispatch(ClearanceActions.loadClearances({
      page: 0,
      pageSize: 50,
      type
    }));
  }

  /**
   * Handles the submission of a new clearance request
   * @param vesselCallId The ID of the vessel call
   * @param type The type of clearance being requested
   * @param documents Optional array of supporting documents
   */
  submitClearanceRequest(vesselCallId: number, type: ClearanceType, documents?: File[]): void {
    this.store.dispatch(ClearanceActions.submitClearance({
      vesselCallId,
      type,
      submittedBy: this.getCurrentUserId().toString(),
      documents
    }));
  }

  /**
   * Calculates the validity period for a clearance based on its status
   * @param status The clearance status
   * @returns Date object representing the validity end date
   */
  private calculateValidityPeriod(status: ClearanceStatus): Date {
    if (status === ClearanceStatus.APPROVED) {
      const validityDate = new Date();
      validityDate.setHours(validityDate.getHours() + 24); // 24-hour validity
      return validityDate;
    }
    return null;
  }

  /**
   * Retrieves the current user's ID from the authentication service
   * @returns The current user's ID
   */
  private getCurrentUserId(): number {
    // Implement based on authentication service
    return 1; // Placeholder
  }
}