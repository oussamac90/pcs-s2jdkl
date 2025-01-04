/**
 * @fileoverview User model definitions for authentication and authorization
 * @version 1.0.0
 * 
 * Implements role-based access control and user data structure with strict type safety
 * and security considerations for the Vessel Call Management System.
 */

/**
 * Enumeration of user roles that strictly correspond to the system's permission matrix.
 * Each role has specific access levels defined for various system functions.
 * 
 * @enum {string}
 */
export enum UserRole {
    /**
     * Port Authority role with full access to vessel calls, berth management,
     * service approval capabilities, and view-only access to admin functions
     */
    PORT_AUTHORITY = 'PORT_AUTHORITY',

    /**
     * Vessel Agent role with create/read access to vessel calls,
     * view-only access to berth management, and service request capabilities
     */
    VESSEL_AGENT = 'VESSEL_AGENT',

    /**
     * Service Provider role with read-only access to vessel calls and berth management,
     * and management capabilities for their own services
     */
    SERVICE_PROVIDER = 'SERVICE_PROVIDER',

    /**
     * System Administrator role with full access to all system functions
     * including administrative capabilities
     */
    SYSTEM_ADMIN = 'SYSTEM_ADMIN'
}

/**
 * Interface defining the complete structure of user data with strict typing.
 * Excludes sensitive information (password, tokens) and includes audit fields.
 * 
 * @interface User
 */
export interface User {
    /**
     * Unique identifier for the user
     */
    id: string;

    /**
     * Unique username for authentication
     */
    username: string;

    /**
     * User's email address for notifications and communication
     */
    email: string;

    /**
     * User's assigned role determining system access permissions
     */
    role: UserRole;

    /**
     * User's first name
     */
    firstName: string;

    /**
     * User's last name
     */
    lastName: string;

    /**
     * Organization or company the user represents
     */
    organization: string;

    /**
     * Timestamp of user's last successful login
     * Stored in ISO 8601 format
     */
    lastLoginAt: string;
}