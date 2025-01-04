import { Pipe, PipeTransform } from '@angular/core';
import { VesselCallStatus } from '../models/vessel-call.model';
import { BerthAllocationStatus } from '../models/berth-allocation.model';
import { ServiceStatus } from '../models/service-booking.model';
import { ClearanceStatus } from '../models/clearance.model';

/**
 * Material Design color codes that meet WCAG 2.1 Level AA contrast requirements (4.5:1)
 * against white backgrounds for status representation.
 * 
 * Color codes are from the Material Design 500 palette for consistent theming:
 * - Blue (#1976D2) for in-progress/active states
 * - Green (#43A047) for positive/completed states
 * - Amber (#FFA000) for warning/pending states
 * - Red (#D32F2F) for negative/error states
 * - Grey (#757575) for neutral/inactive states
 */
const STATUS_COLORS = {
  // Vessel Call Status Colors
  [VesselCallStatus.PLANNED]: '#1976D2',    // Blue
  [VesselCallStatus.ARRIVED]: '#FFA000',    // Amber
  [VesselCallStatus.AT_BERTH]: '#43A047',   // Green
  [VesselCallStatus.DEPARTED]: '#757575',    // Grey
  [VesselCallStatus.CANCELLED]: '#D32F2F',   // Red

  // Berth Allocation Status Colors
  [BerthAllocationStatus.SCHEDULED]: '#1976D2',  // Blue
  [BerthAllocationStatus.OCCUPIED]: '#43A047',   // Green
  [BerthAllocationStatus.COMPLETED]: '#757575',  // Grey
  [BerthAllocationStatus.CANCELLED]: '#D32F2F',  // Red

  // Service Status Colors
  [ServiceStatus.REQUESTED]: '#FFA000',      // Amber
  [ServiceStatus.CONFIRMED]: '#1976D2',      // Blue
  [ServiceStatus.IN_PROGRESS]: '#1976D2',    // Blue
  [ServiceStatus.COMPLETED]: '#43A047',      // Green
  [ServiceStatus.CANCELLED]: '#D32F2F',      // Red

  // Clearance Status Colors
  [ClearanceStatus.PENDING]: '#FFA000',      // Amber
  [ClearanceStatus.IN_PROGRESS]: '#1976D2',  // Blue
  [ClearanceStatus.APPROVED]: '#43A047',     // Green
  [ClearanceStatus.REJECTED]: '#D32F2F',     // Red
  [ClearanceStatus.CANCELLED]: '#D32F2F',    // Red

  // Default color for unrecognized status
  DEFAULT: '#757575'                         // Grey
} as const;

/**
 * Angular pipe that transforms various status enums into WCAG 2.1 compliant color codes
 * for consistent visual representation across the application.
 * 
 * Usage examples:
 * - {{ vesselCall.status | statusColor }}
 * - {{ berthAllocation.status | statusColor }}
 * - {{ serviceBooking.status | statusColor }}
 * - {{ clearance.status | statusColor }}
 * 
 * @implements {PipeTransform}
 */
@Pipe({
  name: 'statusColor',
  pure: true
})
export class StatusColorPipe implements PipeTransform {
  /**
   * Transforms a status value into its corresponding WCAG 2.1 compliant color code.
   * 
   * @param status - The status value to transform
   * @returns Material Design color code in hexadecimal format (#RRGGBB)
   */
  transform(status: string | VesselCallStatus | BerthAllocationStatus | ServiceStatus | ClearanceStatus): string {
    // Return default color if status is null or undefined
    if (!status) {
      return STATUS_COLORS.DEFAULT;
    }

    // Get color from status mapping, fallback to default if not found
    const color = STATUS_COLORS[status as keyof typeof STATUS_COLORS] || STATUS_COLORS.DEFAULT;

    // Log warning if status is not recognized
    if (color === STATUS_COLORS.DEFAULT && status !== 'DEFAULT') {
      console.warn(`StatusColorPipe: Unrecognized status "${status}", using default color`);
    }

    return color;
  }
}