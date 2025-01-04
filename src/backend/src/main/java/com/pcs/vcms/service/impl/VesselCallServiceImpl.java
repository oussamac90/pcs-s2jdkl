package com.pcs.vcms.service.impl;

import com.pcs.vcms.dto.VesselCallDTO;
import com.pcs.vcms.entity.VesselCall;
import com.pcs.vcms.entity.VesselCall.VesselCallStatus;
import com.pcs.vcms.mapper.VesselCallMapper;
import com.pcs.vcms.repository.VesselCallRepository;
import com.pcs.vcms.service.NotificationService;
import com.pcs.vcms.service.VesselCallService;
import com.pcs.vcms.exception.DuplicateCallSignException;
import com.pcs.vcms.exception.EntityNotFoundException;
import com.pcs.vcms.exception.IllegalStateTransitionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.validation.annotation.Validated;

import lombok.extern.slf4j.Slf4j;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Implementation of VesselCallService providing comprehensive vessel call management
 * with enhanced security, caching, and audit logging capabilities.
 *
 * @version 1.0
 * @since 2023-11-15
 */
@Service
@Validated
@Slf4j
@Transactional(isolation = Isolation.READ_COMMITTED)
public class VesselCallServiceImpl implements VesselCallService {

    private final VesselCallRepository vesselCallRepository;
    private final VesselCallMapper vesselCallMapper;
    private final NotificationService notificationService;
    private final CacheManager cacheManager;

