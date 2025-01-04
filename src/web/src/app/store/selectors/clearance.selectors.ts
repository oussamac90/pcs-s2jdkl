import { createFeatureSelector, createSelector } from '@ngrx/store';
import { AppState } from '../state/app.state';
import { Clearance, ClearanceStatus } from '../../shared/models/clearance.model';

/**
 * Feature selector for accessing the clearance state slice.
 * Provides type-safe access to the clearance feature state.
 */
export const selectClearanceState = createFeatureSelector<AppState, AppState['clearances']>('clearances');

/**
 * Selector for accessing all clearance entities.
 * Returns a dictionary of clearance entities indexed by ID.
 */
export const selectClearanceEntities = createSelector(
  selectClearanceState,
  state => state.entities
);

/**
 * Selector for accessing all clearance IDs.
 * Returns an array of clearance IDs in the store.
 */
export const selectClearanceIds = createSelector(
  selectClearanceState,
  state => state.ids
);

/**
 * Selector for accessing the loading state of clearance operations.
 * Used to show/hide loading indicators in the UI.
 */
export const selectClearanceLoading = createSelector(
  selectClearanceState,
  state => state.loading
);

/**
 * Selector for accessing any error state in clearance operations.
 * Used for error handling and display in the UI.
 */
export const selectClearanceError = createSelector(
  selectClearanceState,
  state => state.error
);

/**
 * Selector for accessing the currently selected clearance ID.
 * Used for highlighting or focusing on a specific clearance.
 */
export const selectSelectedClearanceId = createSelector(
  selectClearanceState,
  state => state.selectedId
);

/**
 * Selector for accessing all clearance entities as an array.
 * Transforms the entities dictionary into a sorted array.
 */
export const selectAllClearances = createSelector(
  selectClearanceEntities,
  selectClearanceIds,
  (entities, ids): Clearance[] => ids.map(id => entities[id])
);

/**
 * Selector for accessing the currently selected clearance entity.
 * Returns the full clearance object for the selected ID.
 */
export const selectSelectedClearance = createSelector(
  selectClearanceEntities,
  selectSelectedClearanceId,
  (entities, selectedId) => selectedId ? entities[selectedId] : null
);

/**
 * Selector for filtering clearances by their current status.
 * Returns an array of clearances matching the specified status.
 * @param status The clearance status to filter by
 */
export const selectClearancesByStatus = (status: ClearanceStatus) => createSelector(
  selectAllClearances,
  (clearances): Clearance[] => clearances.filter(clearance => clearance.status === status)
);

/**
 * Selector for getting clearances sorted by submission date.
 * Returns an array of clearances sorted by submittedAt timestamp.
 * @param ascending Sort direction (true for ascending, false for descending)
 */
export const selectClearancesSortedByDate = (ascending: boolean = true) => createSelector(
  selectAllClearances,
  (clearances): Clearance[] => [...clearances].sort((a, b) => {
    const comparison = a.submittedAt.getTime() - b.submittedAt.getTime();
    return ascending ? comparison : -comparison;
  })
);

/**
 * Selector for getting clearances related to a specific vessel.
 * Returns an array of clearances filtered by vessel ID.
 * @param vesselId The ID of the vessel to filter by
 */
export const selectClearancesByVessel = (vesselId: number) => createSelector(
  selectAllClearances,
  (clearances): Clearance[] => clearances.filter(clearance => clearance.vesselCallId === vesselId)
);

/**
 * Selector for getting the last updated timestamp of the clearance state.
 * Used for cache invalidation and refresh logic.
 */
export const selectClearanceLastUpdated = createSelector(
  selectClearanceState,
  state => state.lastUpdated
);

/**
 * Selector for getting pending clearances that require attention.
 * Returns an array of clearances with PENDING status.
 */
export const selectPendingClearances = createSelector(
  selectAllClearances,
  (clearances): Clearance[] => clearances.filter(clearance => clearance.status === ClearanceStatus.PENDING)
);

/**
 * Selector for getting clearances that have been approved.
 * Returns an array of clearances with APPROVED status.
 */
export const selectApprovedClearances = createSelector(
  selectAllClearances,
  (clearances): Clearance[] => clearances.filter(clearance => clearance.status === ClearanceStatus.APPROVED)
);

/**
 * Selector for getting rejected clearances.
 * Returns an array of clearances with REJECTED status.
 */
export const selectRejectedClearances = createSelector(
  selectAllClearances,
  (clearances): Clearance[] => clearances.filter(clearance => clearance.status === ClearanceStatus.REJECTED)
);