import { createReducer, on } from '@ngrx/store';
import { EntityState, EntityAdapter, createEntityAdapter } from '@ngrx/entity';
import { ServiceBooking, ServiceType, ServiceStatus } from '../../shared/models/service-booking.model';
import * as ServiceBookingActions from '../actions/service-booking.actions';

/**
 * Interface defining the structure of service booking state
 * Extends EntityState for normalized entity management
 */
export interface ServiceBookingState extends EntityState<ServiceBooking> {
  loading: boolean;
  error: string | null;
  lastUpdated: string | null;
  auditLog: string[];
  totalItems: number;
  totalPages: number;
  currentPage: number;
}

/**
 * Entity adapter configuration for ServiceBooking
 * Provides CRUD operations optimization with proper sorting
 */
export const adapter: EntityAdapter<ServiceBooking> = createEntityAdapter<ServiceBooking>({
  selectId: (booking: ServiceBooking) => booking.id,
  sortComparer: (a: ServiceBooking, b: ServiceBooking) => {
    // Sort by service time, then by creation date
    const timeCompare = new Date(a.serviceTime).getTime() - new Date(b.serviceTime).getTime();
    if (timeCompare !== 0) return timeCompare;
    return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
  }
});

/**
 * Initial state configuration with audit capabilities
 */
export const initialState: ServiceBookingState = adapter.getInitialState({
  loading: false,
  error: null,
  lastUpdated: null,
  auditLog: [],
  totalItems: 0,
  totalPages: 0,
  currentPage: 0
});

/**
 * Helper function to add audit log entry
 */
const addAuditLogEntry = (state: ServiceBookingState, action: string, details: string): string[] => {
  const timestamp = new Date().toISOString();
  const logEntry = `${timestamp} - ${action}: ${details}`;
  return [...state.auditLog, logEntry];
};

/**
 * Service booking reducer with comprehensive error handling and audit logging
 */
export const serviceBookingReducer = createReducer(
  initialState,

  // Load service bookings
  on(ServiceBookingActions.loadServiceBookings, (state) => ({
    ...state,
    loading: true,
    error: null,
    auditLog: addAuditLogEntry(state, 'Load Requested', 'Fetching service bookings')
  })),

  on(ServiceBookingActions.loadServiceBookingsSuccess, (state, { bookings, totalItems, totalPages, timestamp }) => {
    const updatedState = adapter.setAll(bookings, {
      ...state,
      loading: false,
      error: null,
      lastUpdated: timestamp,
      totalItems: totalItems || 0,
      totalPages: totalPages || 0,
      auditLog: addAuditLogEntry(state, 'Load Success', `Loaded ${bookings.length} bookings`)
    });
    return updatedState;
  }),

  on(ServiceBookingActions.loadServiceBookingsFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error: ServiceBookingActions.sanitizeError(error),
    auditLog: addAuditLogEntry(state, 'Load Failed', ServiceBookingActions.sanitizeError(error))
  })),

  // Create service booking
  on(ServiceBookingActions.createServiceBooking, (state) => ({
    ...state,
    loading: true,
    error: null,
    auditLog: addAuditLogEntry(state, 'Create Requested', 'Creating new service booking')
  })),

  on(ServiceBookingActions.createServiceBookingSuccess, (state, { booking, timestamp }) => {
    const updatedState = adapter.addOne(booking, {
      ...state,
      loading: false,
      error: null,
      lastUpdated: timestamp,
      auditLog: addAuditLogEntry(state, 'Create Success', `Created booking ID: ${booking.id}`)
    });
    return updatedState;
  }),

  on(ServiceBookingActions.createServiceBookingFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error: ServiceBookingActions.sanitizeError(error),
    auditLog: addAuditLogEntry(state, 'Create Failed', ServiceBookingActions.sanitizeError(error))
  })),

  // Update service booking
  on(ServiceBookingActions.updateServiceBooking, (state) => ({
    ...state,
    loading: true,
    error: null,
    auditLog: addAuditLogEntry(state, 'Update Requested', 'Updating service booking')
  })),

  on(ServiceBookingActions.updateServiceBookingSuccess, (state, { booking, timestamp }) => {
    const updatedState = adapter.updateOne(
      { id: booking.id, changes: booking },
      {
        ...state,
        loading: false,
        error: null,
        lastUpdated: timestamp,
        auditLog: addAuditLogEntry(state, 'Update Success', `Updated booking ID: ${booking.id}`)
      }
    );
    return updatedState;
  }),

  on(ServiceBookingActions.updateServiceBookingFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error: ServiceBookingActions.sanitizeError(error),
    auditLog: addAuditLogEntry(state, 'Update Failed', ServiceBookingActions.sanitizeError(error))
  })),

  // Delete service booking
  on(ServiceBookingActions.deleteServiceBooking, (state) => ({
    ...state,
    loading: true,
    error: null,
    auditLog: addAuditLogEntry(state, 'Delete Requested', 'Deleting service booking')
  })),

  on(ServiceBookingActions.deleteServiceBookingSuccess, (state, { id, timestamp }) => {
    const updatedState = adapter.removeOne(id, {
      ...state,
      loading: false,
      error: null,
      lastUpdated: timestamp,
      auditLog: addAuditLogEntry(state, 'Delete Success', `Deleted booking ID: ${id}`)
    });
    return updatedState;
  }),

  on(ServiceBookingActions.deleteServiceBookingFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error: ServiceBookingActions.sanitizeError(error),
    auditLog: addAuditLogEntry(state, 'Delete Failed', ServiceBookingActions.sanitizeError(error))
  }))
);

// Export the entity adapter selectors
export const {
  selectIds,
  selectEntities,
  selectAll,
  selectTotal
} = adapter.getSelectors();