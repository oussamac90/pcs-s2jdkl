/**
 * Enumeration of all possible vessel call statuses.
 * Used for strict type checking and status management throughout the application.
 * 
 * @enum {string}
 */
export enum VesselCallStatus {
    /** Initial status when vessel call is scheduled */
    PLANNED = 'PLANNED',
    /** Vessel has arrived at port waters */
    ARRIVED = 'ARRIVED',
    /** Vessel is currently berthed */
    AT_BERTH = 'AT_BERTH',
    /** Vessel has departed from port */
    DEPARTED = 'DEPARTED',
    /** Vessel call has been cancelled */
    CANCELLED = 'CANCELLED'
}

/**
 * Interface defining the structure of a vessel call.
 * Provides comprehensive type safety for vessel call operations and real-time updates.
 * 
 * @interface VesselCall
 */
export interface VesselCall {
    /** Unique identifier for the vessel call */
    id: number;

    /** Reference to the vessel's unique identifier */
    vesselId: number;

    /** Name of the vessel */
    vesselName: string;

    /** IMO number - unique vessel identifier as per International Maritime Organization */
    imoNumber: string;

    /** Vessel's radio call sign for the specific port call */
    callSign: string;

    /** Current status of the vessel call */
    status: VesselCallStatus;

    /** Estimated Time of Arrival */
    eta: Date;

    /** Estimated Time of Departure */
    etd: Date;

    /** Actual Time of Arrival - null if vessel hasn't arrived yet */
    ata: Date | null;

    /** Actual Time of Departure - null if vessel hasn't departed yet */
    atd: Date | null;

    /** Timestamp when the vessel call record was created */
    createdAt: Date;

    /** Timestamp of the last update to the vessel call record */
    updatedAt: Date;
}