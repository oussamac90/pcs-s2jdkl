import { ApiResponse } from './api-response.model';

/**
 * Enum defining all available port service types.
 * Supports comprehensive service management including pilotage, tugboat,
 * mooring, and explicit unmooring services.
 */
export enum ServiceType {
  /** Pilotage services for vessel navigation */
  PILOTAGE = 'PILOTAGE',
  
  /** Tugboat assistance services */
  TUGBOAT = 'TUGBOAT',
  
  /** Mooring services for securing vessels */
  MOORING = 'MOORING',
  
  /** Explicit unmooring services for vessel departure */
  UNMOORING = 'UNMOORING'
}

/**
 * Enum defining all possible service booking statuses.
 * Provides comprehensive status tracking from request to completion.
 */
export enum ServiceStatus {
  /** Initial status when service is requested */
  REQUESTED = 'REQUESTED',
  
  /** Status when service provider confirms the booking */
  CONFIRMED = 'CONFIRMED',
  
  /** Status when service delivery has started */
  IN_PROGRESS = 'IN_PROGRESS',
  
  /** Status when service has been successfully delivered */
  COMPLETED = 'COMPLETED',
  
  /** Status when service booking is cancelled */
  CANCELLED = 'CANCELLED'
}

/**
 * Interface defining the complete structure of a port service booking.
 * Provides comprehensive tracking and audit capabilities for all service types.
 */
export interface ServiceBooking {
  /** Unique identifier for the service booking */
  readonly id: number;
  
  /** Reference to the associated vessel call */
  readonly vesselCallId: number;
  
  /** Name of the vessel requesting the service */
  readonly vesselName: string;
  
  /** Type of port service being booked */
  readonly serviceType: ServiceType;
  
  /** Current status of the service booking */
  readonly status: ServiceStatus;
  
  /** Quantity of service units required (e.g., number of tugboats) */
  readonly quantity: number;
  
  /** Scheduled time for service delivery in ISO 8601 format */
  readonly serviceTime: string;
  
  /** Additional notes or special requirements for the service */
  readonly remarks: string;
  
  /** Timestamp of booking creation in ISO 8601 format */
  readonly createdAt: string;
  
  /** Timestamp of last booking update in ISO 8601 format */
  readonly updatedAt: string;
}

/**
 * Type definition for API responses containing service booking data.
 * Ensures type safety when handling service booking API responses.
 */
export type ServiceBookingResponse = ApiResponse<ServiceBooking>;

/**
 * Type definition for API responses containing multiple service bookings.
 * Ensures type safety when handling lists of service bookings.
 */
export type ServiceBookingListResponse = ApiResponse<ServiceBooking[]>;