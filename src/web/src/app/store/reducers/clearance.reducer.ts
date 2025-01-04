import { createReducer, on } from '@ngrx/store';
import { EntityAdapter, createEntityAdapter } from '@ngrx/entity';
import { Clearance, ClearanceStatus } from '../../shared/models/clearance.model';
import { ClearanceState } from '../state/app.state';
import * as ClearanceActions from '../actions/clearance.actions';

/**
 * Entity adapter for managing normalized clearance data
 */
export const clearanceAdapter: EntityAdapter<Clearance> = createEntityAdapter<Clearance>({
  selectId: (clearance: Clearance) => clearance.id,
  sortComparer: (a: Clearance, b: Clearance) => b.updatedAt.getTime() - a.updatedAt.getTime()
});

/**
 * Initial state for the clearance feature
 */
export const initialState: ClearanceState = clearanceAdapter.getInitialState({
  loading: false,
  error: null,
  selectedId: null,
  lastUpdated: null,
  filterCriteria: {
    status: null,
    type: null
  },
  sortCriteria: {
    field: 'updatedAt',
    direction: 'desc'
  }
});

/**
 * Clearance reducer with comprehensive state management
 */
export const clearanceReducer = createReducer(
  initialState,

  // Load clearances
  on(ClearanceActions.loadClearances, (state) => ({
    ...state,
    loading: true,
    error: null
  })),

  on(ClearanceActions.loadClearancesSuccess, (state, { clearances }) => {
    const updatedState = clearanceAdapter.setAll(clearances, {
      ...state,
      loading: false,
      error: null,
      lastUpdated: new Date()
    });
    return updatedState;
  }),

  on(ClearanceActions.loadClearancesFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error: error.message
  })),

  // Load single clearance
  on(ClearanceActions.loadClearance, (state) => ({
    ...state,
    loading: true,
    error: null
  })),

  on(ClearanceActions.loadClearanceSuccess, (state, { clearance }) => {
    const updatedState = clearanceAdapter.upsertOne(clearance, {
      ...state,
      loading: false,
      error: null,
      selectedId: clearance.id,
      lastUpdated: new Date()
    });
    return updatedState;
  }),

  on(ClearanceActions.loadClearanceFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error: error.message
  })),

  // Update clearance status with optimistic update
  on(ClearanceActions.updateClearanceStatus, (state, { id, status }) => {
    const clearance = state.entities[id];
    if (!clearance) return state;

    const updatedClearance: Clearance = {
      ...clearance,
      status,
      updatedAt: new Date()
    };

    return clearanceAdapter.updateOne(
      { id, changes: updatedClearance },
      {
        ...state,
        lastUpdated: new Date()
      }
    );
  }),

  on(ClearanceActions.updateClearanceStatusSuccess, (state, { clearance }) => 
    clearanceAdapter.updateOne(
      { id: clearance.id, changes: clearance },
      {
        ...state,
        error: null,
        lastUpdated: new Date()
      }
    )
  ),

  on(ClearanceActions.updateClearanceStatusFailure, (state, { error }) => {
    // Revert optimistic update by reloading the state
    return {
      ...state,
      error: error.message,
      loading: true // Trigger reload
    };
  }),

  // Submit new clearance with optimistic update
  on(ClearanceActions.submitClearance, (state, { vesselCallId, type, submittedBy }) => {
    const tempId = Date.now(); // Temporary ID for optimistic update
    const tempClearance: Clearance = {
      id: tempId,
      vesselCallId,
      type,
      status: ClearanceStatus.PENDING,
      submittedBy,
      submittedAt: new Date(),
      createdAt: new Date(),
      updatedAt: new Date(),
      approvedBy: null,
      approvedAt: null,
      validUntil: null,
      remarks: null,
      referenceNumber: `TEMP-${tempId}`,
      vesselName: 'Loading...' // Will be updated with real data
    };

    return clearanceAdapter.addOne(tempClearance, {
      ...state,
      lastUpdated: new Date()
    });
  }),

  on(ClearanceActions.submitClearanceSuccess, (state, { clearance }) => {
    // Remove temporary entry and add real one
    const updatedState = clearanceAdapter.removeOne(
      state.ids[state.ids.length - 1] as number,
      state
    );
    return clearanceAdapter.addOne(clearance, {
      ...updatedState,
      error: null,
      lastUpdated: new Date()
    });
  }),

  on(ClearanceActions.submitClearanceFailure, (state, { error }) => {
    // Remove temporary entry on failure
    return clearanceAdapter.removeOne(
      state.ids[state.ids.length - 1] as number,
      {
        ...state,
        error: error.message
      }
    );
  }),

  // Filter and sort operations
  on(ClearanceActions.setFilterCriteria, (state, { criteria }) => ({
    ...state,
    filterCriteria: criteria,
    lastUpdated: new Date()
  })),

  on(ClearanceActions.setSortCriteria, (state, { criteria }) => ({
    ...state,
    sortCriteria: criteria,
    lastUpdated: new Date()
  }))
);

// Export the reducer as default
export default clearanceReducer;

// Selectors
export const {
  selectIds,
  selectEntities,
  selectAll,
  selectTotal
} = clearanceAdapter.getSelectors();