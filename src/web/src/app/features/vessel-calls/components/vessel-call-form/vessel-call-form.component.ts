import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators, AbstractControl } from '@angular/forms';
import { Store, select } from '@ngrx/store';
import { Subject, Observable, BehaviorSubject } from 'rxjs';
import { takeUntil, debounceTime, distinctUntilChanged, filter } from 'rxjs/operators';

import { VesselCall, VesselCallStatus } from '../../../../shared/models/vessel-call.model';
import { createVesselCall, updateVesselCall } from '../../../../store/actions/vessel-call.actions';
import { NotificationService } from '../../../../core/services/notification.service';
import { WebSocketService, WebSocketEventType } from '../../../../core/services/websocket.service';

@Component({
  selector: 'app-vessel-call-form',
  templateUrl: './vessel-call-form.component.html',
  styleUrls: ['./vessel-call-form.component.scss']
})
export class VesselCallFormComponent implements OnInit, OnDestroy {
  vesselCallForm: FormGroup;
  editingVesselCall: VesselCall | null = null;
  destroy$ = new Subject<void>();
  loading$ = new BehaviorSubject<boolean>(false);
  hasUnsavedChanges = false;
  vesselCallStatuses = Object.values(VesselCallStatus);

  private readonly IMO_NUMBER_REGEX = /^IMO\s*(\d{7})$/;
  private readonly CALL_SIGN_REGEX = /^[A-Z0-9]{3,7}$/;

  constructor(
    private fb: FormBuilder,
    private store: Store,
    private notificationService: NotificationService,
    private webSocketService: WebSocketService
  ) {
    this.initForm();
  }

  ngOnInit(): void {
    // Initialize WebSocket connection for real-time updates
    this.webSocketService.connect().then(() => {
      this.setupWebSocketSubscription();
    }).catch(error => {
      this.notificationService.showNotification(
        'Failed to establish real-time connection',
        'WARNING'
      );
    });

    // Track form changes
    this.vesselCallForm.valueChanges.pipe(
      takeUntil(this.destroy$),
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(() => {
      this.hasUnsavedChanges = true;
    });
  }

  private initForm(): void {
    this.vesselCallForm = this.fb.group({
      vesselName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      imoNumber: ['', [Validators.required, this.validateIMONumber.bind(this)]],
      callSign: ['', [Validators.required, Validators.pattern(this.CALL_SIGN_REGEX)]],
      status: [VesselCallStatus.PLANNED, [Validators.required]],
      eta: [null, [Validators.required]],
      etd: [null, [Validators.required]]
    }, {
      validators: [this.validateETAETD.bind(this)]
    });
  }

  private setupWebSocketSubscription(): void {
    this.webSocketService
      .subscribe<VesselCall>('/topic/vessel-updates', WebSocketEventType.VESSEL_UPDATE)
      .pipe(
        takeUntil(this.destroy$),
        filter(message => {
          return this.editingVesselCall?.id === message.payload.id;
        })
      )
      .subscribe(message => {
        this.handleVesselUpdate(message.payload);
      });
  }

  private handleVesselUpdate(updatedVessel: VesselCall): void {
    if (this.hasUnsavedChanges) {
      this.notificationService.showNotification(
        'This vessel call has been updated by another user. Please review your changes.',
        'WARNING',
        { persistent: true }
      );
    } else {
      this.vesselCallForm.patchValue(updatedVessel, { emitEvent: false });
      this.notificationService.showNotification(
        'Vessel call details have been updated',
        'INFO'
      );
    }
  }

  validateIMONumber(control: AbstractControl): { [key: string]: any } | null {
    if (!control.value) {
      return null;
    }

    const value = control.value.toString().trim();
    const matches = value.match(this.IMO_NUMBER_REGEX);

    if (!matches) {
      return { imoFormat: true };
    }

    const digits = matches[1];
    let sum = 0;
    for (let i = 0; i < 6; i++) {
      sum += parseInt(digits[i]) * (7 - i);
    }

    const checkDigit = parseInt(digits[6]);
    if (checkDigit !== (sum % 10)) {
      return { imoChecksum: true };
    }

    return null;
  }

  validateETAETD(group: FormGroup): { [key: string]: any } | null {
    const eta = group.get('eta')?.value;
    const etd = group.get('etd')?.value;

    if (eta && etd && new Date(eta) >= new Date(etd)) {
      return { etaAfterEtd: true };
    }

    return null;
  }

  onSubmit(): void {
    if (this.vesselCallForm.invalid || this.loading$.value) {
      return;
    }

    this.loading$.next(true);

    const formValue = this.vesselCallForm.value;
    const vesselCall: Partial<VesselCall> = {
      ...formValue,
      eta: new Date(formValue.eta),
      etd: new Date(formValue.etd)
    };

    if (this.editingVesselCall) {
      this.store.dispatch(updateVesselCall({
        id: this.editingVesselCall.id,
        changes: vesselCall,
        optimistic: true
      }));
    } else {
      this.store.dispatch(createVesselCall({ vesselCall }));
    }

    this.loading$.next(false);
    this.hasUnsavedChanges = false;
  }

  resetForm(): void {
    if (this.editingVesselCall) {
      this.vesselCallForm.patchValue(this.editingVesselCall);
    } else {
      this.vesselCallForm.reset({
        status: VesselCallStatus.PLANNED
      });
    }
    this.hasUnsavedChanges = false;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.webSocketService.disconnect();
  }
}