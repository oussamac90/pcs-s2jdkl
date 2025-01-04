import { createSelector } from '@ngrx/store';
import { AppState } from '../state/app.state';
import { VesselCall, VesselCallStatus } from '../../shared/models/vessel-call.model';

/**
 * Base selector to access the vessel call state slice.
 * Provides type-safe access to the vessel call feature state.
 */
export const selectVesselCallState = (state: AppState) => state.vesselCalls;

/**
 * Memoized selector to retrieve all vessel calls as a sorted array.
 * Sorts vessel calls by ETA for consistent display ordering.
 */
export const selectAllVesselCalls = createSelector(
  selectVesselCallState,
  (state) => {
    const vesselCalls = state.ids.map(id => state.entities[id]);
    return [...vesselCalls].sort((a, b) => 
      new Date(a.eta).getTime() - new Date(b.eta).getTime()
    );
  }
);

/**
 * Memoized selector factory to retrieve a specific vessel call by ID.
 * Returns undefined if the vessel call doesn't exist.
 * @param id The unique identifier of the vessel call
 */
export const selectVesselCallById = (id: number) => createSelector(
  selectVesselCallState,
  (state) => state.entities[id]
);

/**
 * Memoized selector to retrieve the loading state for vessel call operations.
 * Used to show loading indicators in the UI.
 */
export const selectVesselCallsLoading = createSelector(
  selectVesselCallState,
  (state) => state.loading
);

/**
 * Memoized selector to retrieve any error state for vessel call operations.
 * Returns null when no error exists.
 */
export const selectVesselCallError = createSelector(
  selectVesselCallState,
  (state) => state.error
);

/**
 * Memoized selector to retrieve the currently selected vessel call.
 * Returns null when no vessel call is selected.
 */
export const selectSelectedVesselCall = createSelector(
  selectVesselCallState,
  (state) => state.selectedId ? state.entities[state.selectedId] : null
);

/**
 * Memoized selector factory to retrieve vessel calls filtered by status.
 * Returns a sorted array of vessel calls matching the specified status.
 * @param status The vessel call status to filter by
 */
export const selectVesselCallsByStatus = (status: VesselCallStatus) => createSelector(
  selectAllVesselCalls,
  (vesselCalls) => vesselCalls.filter(call => call.status === status)
);

/**
 * Memoized selector to retrieve the last update timestamp for vessel calls.
 * Used for tracking data freshness and triggering updates.
 */
export const selectVesselCallLastUpdated = createSelector(
  selectVesselCallState,
  (state) => state.lastUpdated
);

/**
 * Memoized selector to retrieve vessel calls based on current filter criteria.
 * Supports complex filtering including status and date range filters.
 */
export const selectFilteredVesselCalls = createSelector(
  selectAllVesselCalls,
  selectVesselCallState,
  (vesselCalls, state) => {
    let filtered = [...vesselCalls];
    
    if (state.filterCriteria.status) {
      filtered = filtered.filter(call => call.status === state.filterCriteria.status);
    }
    
    if (state.filterCriteria.dateRange) {
      const { start, end } = state.filterCriteria.dateRange;
      filtered = filtered.filter(call => {
        const eta = new Date(call.eta);
        return eta >= start && eta <= end;
      });
    }
    
    return filtered;
  }
);

/**
 * Memoized selector to retrieve vessel calls sorted according to current criteria.
 * Supports dynamic sorting by different fields and directions.
 */
export const selectSortedVesselCalls = createSelector(
  selectFilteredVesselCalls,
  selectVesselCallState,
  (vesselCalls, state) => {
    const { field, direction } = state.sortCriteria;
    return [...vesselCalls].sort((a, b) => {
      const aValue = a[field];
      const bValue = b[field];
      const comparison = aValue < bValue ? -1 : aValue > bValue ? 1 : 0;
      return direction === 'asc' ? comparison : -comparison;
    });
  }
);