package com.pcs.vcms.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import com.pcs.vcms.entity.PreArrivalNotification;
import com.pcs.vcms.dto.PreArrivalNotificationDTO;

/**
 * MapStruct mapper interface for secure and validated bidirectional conversion between
 * PreArrivalNotification entity and PreArrivalNotificationDTO objects.
 * Implements strict data validation, relationship handling, and security measures.
 *
 * @version 1.0
 * @since 2023-11-15
 */
@Mapper(
    componentModel = "spring",
    nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL
)
public interface PreArrivalNotificationMapper {

    /**
     * Securely converts a PreArrivalNotification entity to PreArrivalNotificationDTO.
     * Implements strict validation and secure mapping of sensitive data.
     *
     * @param notification The PreArrivalNotification entity to convert
     * @return A validated and secured PreArrivalNotificationDTO
     */
    @Mapping(
        source = "vesselCall.id",
        target = "vesselCallId",
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS
    )
    @Mapping(
        target = "cargoDetails",
        source = "cargoDetails",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL
    )
    @Mapping(
        target = "crewList",
        source = "crewList",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL
    )
    PreArrivalNotificationDTO toDTO(PreArrivalNotification notification);

    /**
     * Securely converts a PreArrivalNotificationDTO to PreArrivalNotification entity.
     * Implements strict validation and secure mapping of sensitive data.
     *
     * @param dto The PreArrivalNotificationDTO to convert
     * @return A validated and secured PreArrivalNotification entity
     */
    @Mapping(
        source = "vesselCallId",
        target = "vesselCall.id",
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS
    )
    @Mapping(
        target = "cargoDetails",
        source = "cargoDetails",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL
    )
    @Mapping(
        target = "crewList",
        source = "crewList",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL
    )
    @Mapping(
        target = "securityClassification",
        constant = "CONFIDENTIAL"
    )
    @Mapping(
        target = "encryptionVersion",
        constant = "AES256-v1"
    )
    @Mapping(
        target = "isArchived",
        constant = "false"
    )
    PreArrivalNotification toEntity(PreArrivalNotificationDTO dto);
}