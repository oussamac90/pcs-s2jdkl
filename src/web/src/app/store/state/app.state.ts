import { VesselCall, VesselCallStatus } from '../../shared/models/vessel-call.model';
import { IBerthAllocation, BerthAllocationStatus } from '../../shared/models/berth-allocation.model';
import { ServiceBooking, ServiceStatus } from '../../shared/models/service-booking.model';
import { Clearance, ClearanceStatus } from '../../shared/models/clearance.model';

/**
 * Interface defining the state structure for vessel calls with enhanced tracking
 * and filtering capabilities.
 */
export interface VesselCallState {
  entities: { [id: number]: VesselCall };
  ids: number[];
  loading: boolean;
  error: string | null;
  selectedId: number | null;
  lastUpdated: Date | null;
  filterCriteria: {
    status?: VesselCallStatus;
    dateRange?: {
      start: Date;
      end: Date;
    };
  };
  sortCriteria: {
    field: string;
    direction: 'asc' | 'desc';
  };
}

/**
 * Interface defining the state structure for berth allocations with enhanced tracking
 * and filtering capabilities.
 */
export interface BerthAllocationState {
  entities: { [id: number]: IBerthAllocation };
  ids: number[];
  loading: boolean;
  error: string | null;
  selectedId: number | null;
  lastUpdated: Date | null;
  filterCriteria: {
    status?: BerthAllocationStatus;
    berthId?: number;
  };
  sortCriteria: {
    field: string;
    direction: 'asc' | 'desc';
  };
}

/**
 * Interface defining the state structure for service bookings with enhanced tracking
 * and filtering capabilities.
 */
export interface ServiceBookingState {
  entities: { [id: number]: ServiceBooking };
  ids: number[];
  loading: boolean;
  error: string | null;
  selectedId: number | null;
  lastUpdated: Date | null;
  filterCriteria: {
    status?: ServiceStatus;
    serviceType?: string;
  };
  sortCriteria: {
    field: string;
    direction: 'asc' | 'desc';
  };
}

/**
 * Interface defining the state structure for clearances with enhanced tracking
 * and filtering capabilities.
 */
export interface ClearanceState {
  entities: { [id: number]: Clearance };
  ids: number[];
  loading: boolean;
  error: string | null;
  selectedId: number | null;
  lastUpdated: Date | null;
  filterCriteria: {
    status?: ClearanceStatus;
    type?: string;
  };
  sortCriteria: {
    field: string;
    direction: 'asc' | 'desc';
  };
}

/**
 * Root state interface for the NgRx store.
 * Combines all feature states into a single, cohesive state tree.
 */
export interface AppState {
  vesselCalls: VesselCallState;
  berthAllocations: BerthAllocationState;
  serviceBookings: ServiceBookingState;
  clearances: ClearanceState;
}

/**
 * Initial state configuration for the NgRx store.
 * Provides default values for all state properties.
 */
export const initialState: AppState = {
  vesselCalls: {
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
  },
  berthAllocations: {
    entities: {},
    ids: [],
    loading: false,
    error: null,
    selectedId: null,
    lastUpdated: null,
    filterCriteria: {},
    sortCriteria: {
      field: 'startTime',
      direction: 'asc'
    }
  },
  serviceBookings: {
    entities: {},
    ids: [],
    loading: false,
    error: null,
    selectedId: null,
    lastUpdated: null,
    filterCriteria: {},
    sortCriteria: {
      field: 'serviceTime',
      direction: 'asc'
    }
  },
  clearances: {
    entities: {},
    ids: [],
    loading: false,
    error: null,
    selectedId: null,
    lastUpdated: null,
    filterCriteria: {},
    sortCriteria: {
      field: 'submittedAt',
      direction: 'desc'
    }
  }
};