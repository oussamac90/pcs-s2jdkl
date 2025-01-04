import { createAction, props } from '@ngrx/store';
import { VesselCall, VesselCallStatus } from '../../shared/models/vessel-call.model';

/**
 * Error type enumeration for specific error categorization
 */
export enum ErrorTypes {
    NETWORK = 'NETWORK',
    VALIDATION = 'VALIDATION',
    AUTHORIZATION = 'AUTHORIZATION',
    UNKNOWN = 'UNKNOWN'
}

/**
 * Interface for error payload structure
 */
interface ErrorPayload {
    message: string;
    code: string;
    type: ErrorTypes;
    details?: any;
}

/**
 * Load Vessel Calls Actions
 */
export const loadVesselCalls = createAction(
    '[Vessel Call] Load Vessel Calls',
    props<{
        filters?: {
            status?: VesselCallStatus;
            dateRange?: {
                start: Date;
                end: Date;
            };
        };
        pagination?: {
            page: number;
            pageSize: number;
        };
    }>()
);

export const loadVesselCallsSuccess = createAction(
    '[Vessel Call] Load Vessel Calls Success',
    props<{
        vesselCalls: VesselCall[];
        totalCount: number;
        page: number;
    }>()
);

export const loadVesselCallsFailure = createAction(
    '[Vessel Call] Load Vessel Calls Failure',
    props<{ error: ErrorPayload }>()
);

/**
 * Create Vessel Call Actions
 */
export const createVesselCall = createAction(
    '[Vessel Call] Create Vessel Call',
    props<{ vesselCall: Omit<VesselCall, 'id' | 'createdAt' | 'updatedAt'> }>()
);

export const createVesselCallSuccess = createAction(
    '[Vessel Call] Create Vessel Call Success',
    props<{ vesselCall: VesselCall }>()
);

export const createVesselCallFailure = createAction(
    '[Vessel Call] Create Vessel Call Failure',
    props<{ error: ErrorPayload }>()
);

/**
 * Update Vessel Call Actions
 */
export const updateVesselCall = createAction(
    '[Vessel Call] Update Vessel Call',
    props<{
        id: number;
        changes: Partial<VesselCall>;
        optimistic?: boolean;
    }>()
);

export const updateVesselCallSuccess = createAction(
    '[Vessel Call] Update Vessel Call Success',
    props<{
        id: number;
        changes: Partial<VesselCall>;
        timestamp: Date;
    }>()
);

export const updateVesselCallFailure = createAction(
    '[Vessel Call] Update Vessel Call Failure',
    props<{
        id: number;
        error: ErrorPayload;
        revertChanges: Partial<VesselCall>;
    }>()
);

/**
 * Delete Vessel Call Actions
 */
export const deleteVesselCall = createAction(
    '[Vessel Call] Delete Vessel Call',
    props<{ id: number }>()
);

export const deleteVesselCallSuccess = createAction(
    '[Vessel Call] Delete Vessel Call Success',
    props<{ id: number }>()
);

export const deleteVesselCallFailure = createAction(
    '[Vessel Call] Delete Vessel Call Failure',
    props<{
        id: number;
        error: ErrorPayload;
    }>()
);

/**
 * Selection Actions
 */
export const selectVesselCall = createAction(
    '[Vessel Call] Select Vessel Call',
    props<{ id: number }>()
);

export const clearSelectedVesselCall = createAction(
    '[Vessel Call] Clear Selected Vessel Call'
);

/**
 * Real-time Synchronization Actions
 */
export const syncVesselCallStatus = createAction(
    '[Vessel Call] Sync Status',
    props<{
        id: number;
        status: VesselCallStatus;
        timestamp: Date;
    }>()
);

/**
 * Batch Operations Actions
 */
export const batchUpdateVesselCalls = createAction(
    '[Vessel Call] Batch Update',
    props<{
        updates: Array<{
            id: number;
            changes: Partial<VesselCall>;
        }>;
    }>()
);

/**
 * Optimization Actions
 */
export const optimizeVesselCallUpdates = createAction(
    '[Vessel Call] Optimize Updates',
    props<{
        strategy: 'debounce' | 'throttle';
        timeWindow: number;
    }>()
);