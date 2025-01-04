import { createAction, props } from '@ngrx/store';
import { HttpErrorResponse } from '@angular/common/http';
import { Clearance, ClearanceStatus, ClearanceType } from '../../shared/models/clearance.model';

/**
 * Enumeration of all Clearance-related action types
 * Provides type safety and centralized management of action types
 */
export enum ClearanceActionTypes {
  // Load clearances
  LOAD = '[Clearance] Load Clearances',
  LOAD_SUCCESS = '[Clearance] Load Clearances Success',
  LOAD_FAILURE = '[Clearance] Load Clearances Failure',

  // Update clearance status
  UPDATE_STATUS = '[Clearance] Update Status',
  UPDATE_STATUS_SUCCESS = '[Clearance] Update Status Success',
  UPDATE_STATUS_FAILURE = '[Clearance] Update Status Failure',

  // Submit new clearance
  SUBMIT = '[Clearance] Submit Clearance',
  SUBMIT_SUCCESS = '[Clearance] Submit Clearance Success',
  SUBMIT_FAILURE = '[Clearance] Submit Clearance Failure',

  // Load single clearance
  LOAD_ONE = '[Clearance] Load Single',
  LOAD_ONE_SUCCESS = '[Clearance] Load Single Success',
  LOAD_ONE_FAILURE = '[Clearance] Load Single Failure'
}

/**
 * Load clearances with pagination
 */
export const loadClearances = createAction(
  ClearanceActionTypes.LOAD,
  props<{
    page: number;
    pageSize: number;
    vesselId?: number;
    type?: ClearanceType;
    status?: ClearanceStatus;
  }>()
);

export const loadClearancesSuccess = createAction(
  ClearanceActionTypes.LOAD_SUCCESS,
  props<{
    clearances: Clearance[];
    total: number;
    page: number;
    pageSize: number;
  }>()
);

export const loadClearancesFailure = createAction(
  ClearanceActionTypes.LOAD_FAILURE,
  props<{ error: HttpErrorResponse }>()
);

/**
 * Load single clearance by ID
 */
export const loadClearance = createAction(
  ClearanceActionTypes.LOAD_ONE,
  props<{ id: number }>()
);

export const loadClearanceSuccess = createAction(
  ClearanceActionTypes.LOAD_ONE_SUCCESS,
  props<{ clearance: Clearance }>()
);

export const loadClearanceFailure = createAction(
  ClearanceActionTypes.LOAD_ONE_FAILURE,
  props<{ error: HttpErrorResponse }>()
);

/**
 * Update clearance status with audit information
 */
export const updateClearanceStatus = createAction(
  ClearanceActionTypes.UPDATE_STATUS,
  props<{
    id: number;
    status: ClearanceStatus;
    remarks?: string;
    userId: number;
    validUntil?: Date;
  }>()
);

export const updateClearanceStatusSuccess = createAction(
  ClearanceActionTypes.UPDATE_STATUS_SUCCESS,
  props<{ clearance: Clearance }>()
);

export const updateClearanceStatusFailure = createAction(
  ClearanceActionTypes.UPDATE_STATUS_FAILURE,
  props<{ error: HttpErrorResponse }>()
);

/**
 * Submit new clearance request
 */
export const submitClearance = createAction(
  ClearanceActionTypes.SUBMIT,
  props<{
    vesselCallId: number;
    type: ClearanceType;
    remarks?: string;
    submittedBy: string;
    documents?: File[];
  }>()
);

export const submitClearanceSuccess = createAction(
  ClearanceActionTypes.SUBMIT_SUCCESS,
  props<{ clearance: Clearance }>()
);

export const submitClearanceFailure = createAction(
  ClearanceActionTypes.SUBMIT_FAILURE,
  props<{ error: HttpErrorResponse }>()
);