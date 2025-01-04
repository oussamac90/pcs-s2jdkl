import { createAction, props } from '@ngrx/store';
import { HttpErrorResponse } from '@angular/common/http';
import { ServiceBooking, ServiceType, ServiceStatus } from '../../shared/models/service-booking.model';

// Feature identifier for service booking actions
export const SERVICE_BOOKING_FEATURE = '[Service Booking]';

// Action type constants
export const LOAD_SERVICE_BOOKINGS = `${SERVICE_BOOKING_FEATURE} Load Service Bookings`;
export const LOAD_SERVICE_BOOKINGS_SUCCESS = `${SERVICE_BOOKING_FEATURE} Load Service Bookings Success`;
export const LOAD_SERVICE_BOOKINGS_FAILURE = `${SERVICE_BOOKING_FEATURE} Load Service Bookings Failure`;
export const CREATE_SERVICE_BOOKING = `${SERVICE_BOOKING_FEATURE} Create Service Booking`;
export const CREATE_SERVICE_BOOKING_SUCCESS = `${SERVICE_BOOKING_FEATURE} Create Service Booking Success`;
export const CREATE_SERVICE_BOOKING_FAILURE = `${SERVICE_BOOKING_FEATURE} Create Service Booking Failure`;
export const UPDATE_SERVICE_BOOKING = `${SERVICE_BOOKING_FEATURE} Update Service Booking`;
export const UPDATE_SERVICE_BOOKING_SUCCESS = `${SERVICE_BOOKING_FEATURE} Update Service Booking Success`;
export const UPDATE_SERVICE_BOOKING_FAILURE = `${SERVICE_BOOKING_FEATURE} Update Service Booking Failure`;
export const DELETE_SERVICE_BOOKING = `${SERVICE_BOOKING_FEATURE} Delete Service Booking`;
export const DELETE_SERVICE_BOOKING_SUCCESS = `${SERVICE_BOOKING_FEATURE} Delete Service Booking Success`;
export const DELETE_SERVICE_BOOKING_FAILURE = `${SERVICE_BOOKING_FEATURE} Delete Service Booking Failure`;

// Filter interface for service booking queries
export interface ServiceBookingFilters {
  vesselCallId?: number;
  serviceType?: ServiceType;
  status?: ServiceStatus;
  startDate?: string;
  endDate?: string;
  page?: number;
  pageSize?: number;
}

// Load service bookings actions
export const loadServiceBookings = createAction(
  LOAD_SERVICE_BOOKINGS,
  props<{ filters?: ServiceBookingFilters }>()
);

export const loadServiceBookingsSuccess = createAction(
  LOAD_SERVICE_BOOKINGS_SUCCESS,
  props<{ 
    bookings: ServiceBooking[];
    totalItems?: number;
    totalPages?: number;
    timestamp: string;
  }>()
);

export const loadServiceBookingsFailure = createAction(
  LOAD_SERVICE_BOOKINGS_FAILURE,
  props<{ error: HttpErrorResponse }>()
);

// Create service booking actions
export const createServiceBooking = createAction(
  CREATE_SERVICE_BOOKING,
  props<{ 
    vesselCallId: number;
    serviceType: ServiceType;
    quantity: number;
    serviceTime: string;
    remarks?: string;
  }>()
);

export const createServiceBookingSuccess = createAction(
  CREATE_SERVICE_BOOKING_SUCCESS,
  props<{ 
    booking: ServiceBooking;
    timestamp: string;
  }>()
);

export const createServiceBookingFailure = createAction(
  CREATE_SERVICE_BOOKING_FAILURE,
  props<{ error: HttpErrorResponse }>()
);

// Update service booking actions
export const updateServiceBooking = createAction(
  UPDATE_SERVICE_BOOKING,
  props<{ 
    id: number;
    status?: ServiceStatus;
    quantity?: number;
    serviceTime?: string;
    remarks?: string;
  }>()
);

export const updateServiceBookingSuccess = createAction(
  UPDATE_SERVICE_BOOKING_SUCCESS,
  props<{ 
    booking: ServiceBooking;
    timestamp: string;
  }>()
);

export const updateServiceBookingFailure = createAction(
  UPDATE_SERVICE_BOOKING_FAILURE,
  props<{ error: HttpErrorResponse }>()
);

// Delete service booking actions
export const deleteServiceBooking = createAction(
  DELETE_SERVICE_BOOKING,
  props<{ id: number }>()
);

export const deleteServiceBookingSuccess = createAction(
  DELETE_SERVICE_BOOKING_SUCCESS,
  props<{ 
    id: number;
    timestamp: string;
  }>()
);

export const deleteServiceBookingFailure = createAction(
  DELETE_SERVICE_BOOKING_FAILURE,
  props<{ error: HttpErrorResponse }>()
);

// Error message constants
export const ERROR_MESSAGES = {
  LOAD_FAILED: 'Failed to load service bookings',
  CREATE_FAILED: 'Failed to create service booking',
  UPDATE_FAILED: 'Failed to update service booking',
  DELETE_FAILED: 'Failed to delete service booking'
} as const;

/**
 * Sanitizes error messages for safe display to users
 * @param error The HTTP error response to sanitize
 * @returns A sanitized error message string
 */
export function sanitizeError(error: HttpErrorResponse): string {
  // Log the original error for debugging
  console.error('Service Booking Error:', error);

  // Return a sanitized user-friendly message
  if (error.error?.message) {
    return error.error.message;
  }
  
  switch (error.status) {
    case 400:
      return 'Invalid service booking request';
    case 401:
      return 'Authentication required';
    case 403:
      return 'Not authorized to perform this action';
    case 404:
      return 'Service booking not found';
    case 409:
      return 'Service booking conflict detected';
    default:
      return 'An unexpected error occurred while processing the service booking';
  }
}