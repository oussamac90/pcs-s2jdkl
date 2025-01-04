package com.pcs.vcms.mapper;

import com.pcs.vcms.dto.ServiceBookingDTO;
import com.pcs.vcms.entity.ServiceBooking;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * MapStruct mapper interface for bidirectional conversion between ServiceBooking entities and DTOs.
 * Implements comprehensive mapping strategies with proper null handling and nested object mapping.
 *
 * @version 1.0
 * @since 2023-11-15
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ServiceBookingMapper {

    /**
     * Converts a ServiceBooking entity to its DTO representation.
     * Maps nested vessel call relationships and maintains data consistency.
     *
     * @param booking the service booking entity to convert
     * @return the corresponding DTO with mapped fields
     */
    @Mapping(source = "vesselCall.id", target = "vesselCallId")
    @Mapping(source = "vesselCall.vessel.name", target = "vesselName")
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "updatedAt", target = "updatedAt")
    ServiceBookingDTO toDTO(ServiceBooking booking);

    /**
     * Converts a ServiceBookingDTO to its entity representation.
     * Ignores read-only and managed fields during the conversion.
     *
     * @param dto the service booking DTO to convert
     * @return the corresponding entity with mapped fields
     */
    @Mapping(target = "vesselCall", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    ServiceBooking toEntity(ServiceBookingDTO dto);

    /**
     * Updates an existing ServiceBooking entity with data from a DTO.
     * Preserves managed fields and relationships during the update.
     *
     * @param dto the DTO containing updated data
     * @param entity the existing entity to update
     */
    @Mapping(target = "vesselCall", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    void updateEntityFromDTO(ServiceBookingDTO dto, @MappingTarget ServiceBooking entity);

    /**
     * Converts a list of ServiceBooking entities to a list of DTOs.
     * Maintains order and handles null values appropriately.
     *
     * @param bookings the list of service booking entities to convert
     * @return list of corresponding DTOs
     */
    List<ServiceBookingDTO> toDTOList(List<ServiceBooking> bookings);

    /**
     * Converts a list of ServiceBookingDTOs to a list of entities.
     * Maintains order and handles null values appropriately.
     *
     * @param dtos the list of service booking DTOs to convert
     * @return list of corresponding entities
     */
    List<ServiceBooking> toEntityList(List<ServiceBookingDTO> dtos);
}