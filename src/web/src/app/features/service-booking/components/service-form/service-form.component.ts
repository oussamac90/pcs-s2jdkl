import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { Subject } from 'rxjs';
import { takeUntil, debounceTime, catchError, retry } from 'rxjs/operators';

import { ServiceBooking, ServiceType, ServiceStatus } from '../../../../shared/models/service-booking.model';
import { createServiceBooking, updateServiceBooking } from '../../../../store/actions/service-booking.actions';
import { NotificationService, NotificationType } from '../../../../core/services/notification.service';

interface ServiceFormData {
  vesselCallId: number;
  serviceType: ServiceType;
  quantity: number;
  serviceTime: Date;
  remarks: string;
  estimatedCost: number;
}

@Component({
  selector: 'app-service-form',
  templateUrl: './service-form.component.html',
  styleUrls: ['./service-form.component.scss']
})
export class ServiceFormComponent implements OnInit, OnDestroy {
  serviceForm: FormGroup;
  serviceTypes: ServiceType[] = Object.values(ServiceType);
  destroy$ = new Subject<void>();
  isSubmitting = false;
  isLoading = false;
  editBooking: ServiceBooking | null = null;
  validationErrors: string[] = [];
  isDirty = false;

  private readonly DEBOUNCE_TIME = 300;
  private readonly MIN_QUANTITY = 1;
  private readonly MAX_QUANTITY = 5;

  constructor(
    private fb: FormBuilder,
    private store: Store,
    private notificationService: NotificationService
  ) {
    this.initializeForm();
  }

  private initializeForm(): void {
    this.serviceForm = this.fb.group({
      vesselCallId: ['', [Validators.required]],
      serviceType: ['', [Validators.required]],
      quantity: [1, [
        Validators.required,
        Validators.min(this.MIN_QUANTITY),
        Validators.max(this.MAX_QUANTITY)
      ]],
      serviceTime: ['', [Validators.required]],
      remarks: ['', [Validators.maxLength(500)]],
      estimatedCost: [{ value: 0, disabled: true }]
    });
  }

  ngOnInit(): void {
    this.setupFormValidation();
    this.setupAccessibility();
    this.setupFormStateTracking();
  }

  private setupFormValidation(): void {
    this.serviceForm.valueChanges
      .pipe(
        takeUntil(this.destroy$),
        debounceTime(this.DEBOUNCE_TIME)
      )
      .subscribe(() => {
        this.validateForm();
        this.updateEstimatedCost();
      });
  }

  private setupAccessibility(): void {
    const form = this.serviceForm;
    form.get('serviceType')?.statusChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(status => {
        const element = document.getElementById('serviceType');
        if (element) {
          element.setAttribute('aria-invalid', status === 'INVALID' ? 'true' : 'false');
        }
      });
  }

  private setupFormStateTracking(): void {
    this.serviceForm.statusChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(status => {
        this.isDirty = this.serviceForm.dirty;
        this.validationErrors = this.getValidationErrors();
      });
  }

  private validateForm(): void {
    this.validationErrors = [];
    const form = this.serviceForm;

    if (form.get('serviceTime')?.value) {
      const serviceTime = new Date(form.get('serviceTime')?.value);
      if (serviceTime < new Date()) {
        this.validationErrors.push('Service time cannot be in the past');
      }
    }

    if (form.get('quantity')?.value > this.MAX_QUANTITY) {
      this.validationErrors.push(`Maximum quantity allowed is ${this.MAX_QUANTITY}`);
    }
  }

  private updateEstimatedCost(): void {
    const serviceType = this.serviceForm.get('serviceType')?.value;
    const quantity = this.serviceForm.get('quantity')?.value;

    let baseCost = 0;
    switch (serviceType) {
      case ServiceType.PILOTAGE:
        baseCost = 1000;
        break;
      case ServiceType.TUGBOAT:
        baseCost = 800 * quantity;
        break;
      case ServiceType.MOORING:
        baseCost = 500;
        break;
      case ServiceType.UNMOORING:
        baseCost = 500;
        break;
    }

    this.serviceForm.patchValue({ estimatedCost: baseCost }, { emitEvent: false });
  }

  private getValidationErrors(): string[] {
    const errors: string[] = [];
    Object.keys(this.serviceForm.controls).forEach(key => {
      const control = this.serviceForm.get(key);
      if (control?.errors) {
        Object.keys(control.errors).forEach(errorKey => {
          switch (errorKey) {
            case 'required':
              errors.push(`${key} is required`);
              break;
            case 'min':
              errors.push(`${key} must be at least ${control.errors[errorKey].min}`);
              break;
            case 'max':
              errors.push(`${key} must not exceed ${control.errors[errorKey].max}`);
              break;
            default:
              errors.push(`${key} is invalid`);
          }
        });
      }
    });
    return errors;
  }

  onSubmit(): void {
    if (this.serviceForm.invalid || this.isSubmitting) {
      return;
    }

    this.isSubmitting = true;
    const formData: ServiceFormData = this.serviceForm.getRawValue();

    const booking = {
      ...formData,
      status: ServiceStatus.REQUESTED,
      serviceTime: formData.serviceTime.toISOString()
    };

    const action = this.editBooking
      ? updateServiceBooking({ 
          id: this.editBooking.id,
          ...booking
        })
      : createServiceBooking(booking);

    this.store.dispatch(action);

    this.notificationService.showNotification(
      'Service booking submitted successfully',
      NotificationType.SUCCESS,
      {
        duration: 5000,
        action: 'View Details'
      }
    );

    this.resetForm();
  }

  resetForm(): void {
    this.serviceForm.reset({
      quantity: 1,
      estimatedCost: 0
    });
    this.isSubmitting = false;
    this.isDirty = false;
    this.validationErrors = [];
    this.editBooking = null;

    // Reset accessibility attributes
    Object.keys(this.serviceForm.controls).forEach(key => {
      const element = document.getElementById(key);
      if (element) {
        element.setAttribute('aria-invalid', 'false');
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}