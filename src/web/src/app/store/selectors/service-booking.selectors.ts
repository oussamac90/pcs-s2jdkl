import { createFeatureSelector, createSelector, MemoizedSelector } from '@ngrx/store';
import { AppState } from '../state/app.state';
import { ServiceBooking, ServiceType, ServiceStatus } from '../../shared/models/service-booking.model';

/**
 * Feature selector for accessing the service booking state slice.
 * Provides type-safe access to the service booking feature state.
 */
export const selectServiceBookingState = createFeatureSelector<AppState, ServiceBookingState>('serviceBookings');

/**
 * Selector for retrieving all service booking entities.
 * Returns a memoized array of all service bookings.
 */
export const selectAllServiceBookings = createSelector(
  selectServiceBookingState,
  (state) => state.ids.map(id => state.entities[id])
);

/**
 * Selector for retrieving the loading state of service bookings.
 * Used to show loading indicators in the UI.
 */
export const selectServiceBookingsLoading = createSelector(
  selectServiceBookingState,
  (state) => state.loading
);

/**
 * Selector for retrieving any error state related to service bookings.
 * Used for error handling and display in the UI.
 */
export const selectServiceBookingsError = createSelector(
  selectServiceBookingState,
  (state) => state.error
);

/**
 * Selector for retrieving the currently selected service booking.
 * Returns null if no booking is selected.
 */
export const selectSelectedServiceBooking = createSelector(
  selectServiceBookingState,
  (state) => state.selectedId ? state.entities[state.selectedId] : null
);

/**
 * Selector for filtering service bookings by type.
 * Returns a memoized array of bookings filtered by the specified service type.
 */
export const selectServiceBookingsByType = (type: ServiceType): MemoizedSelector<AppState, ServiceBooking[]> => 
  createSelector(
    selectAllServiceBookings,
    (bookings) => bookings.filter(booking => booking.serviceType === type)
  );

/**
 * Selector for retrieving pending service bookings.
 * Returns a memoized array of bookings with REQUESTED status, sorted by service time.
 */
export const selectPendingServiceBookings = createSelector(
  selectAllServiceBookings,
  (bookings) => bookings
    .filter(booking => booking.status === ServiceStatus.REQUESTED)
    .sort((a, b) => new Date(a.serviceTime).getTime() - new Date(b.serviceTime).getTime())
);

/**
 * Selector for retrieving confirmed service bookings.
 * Returns a memoized array of bookings with CONFIRMED status.
 */
export const selectConfirmedServiceBookings = createSelector(
  selectAllServiceBookings,
  (bookings) => bookings.filter(booking => booking.status === ServiceStatus.CONFIRMED)
);

/**
 * Selector for retrieving in-progress service bookings.
 * Returns a memoized array of bookings with IN_PROGRESS status.
 */
export const selectInProgressServiceBookings = createSelector(
  selectAllServiceBookings,
  (bookings) => bookings.filter(booking => booking.status === ServiceStatus.IN_PROGRESS)
);

/**
 * Selector for retrieving service bookings by vessel call ID.
 * Returns a memoized array of bookings for a specific vessel call.
 */
export const selectServiceBookingsByVesselCall = (vesselCallId: number): MemoizedSelector<AppState, ServiceBooking[]> =>
  createSelector(
    selectAllServiceBookings,
    (bookings) => bookings.filter(booking => booking.vesselCallId === vesselCallId)
  );

/**
 * Selector for retrieving the last updated timestamp of service bookings.
 * Used for cache invalidation and refresh logic.
 */
export const selectServiceBookingsLastUpdated = createSelector(
  selectServiceBookingState,
  (state) => state.lastUpdated
);

/**
 * Selector for retrieving filtered service bookings based on current filter criteria.
 * Applies both status and service type filters if present.
 */
export const selectFilteredServiceBookings = createSelector(
  selectAllServiceBookings,
  selectServiceBookingState,
  (bookings, state) => {
    let filtered = [...bookings];
    
    if (state.filterCriteria.status) {
      filtered = filtered.filter(booking => booking.status === state.filterCriteria.status);
    }
    
    if (state.filterCriteria.serviceType) {
      filtered = filtered.filter(booking => booking.serviceType === state.filterCriteria.serviceType);
    }
    
    return filtered;
  }
);