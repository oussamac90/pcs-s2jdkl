import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { Store, select } from '@ngrx/store';
import { Subject, BehaviorSubject } from 'rxjs';
import { takeUntil, debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { 
  Clearance, 
  ClearanceType, 
  ClearanceStatus 
} from '../../../../shared/models/clearance.model';
import { NotificationService } from '../../../../core/services/notification.service';
import { 
  loadClearances, 
  updateClearanceStatus 
} from '../../../../store/actions/clearance.actions';

@Component({
  selector: 'app-clearance-workflow',
  templateUrl: './clearance-workflow.component.html',
  styleUrls: [
    './clearance-workflow.component.scss',
    './clearance-workflow.component.responsive.scss'
  ]
})
export class ClearanceWorkflowComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  private clearancesSubject$ = new BehaviorSubject<Clearance[]>([]);

  clearances: Clearance[] = [];
  loading = false;
  clearanceTypes = Object.values(ClearanceType);
  
  // Weighted progress calculation for different clearance types
  private clearanceWeights = new Map<ClearanceType, number>([
    [ClearanceType.CUSTOMS, 0.3],
    [ClearanceType.IMMIGRATION, 0.2],
    [ClearanceType.PORT_AUTHORITY, 0.2],
    [ClearanceType.HEALTH, 0.15],
    [ClearanceType.SECURITY, 0.15]
  ]);

  constructor(
    private store: Store,
    private notificationService: NotificationService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // Load initial clearances with pagination
    this.store.dispatch(loadClearances({ 
      page: 0, 
      pageSize: 10 
    }));

    // Subscribe to clearance state changes
    this.store.pipe(
      select(state => state['clearance'].clearances),
      takeUntil(this.destroy$),
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(clearances => {
      this.clearances = clearances;
      this.clearancesSubject$.next(clearances);
      this.cdr.detectChanges();
    });

    // Subscribe to loading state
    this.store.pipe(
      select(state => state['clearance'].loading),
      takeUntil(this.destroy$)
    ).subscribe(loading => {
      this.loading = loading;
      this.cdr.detectChanges();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.clearancesSubject$.complete();
  }

  /**
   * Updates the status of a clearance with validation and audit logging
   * @param clearanceId The ID of the clearance to update
   * @param newStatus The new status to set
   * @param remarks Optional remarks for the status change
   */
  updateStatus(clearanceId: number, newStatus: ClearanceStatus, remarks?: string): void {
    const clearance = this.clearances.find(c => c.id === clearanceId);
    
    if (!clearance) {
      this.notificationService.showError('Clearance not found');
      return;
    }

    if (!this.canProceed(clearance)) {
      this.notificationService.showWarning(
        'Cannot proceed - previous clearances must be approved first'
      );
      return;
    }

    if (!this.isValidStatusTransition(clearance.status, newStatus)) {
      this.notificationService.showError('Invalid status transition');
      return;
    }

    this.store.dispatch(updateClearanceStatus({
      id: clearanceId,
      status: newStatus,
      remarks: remarks,
      userId: 1, // TODO: Get from auth service
      validUntil: this.calculateValidityPeriod(clearance.type)
    }));
  }

  /**
   * Calculates the weighted progress percentage of the clearance workflow
   * @returns Progress percentage between 0-100
   */
  getProgressPercentage(): number {
    if (!this.clearances.length) return 0;

    let totalWeight = 0;
    let completedWeight = 0;

    this.clearances.forEach(clearance => {
      const weight = this.clearanceWeights.get(clearance.type) || 0;
      totalWeight += weight;

      if (clearance.status === ClearanceStatus.APPROVED) {
        completedWeight += weight;
      } else if (clearance.status === ClearanceStatus.IN_PROGRESS) {
        completedWeight += (weight * 0.5);
      }
    });

    return Math.round((completedWeight / totalWeight) * 100);
  }

  /**
   * Checks if a clearance can proceed based on workflow rules
   * @param clearance The clearance to check
   * @returns boolean indicating if the clearance can proceed
   */
  canProceed(clearance: Clearance): boolean {
    const clearanceOrder = [
      ClearanceType.CUSTOMS,
      ClearanceType.IMMIGRATION,
      ClearanceType.HEALTH,
      ClearanceType.SECURITY,
      ClearanceType.PORT_AUTHORITY
    ];

    const currentIndex = clearanceOrder.indexOf(clearance.type);
    if (currentIndex === 0) return true;

    // Check if all previous clearances are approved
    for (let i = 0; i < currentIndex; i++) {
      const previousType = clearanceOrder[i];
      const previousClearance = this.clearances.find(c => c.type === previousType);
      
      if (!previousClearance || previousClearance.status !== ClearanceStatus.APPROVED) {
        return false;
      }
    }

    return true;
  }

  /**
   * Validates if a status transition is allowed
   * @param currentStatus Current clearance status
   * @param newStatus Proposed new status
   * @returns boolean indicating if the transition is valid
   */
  private isValidStatusTransition(
    currentStatus: ClearanceStatus, 
    newStatus: ClearanceStatus
  ): boolean {
    const validTransitions = new Map<ClearanceStatus, ClearanceStatus[]>([
      [ClearanceStatus.PENDING, [ClearanceStatus.IN_PROGRESS, ClearanceStatus.CANCELLED]],
      [ClearanceStatus.IN_PROGRESS, [ClearanceStatus.APPROVED, ClearanceStatus.REJECTED]],
      [ClearanceStatus.REJECTED, [ClearanceStatus.IN_PROGRESS]],
      [ClearanceStatus.APPROVED, [ClearanceStatus.IN_PROGRESS]],
      [ClearanceStatus.CANCELLED, [ClearanceStatus.PENDING]]
    ]);

    const allowedTransitions = validTransitions.get(currentStatus) || [];
    return allowedTransitions.includes(newStatus);
  }

  /**
   * Calculates the validity period for a clearance based on its type
   * @param type The type of clearance
   * @returns Date indicating when the clearance expires
   */
  private calculateValidityPeriod(type: ClearanceType): Date {
    const validityDays = new Map<ClearanceType, number>([
      [ClearanceType.CUSTOMS, 7],
      [ClearanceType.IMMIGRATION, 3],
      [ClearanceType.PORT_AUTHORITY, 2],
      [ClearanceType.HEALTH, 5],
      [ClearanceType.SECURITY, 3]
    ]);

    const days = validityDays.get(type) || 1;
    const validUntil = new Date();
    validUntil.setDate(validUntil.getDate() + days);
    return validUntil;
  }
}