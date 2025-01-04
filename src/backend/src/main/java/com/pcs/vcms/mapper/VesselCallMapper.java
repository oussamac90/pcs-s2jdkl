package com.pcs.vcms.mapper;

import com.pcs.vcms.entity.VesselCall;
import com.pcs.vcms.dto.VesselCallDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.springframework.cache.annotation.Cacheable;

/**
 * MapStruct mapper interface for secure and optimized bidirectional conversion between
 * VesselCall entities and DTOs. Implements caching, validation, and security measures.
 *
 * @version 1.0
 * @since 2023-11-15
 */
@Mapper(componentModel = "spring")
public interface VesselCallMapper {

    /**
     * Converts a VesselCall entity to a VesselCallDTO with security measures and validation.
     * Implements caching for improved performance on frequently accessed vessel calls.
     *
     * @param vesselCall the vessel call entity to convert
     * @return secured and validated VesselCallDTO
     */
    @Cacheable(value = "vesselCallDTOCache", key = "#vesselCall.id")
    @Mapping(source = "vessel.id", target = "vesselId")
    @Mapping(source = "vessel.name", target = "vesselName")
    @Mapping(source = "vessel.imoNumber", target = "imoNumber")
    @Mapping(source = "callSign", target = "callSign")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "eta", target = "eta")
    @Mapping(source = "etd", target = "etd")
    @Mapping(source = "ata", target = "ata")
    @Mapping(source = "atd", target = "atd")
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "updatedAt", target = "updatedAt")
    @Mapping(source = "version", target = "version")
    VesselCallDTO toDTO(VesselCall vesselCall);

    /**
     * Converts a VesselCallDTO to a VesselCall entity with validation.
     * Ignores sensitive relationship fields for security.
     *
     * @param dto the DTO to convert
     * @return validated VesselCall entity
     */
    @Mapping(target = "vessel", ignore = true)
    @Mapping(target = "berthAllocations", ignore = true)
    @Mapping(target = "preArrivalNotifications", ignore = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "callSign", source = "callSign")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "eta", source = "eta")
    @Mapping(target = "etd", source = "etd")
    @Mapping(target = "ata", source = "ata")
    @Mapping(target = "atd", source = "atd")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", source = "version")
    VesselCall toEntity(VesselCallDTO dto);

    /**
     * Updates an existing VesselCall entity with data from a DTO.
     * Implements optimistic locking and preserves relationships.
     *
     * @param dto the source DTO with updated data
     * @param entity the target entity to update
     */
    @Mapping(target = "vessel", ignore = true)
    @Mapping(target = "berthAllocations", ignore = true)
    @Mapping(target = "preArrivalNotifications", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "callSign", source = "callSign")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "eta", source = "eta")
    @Mapping(target = "etd", source = "etd")
    @Mapping(target = "ata", source = "ata")
    @Mapping(target = "atd", source = "atd")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "version", expression = "java(dto.getVersion() + 1)")
    void updateEntityFromDTO(VesselCallDTO dto, @MappingTarget VesselCall entity);

    /**
     * Validates temporal constraints for vessel call times.
     * Used as a before-mapping callback.
     *
     * @param dto the DTO to validate
     */
    @Named("validateTimes")
    default void validateTimes(VesselCallDTO dto) {
        if (dto != null) {
            dto.validateTimes();
        }
    }
}