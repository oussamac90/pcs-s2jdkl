import { createAction, props } from '@ngrx/store';
import { IBerthAllocation } from '../../shared/models/berth-allocation.model';

/**
 * Enum containing all action types for berth allocation state management.
 * Provides type-safe action type constants with descriptive namespacing.
 */
export enum BerthAllocationActionTypes {
  LOAD_BERTH_ALLOCATIONS = '[Berth Allocation] Load Berth Allocations',
  LOAD_BERTH_ALLOCATIONS_SUCCESS = '[Berth Allocation] Load Berth Allocations Success',
  LOAD_BERTH_ALLOCATIONS_FAILURE = '[Berth Allocation] Load Berth Allocations Failure',
  
  CREATE_BERTH_ALLOCATION = '[Berth Allocation] Create Berth Allocation',
  CREATE_BERTH_ALLOCATION_SUCCESS = '[Berth Allocation] Create Berth Allocation Success',
  CREATE_BERTH_ALLOCATION_FAILURE = '[Berth Allocation] Create Berth Allocation Failure',
  
  UPDATE_BERTH_ALLOCATION = '[Berth Allocation] Update Berth Allocation',
  UPDATE_BERTH_ALLOCATION_SUCCESS = '[Berth Allocation] Update Berth Allocation Success',
  UPDATE_BERTH_ALLOCATION_FAILURE = '[Berth Allocation] Update Berth Allocation Failure',
  
  DELETE_BERTH_ALLOCATION = '[Berth Allocation] Delete Berth Allocation',
  DELETE_BERTH_ALLOCATION_SUCCESS = '[Berth Allocation] Delete Berth Allocation Success',
  DELETE_BERTH_ALLOCATION_FAILURE = '[Berth Allocation] Delete Berth Allocation Failure',
  
  CLEAR_BERTH_ALLOCATION_ERROR = '[Berth Allocation] Clear Error',
  RESET_BERTH_ALLOCATION_STATE = '[Berth Allocation] Reset State',
  UPDATE_BERTH_ALLOCATION_STATUS = '[Berth Allocation] Update Status',
  SYNC_BERTH_ALLOCATION = '[Berth Allocation] Sync WebSocket Update'
}

/**
 * Interface for berth allocation filter criteria
 */
export interface BerthAllocationFilters {
  berthId?: number;
  vesselId?: number;
  startDate?: Date;
  endDate?: Date;
  status?: string[];
}

// Load Actions
export const loadBerthAllocations = createAction(
  BerthAllocationActionTypes.LOAD_BERTH_ALLOCATIONS,
  props<{ filters?: BerthAllocationFilters }>()
);

export const loadBerthAllocationsSuccess = createAction(
  BerthAllocationActionTypes.LOAD_BERTH_ALLOCATIONS_SUCCESS,
  props<{ berthAllocations: IBerthAllocation[]; timestamp: Date }>()
);

export const loadBerthAllocationsFailure = createAction(
  BerthAllocationActionTypes.LOAD_BERTH_ALLOCATIONS_FAILURE,
  props<{ error: string }>()
);

// Create Actions
export const createBerthAllocation = createAction(
  BerthAllocationActionTypes.CREATE_BERTH_ALLOCATION,
  props<{ berthAllocation: Omit<IBerthAllocation, 'id'> }>()
);

export const createBerthAllocationSuccess = createAction(
  BerthAllocationActionTypes.CREATE_BERTH_ALLOCATION_SUCCESS,
  props<{ berthAllocation: IBerthAllocation }>()
);

export const createBerthAllocationFailure = createAction(
  BerthAllocationActionTypes.CREATE_BERTH_ALLOCATION_FAILURE,
  props<{ error: string }>()
);

// Update Actions
export const updateBerthAllocation = createAction(
  BerthAllocationActionTypes.UPDATE_BERTH_ALLOCATION,
  props<{ id: number; changes: Partial<IBerthAllocation> }>()
);

export const updateBerthAllocationSuccess = createAction(
  BerthAllocationActionTypes.UPDATE_BERTH_ALLOCATION_SUCCESS,
  props<{ berthAllocation: IBerthAllocation }>()
);

export const updateBerthAllocationFailure = createAction(
  BerthAllocationActionTypes.UPDATE_BERTH_ALLOCATION_FAILURE,
  props<{ error: string }>()
);

// Delete Actions
export const deleteBerthAllocation = createAction(
  BerthAllocationActionTypes.DELETE_BERTH_ALLOCATION,
  props<{ id: number }>()
);

export const deleteBerthAllocationSuccess = createAction(
  BerthAllocationActionTypes.DELETE_BERTH_ALLOCATION_SUCCESS,
  props<{ id: number }>()
);

export const deleteBerthAllocationFailure = createAction(
  BerthAllocationActionTypes.DELETE_BERTH_ALLOCATION_FAILURE,
  props<{ error: string }>()
);

// Status Update Action
export const updateBerthAllocationStatus = createAction(
  BerthAllocationActionTypes.UPDATE_BERTH_ALLOCATION_STATUS,
  props<{ id: number; status: string }>()
);

// WebSocket Sync Action
export const syncBerthAllocation = createAction(
  BerthAllocationActionTypes.SYNC_BERTH_ALLOCATION,
  props<{ berthAllocation: IBerthAllocation }>()
);

// Utility Actions
export const clearBerthAllocationError = createAction(
  BerthAllocationActionTypes.CLEAR_BERTH_ALLOCATION_ERROR
);

export const resetBerthAllocationState = createAction(
  BerthAllocationActionTypes.RESET_BERTH_ALLOCATION_STATE
);