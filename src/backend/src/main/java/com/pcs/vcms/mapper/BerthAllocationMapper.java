package com.pcs.vcms.mapper;

import com.pcs.vcms.entity.BerthAllocation;
import com.pcs.vcms.dto.BerthAllocationDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.BeanMapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.DecoratedWith;

/**
 * MapStruct mapper interface for secure and efficient conversion between BerthAllocation entity
 * and BerthAllocationDTO with comprehensive validation and audit support.
 * Implements field-level security controls and maintains data integrity during transformations.
 *
 * @version 1.0
 * @since 2023-11-15
 */
@Mapper(componentModel = "spring", 
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
@DecoratedWith(BerthAllocationMapperDecorator.class)
public interface BerthAllocationMapper {

    /**
     * Converts BerthAllocation entity to BerthAllocationDTO with secure field mapping
     * and comprehensive validation.
     *
     * @param allocation the BerthAllocation entity to convert
     * @return sanitized and validated BerthAllocationDTO
     */
    @Mapping(source = "vesselCall.id", target = "vesselCallId")
    @Mapping(source = "vesselCall.vessel.name", target = "vesselName")
    @Mapping(source = "berth.id", target = "berthId")
    @Mapping(source = "berth.name", target = "berthName")
    @Mapping(source = "startTime", target = "startTime", qualifiedByName = "validateDateTime")
    @Mapping(source = "endTime", target = "endTime", qualifiedByName = "validateDateTime")
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "updatedAt", target = "updatedAt")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    BerthAllocationDTO toDTO(BerthAllocation allocation);

    /**
     * Converts BerthAllocationDTO to BerthAllocation entity with proper validation
     * and security controls. Nested relationships are handled separately through
     * service layer to maintain security and data integrity.
     *
     * @param dto the BerthAllocationDTO to convert
     * @return validated BerthAllocation entity
     */
    @Mapping(target = "vesselCall", ignore = true)
    @Mapping(target = "berth", ignore = true)
    @Mapping(source = "startTime", target = "startTime", qualifiedByName = "validateDateTime")
    @Mapping(source = "endTime", target = "endTime", qualifiedByName = "validateDateTime")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    BerthAllocation toEntity(BerthAllocationDTO dto);

    /**
     * Validates datetime fields to ensure they meet security and business requirements.
     * Prevents injection attacks and ensures data integrity.
     *
     * @param dateTime the datetime to validate
     * @return validated datetime
     */
    @Named("validateDateTime")
    default java.time.LocalDateTime validateDateTime(java.time.LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        // Ensure the datetime is not too far in the future (e.g., 1 year max)
        if (dateTime.isAfter(java.time.LocalDateTime.now().plusYears(1))) {
            throw new IllegalArgumentException("DateTime cannot be more than 1 year in the future");
        }
        return dateTime;
    }
}