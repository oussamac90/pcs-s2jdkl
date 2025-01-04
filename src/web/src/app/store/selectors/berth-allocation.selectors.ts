import { createFeatureSelector, createSelector } from '@ngrx/store';
import { AppState } from '../state/app.state';
import { IBerthAllocation, BerthAllocationStatus } from '../../shared/models/berth-allocation.model';

/**
 * Feature selector for accessing the berth allocation state slice.
 * Provides type-safe access to the entire berth allocation feature state.
 */
export const selectBerthAllocationState = createFeatureSelector<AppState, AppState['berthAllocations']>('berthAllocations');

/**
 * Selector for retrieving all berth allocation entities.
 * Provides memoized access to the complete list of berth allocations.
 */
export const selectAllBerthAllocations = createSelector(
  selectBerthAllocationState,
  state => Object.values(state.entities)
);

/**
 * Selector for retrieving the loading state of berth allocations.
 * Used to show loading indicators in the UI.
 */
export const selectBerthAllocationsLoading = createSelector(
  selectBerthAllocationState,
  state => state.loading
);

/**
 * Selector for retrieving any error state in berth allocations.
 * Used for error handling and display in the UI.
 */
export const selectBerthAllocationsError = createSelector(
  selectBerthAllocationState,
  state => state.error
);

/**
 * Selector for retrieving the currently selected berth allocation.
 * Returns the full berth allocation entity for the selected ID.
 */
export const selectSelectedBerthAllocation = createSelector(
  selectBerthAllocationState,
  state => state.selectedId ? state.entities[state.selectedId] : null
);

/**
 * Selector for retrieving berth allocations filtered by time range.
 * @param startTime - Start of the time range
 * @param endTime - End of the time range
 * Returns memoized list of allocations within the specified time range.
 */
export const selectBerthAllocationsByTimeRange = createSelector(
  selectAllBerthAllocations,
  (allocations: IBerthAllocation[], props: { startTime: Date; endTime: Date }) => {
    const start = new Date(props.startTime);
    const end = new Date(props.endTime);
    return allocations.filter(allocation => {
      const allocationStart = new Date(allocation.startTime);
      const allocationEnd = new Date(allocation.endTime);
      return allocationStart >= start && allocationEnd <= end;
    });
  }
);

/**
 * Selector for retrieving berth allocations by status.
 * @param status - The status to filter by
 * Returns memoized list of allocations matching the specified status.
 */
export const selectBerthAllocationsByStatus = createSelector(
  selectAllBerthAllocations,
  (allocations: IBerthAllocation[], props: { status: BerthAllocationStatus }) =>
    allocations.filter(allocation => allocation.status === props.status)
);

/**
 * Selector for retrieving berth allocations by berth ID.
 * @param berthId - The berth ID to filter by
 * Returns memoized list of allocations for the specified berth.
 */
export const selectBerthAllocationsByBerthId = createSelector(
  selectAllBerthAllocations,
  (allocations: IBerthAllocation[], props: { berthId: number }) =>
    allocations.filter(allocation => allocation.berthId === props.berthId)
);

/**
 * Selector for retrieving berth allocations by vessel call ID.
 * @param vesselCallId - The vessel call ID to filter by
 * Returns memoized list of allocations for the specified vessel call.
 */
export const selectBerthAllocationsByVesselCallId = createSelector(
  selectAllBerthAllocations,
  (allocations: IBerthAllocation[], props: { vesselCallId: number }) =>
    allocations.filter(allocation => allocation.vesselCallId === props.vesselCallId)
);

/**
 * Selector for retrieving the last updated timestamp of berth allocations.
 * Used for cache invalidation and refresh logic.
 */
export const selectBerthAllocationsLastUpdated = createSelector(
  selectBerthAllocationState,
  state => state.lastUpdated
);

/**
 * Selector for retrieving current filter criteria for berth allocations.
 * Used for maintaining filter state in the UI.
 */
export const selectBerthAllocationsFilterCriteria = createSelector(
  selectBerthAllocationState,
  state => state.filterCriteria
);

/**
 * Selector for retrieving current sort criteria for berth allocations.
 * Used for maintaining sort state in the UI.
 */
export const selectBerthAllocationsSortCriteria = createSelector(
  selectBerthAllocationState,
  state => state.sortCriteria
);