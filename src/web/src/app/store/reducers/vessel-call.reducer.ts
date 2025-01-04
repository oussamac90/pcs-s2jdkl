import { createReducer, on } from '@ngrx/store';
import { VesselCall, VesselCallStatus } from '../../shared/models/vessel-call.model';
import { VesselCallState } from '../state/app.state';
import * as VesselCallActions from '../actions/vessel-call.actions';

/**
 * Initial state for vessel call management
 */
const initialState: VesselCallState = {
    entities: {},
    ids: [],
    loading: false,
    error: null,
    selectedId: null,
    lastUpdated: null,
    filterCriteria: {},
    sortCriteria: {
        field: 'eta',
        direction: 'asc'
    }
};

/**
 * Enhanced reducer for vessel call state management with optimistic updates
 * and real-time synchronization support
 */
export const vesselCallReducer = createReducer(
    initialState,

    // Load Vessel Calls
    on(VesselCallActions.loadVesselCalls, (state) => ({
        ...state,
        loading: true,
        error: null
    })),

    on(VesselCallActions.loadVesselCallsSuccess, (state, { vesselCalls, totalCount, page }) => {
        const entities = vesselCalls.reduce((acc, vesselCall) => ({
            ...acc,
            [vesselCall.id]: vesselCall
        }), {});

        return {
            ...state,
            entities,
            ids: vesselCalls.map(call => call.id),
            loading: false,
            error: null,
            lastUpdated: new Date()
        };
    }),

    on(VesselCallActions.loadVesselCallsFailure, (state, { error }) => ({
        ...state,
        loading: false,
        error: {
            message: error.message,
            code: error.code,
            type: error.type,
            details: error.details,
            timestamp: new Date()
        }
    })),

    // Create Vessel Call
    on(VesselCallActions.createVesselCallSuccess, (state, { vesselCall }) => ({
        ...state,
        entities: {
            ...state.entities,
            [vesselCall.id]: vesselCall
        },
        ids: [...state.ids, vesselCall.id],
        lastUpdated: new Date()
    })),

    // Update Vessel Call with Optimistic Updates
    on(VesselCallActions.updateVesselCall, (state, { id, changes, optimistic }) => {
        if (!optimistic) return state;

        const currentEntity = state.entities[id];
        const updatedEntity = { ...currentEntity, ...changes };

        return {
            ...state,
            entities: {
                ...state.entities,
                [id]: updatedEntity
            }
        };
    }),

    on(VesselCallActions.updateVesselCallSuccess, (state, { id, changes, timestamp }) => ({
        ...state,
        entities: {
            ...state.entities,
            [id]: {
                ...state.entities[id],
                ...changes,
                updatedAt: timestamp
            }
        },
        lastUpdated: timestamp
    })),

    on(VesselCallActions.updateVesselCallFailure, (state, { id, error, revertChanges }) => ({
        ...state,
        entities: {
            ...state.entities,
            [id]: {
                ...state.entities[id],
                ...revertChanges
            }
        },
        error: {
            message: error.message,
            code: error.code,
            type: error.type,
            details: error.details,
            timestamp: new Date()
        }
    })),

    // Delete Vessel Call
    on(VesselCallActions.deleteVesselCallSuccess, (state, { id }) => {
        const { [id]: removed, ...entities } = state.entities;
        return {
            ...state,
            entities,
            ids: state.ids.filter(callId => callId !== id),
            selectedId: state.selectedId === id ? null : state.selectedId,
            lastUpdated: new Date()
        };
    }),

    // Selection Management
    on(VesselCallActions.selectVesselCall, (state, { id }) => ({
        ...state,
        selectedId: id
    })),

    on(VesselCallActions.clearSelectedVesselCall, (state) => ({
        ...state,
        selectedId: null
    })),

    // Real-time Synchronization
    on(VesselCallActions.syncVesselCallStatus, (state, { id, status, timestamp }) => {
        const currentEntity = state.entities[id];
        if (!currentEntity) return state;

        return {
            ...state,
            entities: {
                ...state.entities,
                [id]: {
                    ...currentEntity,
                    status,
                    updatedAt: timestamp
                }
            },
            lastUpdated: timestamp
        };
    }),

    // Batch Updates
    on(VesselCallActions.batchUpdateVesselCalls, (state, { updates }) => {
        const updatedEntities = updates.reduce((acc, update) => ({
            ...acc,
            [update.id]: {
                ...state.entities[update.id],
                ...update.changes
            }
        }), {});

        return {
            ...state,
            entities: {
                ...state.entities,
                ...updatedEntities
            },
            lastUpdated: new Date()
        };
    })
);