    @Autowired
    public VesselCallServiceImpl(
            VesselCallRepository vesselCallRepository,
            VesselCallMapper vesselCallMapper,
            NotificationService notificationService,
            CacheManager cacheManager) {
        this.vesselCallRepository = vesselCallRepository;
        this.vesselCallMapper = vesselCallMapper;
        this.notificationService = notificationService;
        this.cacheManager = cacheManager;
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('VESSEL_OPERATOR')")
    public VesselCallDTO createVesselCall(@Valid @NotNull VesselCallDTO vesselCallDTO) {
        log.info("Creating new vessel call with call sign: {}", vesselCallDTO.getCallSign());

        // Validate call sign uniqueness
        if (vesselCallRepository.existsByCallSign(vesselCallDTO.getCallSign())) {
            throw new DuplicateCallSignException("Call sign already exists: " + vesselCallDTO.getCallSign());
        }

        // Convert DTO to entity and validate
        VesselCall vesselCall = vesselCallMapper.toEntity(vesselCallDTO);
        vesselCall.setStatus(VesselCallStatus.PLANNED);

        // Save and notify
        VesselCall savedCall = vesselCallRepository.save(vesselCall);
        notificationService.sendVesselCallUpdate(savedCall);

        log.info("Created vessel call with ID: {}", savedCall.getId());
        return vesselCallMapper.toDTO(savedCall);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('VESSEL_OPERATOR', 'PORT_AUTHORITY')")
    @CacheEvict(value = "vesselCalls", key = "#id")
    public VesselCallDTO updateVesselCall(@NotNull Long id, @Valid @NotNull VesselCallDTO vesselCallDTO) {
        log.info("Updating vessel call with ID: {}", id);

        VesselCall existingCall = vesselCallRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Vessel call not found with ID: " + id));

        vesselCallMapper.updateEntityFromDTO(vesselCallDTO, existingCall);
        VesselCall updatedCall = vesselCallRepository.save(existingCall);
        notificationService.sendVesselCallUpdate(updatedCall);

        log.info("Updated vessel call with ID: {}", id);
        return vesselCallMapper.toDTO(updatedCall);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('VESSEL_OPERATOR', 'PORT_AUTHORITY', 'SERVICE_PROVIDER')")
    @Cacheable(value = "vesselCalls", key = "#id")
    public Optional<VesselCallDTO> getVesselCall(@NotNull Long id) {
        log.debug("Retrieving vessel call with ID: {}", id);
        return vesselCallRepository.findById(id)
                .map(vesselCallMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('VESSEL_OPERATOR', 'PORT_AUTHORITY', 'SERVICE_PROVIDER')")
    @Cacheable(value = "vesselCallsByCallSign")
    public Optional<VesselCallDTO> findByCallSign(@NotNull String callSign) {
        log.debug("Finding vessel call by call sign: {}", callSign);
        return vesselCallRepository.findByCallSign(callSign)
                .map(vesselCallMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('VESSEL_OPERATOR', 'PORT_AUTHORITY', 'SERVICE_PROVIDER')")
    public Page<VesselCallDTO> findByStatus(@NotNull VesselCallStatus status, Pageable pageable) {
        log.debug("Finding vessel calls by status: {}", status);
        return vesselCallRepository.findByStatus(status, pageable)
                .map(vesselCallMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('VESSEL_OPERATOR', 'PORT_AUTHORITY', 'SERVICE_PROVIDER')")
    public Page<VesselCallDTO> findByDateRange(
            @NotNull LocalDateTime startDate,
            @NotNull LocalDateTime endDate,
            Pageable pageable) {
        log.debug("Finding vessel calls between {} and {}", startDate, endDate);
        return vesselCallRepository.findByEtaBetween(startDate, endDate, pageable)
                .map(vesselCallMapper::toDTO);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('VESSEL_OPERATOR', 'PORT_AUTHORITY')")
    @CacheEvict(value = "vesselCalls", key = "#id")
    public VesselCallDTO updateStatus(@NotNull Long id, @NotNull VesselCallStatus newStatus) {
        log.info("Updating status to {} for vessel call ID: {}", newStatus, id);

        VesselCall vesselCall = vesselCallRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Vessel call not found with ID: " + id));

        validateStatusTransition(vesselCall.getStatus(), newStatus);
        vesselCall.setStatus(newStatus);

        // Update actual times based on status
        updateActualTimes(vesselCall, newStatus);

        VesselCall updatedCall = vesselCallRepository.save(vesselCall);
        notificationService.sendVesselCallUpdate(updatedCall);

        log.info("Updated status to {} for vessel call ID: {}", newStatus, id);
        return vesselCallMapper.toDTO(updatedCall);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('VESSEL_OPERATOR', 'PORT_AUTHORITY')")
    @CacheEvict(value = "vesselCalls", key = "#id")
    public VesselCallDTO cancelVesselCall(@NotNull Long id, String reason) {
        log.info("Cancelling vessel call with ID: {} - Reason: {}", id, reason);

        VesselCall vesselCall = vesselCallRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Vessel call not found with ID: " + id));

        if (vesselCall.getStatus() == VesselCallStatus.DEPARTED) {
            throw new IllegalStateTransitionException("Cannot cancel a departed vessel call");
        }

        vesselCall.setStatus(VesselCallStatus.CANCELLED);
        VesselCall cancelledCall = vesselCallRepository.save(vesselCall);
        notificationService.sendVesselCallUpdate(cancelledCall);

        log.info("Cancelled vessel call with ID: {}", id);
        return vesselCallMapper.toDTO(cancelledCall);
    }

    private void validateStatusTransition(VesselCallStatus currentStatus, VesselCallStatus newStatus) {
        if (!isValidStatusTransition(currentStatus, newStatus)) {
            throw new IllegalStateTransitionException(
                    String.format("Invalid status transition from %s to %s", currentStatus, newStatus));
        }
    }

    private boolean isValidStatusTransition(VesselCallStatus currentStatus, VesselCallStatus newStatus) {
        switch (currentStatus) {
            case PLANNED:
                return newStatus == VesselCallStatus.ARRIVED || newStatus == VesselCallStatus.CANCELLED;
            case ARRIVED:
                return newStatus == VesselCallStatus.AT_BERTH || newStatus == VesselCallStatus.CANCELLED;
            case AT_BERTH:
                return newStatus == VesselCallStatus.DEPARTED || newStatus == VesselCallStatus.CANCELLED;
            case DEPARTED:
            case CANCELLED:
                return false;
            default:
                return false;
        }
    }

    private void updateActualTimes(VesselCall vesselCall, VesselCallStatus newStatus) {
        LocalDateTime now = LocalDateTime.now();
        switch (newStatus) {
            case ARRIVED:
                vesselCall.setAta(now);
                break;
            case DEPARTED:
                vesselCall.setAtd(now);
                break;
            default:
                break;
        }
    }
}