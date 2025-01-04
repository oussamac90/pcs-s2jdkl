import { ApiResponse } from '../models/api-response.model';

/**
 * Enum representing all possible states of a berth allocation.
 * Used for type-safe status tracking throughout the berth management workflow.
 */
export enum BerthAllocationStatus {
  /** Initial state when berth is allocated but vessel hasn't arrived */
  SCHEDULED = 'SCHEDULED',
  
  /** Active state when vessel is currently at berth */
  OCCUPIED = 'OCCUPIED',
  
  /** Terminal state when vessel has departed and allocation is finished */
  COMPLETED = 'COMPLETED',
  
  /** Terminal state when allocation was cancelled before completion */
  CANCELLED = 'CANCELLED'
}

/**
 * Comprehensive interface defining the structure of berth allocation data.
 * Maps directly to backend BerthAllocationDTO for type-safe data transfer.
 */
export interface IBerthAllocation {
  /** Unique identifier for the berth allocation */
  readonly id: number;
  
  /** Reference to the associated vessel call */
  readonly vesselCallId: number;
  
  /** Display name of the vessel for UI purposes */
  readonly vesselName: string;
  
  /** Reference to the allocated berth */
  readonly berthId: number;
  
  /** Display name of the berth for UI purposes */
  readonly berthName: string;
  
  /** ISO 8601 timestamp for planned start of berth occupation */
  readonly startTime: string;
  
  /** ISO 8601 timestamp for planned end of berth occupation */
  readonly endTime: string;
  
  /** Current status of the berth allocation */
  readonly status: BerthAllocationStatus;
  
  /** ISO 8601 timestamp of record creation */
  readonly createdAt: string;
  
  /** ISO 8601 timestamp of last record update */
  readonly updatedAt: string;
}

/**
 * Type alias for API responses containing berth allocation data.
 * Provides type safety for API communication handling berth allocations.
 */
export type BerthAllocationResponse = ApiResponse<IBerthAllocation>;

/**
 * Type alias for API responses containing arrays of berth allocations.
 * Used for list operations and bulk data retrieval.
 */
export type BerthAllocationListResponse = ApiResponse<IBerthAllocation[]>;