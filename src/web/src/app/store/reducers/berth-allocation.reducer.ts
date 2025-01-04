import { createReducer, on } from '@ngrx/store';
import { IBerthAllocation } from '../../shared/models/berth-allocation.model';
import { BerthAllocationState } from '../state/app.state';
import * as BerthAllocationActions from '../actions/berth-allocation.actions';

/**
 * Initial state for berth allocation management
 * Provides default values for all state properties with type safety
 */
export const initialState: BerthAllocationState = {
  entities: {},
  ids: [],
  loading: false,
  error: null,
  selectedId: null,
  lastUpdated: null,
  filterCriteria: {},
  sortCriteria: {
    field: 'startTime',
    direction: 'asc'
  }
};

/**
 * Enhanced reducer for managing berth allocation state
 * Implements comprehensive state management with optimistic updates and WebSocket sync
 */
export const berthAllocationReducer = createReducer(
  initialState,

  // Load Berth Allocations
  on(BerthAllocationActions.loadBerthAllocations, (state) => ({
    ...state,
    loading: true,
    error: null
  })),

  on(BerthAllocationActions.loadBerthAllocationsSuccess, (state, { berthAllocations, timestamp }) => {
    const entities: { [id: number]: IBerthAllocation } = {};
    const ids: number[] = [];

    berthAllocations.forEach(allocation => {
      entities[allocation.id] = allocation;
      ids.push(allocation.id);
    });

    return {
      ...state,
      entities,
      ids,
      loading: false,
      error: null,
      lastUpdated: timestamp
    };
  }),

  on(BerthAllocationActions.loadBerthAllocationsFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error: {
      message: error,
      code: 'LOAD_ERROR'
    }
  })),

  // Create Berth Allocation
  on(BerthAllocationActions.createBerthAllocation, state => ({
    ...state,
    loading: true,
    error: null
  })),

  on(BerthAllocationActions.createBerthAllocationSuccess, (state, { berthAllocation }) => ({
    ...state,
    entities: {
      ...state.entities,
      [berthAllocation.id]: berthAllocation
    },
    ids: [...state.ids, berthAllocation.id],
    loading: false,
    error: null,
    lastUpdated: new Date()
  })),

  on(BerthAllocationActions.createBerthAllocationFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error: {
      message: error,
      code: 'CREATE_ERROR'
    }
  })),

  // Update Berth Allocation
  on(BerthAllocationActions.updateBerthAllocation, (state, { id, changes }) => ({
    ...state,
    entities: {
      ...state.entities,
      [id]: { ...state.entities[id], ...changes }  // Optimistic update
    },
    loading: true,
    error: null
  })),

  on(BerthAllocationActions.updateBerthAllocationSuccess, (state, { berthAllocation }) => ({
    ...state,
    entities: {
      ...state.entities,
      [berthAllocation.id]: berthAllocation
    },
    loading: false,
    error: null,
    lastUpdated: new Date()
  })),

  on(BerthAllocationActions.updateBerthAllocationFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error: {
      message: error,
      code: 'UPDATE_ERROR'
    }
  })),

  // Delete Berth Allocation
  on(BerthAllocationActions.deleteBerthAllocation, (state, { id }) => ({
    ...state,
    loading: true,
    error: null
  })),

  on(BerthAllocationActions.deleteBerthAllocationSuccess, (state, { id }) => ({
    ...state,
    entities: Object.keys(state.entities).reduce((entities, key) => {
      if (Number(key) !== id) {
        entities[Number(key)] = state.entities[Number(key)];
      }
      return entities;
    }, {} as { [id: number]: IBerthAllocation }),
    ids: state.ids.filter(existingId => existingId !== id),
    loading: false,
    error: null,
    lastUpdated: new Date()
  })),

  on(BerthAllocationActions.deleteBerthAllocationFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error: {
      message: error,
      code: 'DELETE_ERROR'
    }
  })),

  // WebSocket Sync
  on(BerthAllocationActions.syncBerthAllocation, (state, { berthAllocation }) => ({
    ...state,
    entities: {
      ...state.entities,
      [berthAllocation.id]: berthAllocation
    },
    ids: state.ids.includes(berthAllocation.id) 
      ? state.ids 
      : [...state.ids, berthAllocation.id],
    lastUpdated: new Date()
  })),

  // Status Update
  on(BerthAllocationActions.updateBerthAllocationStatus, (state, { id, status }) => ({
    ...state,
    entities: {
      ...state.entities,
      [id]: {
        ...state.entities[id],
        status: status
      }
    },
    lastUpdated: new Date()
  })),

  // Clear Error
  on(BerthAllocationActions.clearBerthAllocationError, state => ({
    ...state,
    error: null
  })),

  // Reset State
  on(BerthAllocationActions.resetBerthAllocationState, () => ({
    ...initialState
  }))
);