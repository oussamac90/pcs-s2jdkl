package com.pcs.vcms.mapper;

import com.pcs.vcms.entity.Clearance;
import com.pcs.vcms.dto.ClearanceDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper interface for secure and efficient conversion between Clearance entity
 * and ClearanceDTO objects. Implements comprehensive field mappings with audit trail preservation
 * and nested object handling for the Vessel Call Management System.
 *
 * @version 1.0
 * @since 2023-11-15
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ClearanceMapper {

    /**
     * Converts a Clearance entity to ClearanceDTO with complete field mapping
     * including nested vessel call information and audit trail preservation.
     *
     * @param clearance The Clearance entity to convert
     * @return ClearanceDTO with all mapped fields
     */
    @Mapping(source = "vesselCall.id", target = "vesselCallId")
    @Mapping(source = "vesselCall.vessel.name", target = "vesselName")
    @Mapping(source = "type", target = "type")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "referenceNumber", target = "referenceNumber")
    @Mapping(source = "submittedBy", target = "submittedBy")
    @Mapping(source = "approvedBy", target = "approvedBy")
    @Mapping(source = "remarks", target = "remarks")
    @Mapping(source = "submittedAt", target = "submittedAt")
    @Mapping(source = "approvedAt", target = "approvedAt")
    @Mapping(source = "validUntil", target = "validUntil")
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "updatedAt", target = "updatedAt")
    ClearanceDTO toDTO(Clearance clearance);

    /**
     * Converts a ClearanceDTO to Clearance entity with appropriate field mapping
     * and audit data preservation. VesselCall relationship is handled separately
     * by the service layer.
     *
     * @param dto The ClearanceDTO to convert
     * @return Clearance entity with mapped fields
     */
    @Mapping(target = "vesselCall", ignore = true)
    @Mapping(target = "type", source = "type")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "referenceNumber", source = "referenceNumber")
    @Mapping(target = "submittedBy", source = "submittedBy")
    @Mapping(target = "approvedBy", source = "approvedBy")
    @Mapping(target = "remarks", source = "remarks")
    @Mapping(target = "submittedAt", source = "submittedAt")
    @Mapping(target = "approvedAt", source = "approvedAt")
    @Mapping(target = "validUntil", source = "validUntil")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Clearance toEntity(ClearanceDTO dto);

    /**
     * Updates an existing Clearance entity with data from a DTO while preserving
     * the audit trail and existing relationships.
     *
     * @param dto The ClearanceDTO containing updated data
     * @param entity The existing Clearance entity to update
     * @return Updated Clearance entity
     */
    @Mapping(target = "vesselCall", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Clearance updateEntity(@MappingTarget Clearance entity, ClearanceDTO dto);
}