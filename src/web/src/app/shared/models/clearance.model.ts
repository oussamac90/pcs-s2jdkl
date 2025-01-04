import { ApiResponse } from './api-response.model';

/**
 * Enumeration of all possible clearance types in the vessel call management system.
 * Used for strict type validation of clearance requirements.
 */
export enum ClearanceType {
  /** Customs authority clearance for cargo and vessel */
  CUSTOMS = 'CUSTOMS',
  
  /** Immigration authority clearance for crew members */
  IMMIGRATION = 'IMMIGRATION',
  
  /** Port authority operational clearance */
  PORT_AUTHORITY = 'PORT_AUTHORITY',
  
  /** Health/quarantine authority clearance */
  HEALTH = 'HEALTH',
  
  /** Maritime security clearance */
  SECURITY = 'SECURITY'
}

/**
 * Enumeration of all possible clearance statuses in the workflow.
 * Represents the current state of a clearance request.
 */
export enum ClearanceStatus {
  /** Initial status when clearance is submitted */
  PENDING = 'PENDING',
  
  /** Clearance is being reviewed by authorities */
  IN_PROGRESS = 'IN_PROGRESS',
  
  /** Clearance has been granted */
  APPROVED = 'APPROVED',
  
  /** Clearance has been denied */
  REJECTED = 'REJECTED',
  
  /** Clearance request has been cancelled */
  CANCELLED = 'CANCELLED'
}

/**
 * Interface defining the structure of vessel clearance data.
 * Provides comprehensive type safety and validation support for clearance operations.
 */
export interface Clearance {
  /** Unique identifier for the clearance record */
  readonly id: number;
  
  /** Reference to the associated vessel call */
  readonly vesselCallId: number;
  
  /** Name of the vessel requiring clearance */
  readonly vesselName: string;
  
  /** Type of clearance being requested/processed */
  readonly type: ClearanceType;
  
  /** Current status of the clearance request */
  readonly status: ClearanceStatus;
  
  /** Unique reference number for the clearance request */
  readonly referenceNumber: string;
  
  /** Username/ID of the person who submitted the clearance request */
  readonly submittedBy: string;
  
  /** Username/ID of the person who approved/rejected the clearance */
  readonly approvedBy: string | null;
  
  /** Optional remarks or notes about the clearance */
  readonly remarks: string | null;
  
  /** Timestamp when the clearance was submitted */
  readonly submittedAt: Date;
  
  /** Timestamp when the clearance was approved/rejected */
  readonly approvedAt: Date | null;
  
  /** Expiration date of the clearance if applicable */
  readonly validUntil: Date | null;
  
  /** Record creation timestamp */
  readonly createdAt: Date;
  
  /** Last update timestamp */
  readonly updatedAt: Date;
}

/**
 * Type definition for API responses containing clearance data.
 * Provides type safety for clearance-related API communications.
 */
export type ClearanceResponse = ApiResponse<Clearance>;

/**
 * Type definition for API responses containing arrays of clearance data.
 * Used for list operations with multiple clearance records.
 */
export type ClearanceListResponse = ApiResponse<Clearance[]>;