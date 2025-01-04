package com.pcs.vcms.service.impl;

import com.pcs.vcms.service.ClearanceService;
import com.pcs.vcms.repository.ClearanceRepository;
import com.pcs.vcms.dto.ClearanceDTO;
import com.pcs.vcms.entity.Clearance;
import com.pcs.vcms.entity.Clearance.ClearanceStatus;
import com.pcs.vcms.audit.AuditService;
import com.pcs.vcms.exception.ClearanceNotFoundException;
import com.pcs.vcms.exception.InvalidStatusTransitionException;
import com.pcs.vcms.mapper.ClearanceMapper;
import com.pcs.vcms.validation.ClearanceValidator;
import io.micrometer.core.annotation.Monitored;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of ClearanceService providing secure, audited, and optimized
 * business logic for managing vessel clearances.
 *
 * @version 1.0
 * @since 2023-11-15
 */
@Service
@Validated
@Slf4j
@RequiredArgsConstructor
@CacheConfig(cacheNames = "clearances")
@Monitored
public class ClearanceServiceImpl implements ClearanceService {

    private final ClearanceRepository clearanceRepository;
    private final AuditService auditService;
    private final ClearanceValidator clearanceValidator;
    private final ClearanceMapper clearanceMapper;
    private final MeterRegistry meterRegistry;

    private final Timer submitClearanceTimer;
    private final Timer updateStatusTimer;

    /**
     * Constructor initializing performance monitoring.
     */
    public ClearanceServiceImpl(ClearanceRepository clearanceRepository,
                              AuditService auditService,
                              ClearanceValidator clearanceValidator,
                              ClearanceMapper clearanceMapper,
                              MeterRegistry meterRegistry) {
        this.clearanceRepository = clearanceRepository;
        this.auditService = auditService;
        this.clearanceValidator = clearanceValidator;
        this.clearanceMapper = clearanceMapper;
        this.meterRegistry = meterRegistry;
        
        this.submitClearanceTimer = Timer.builder("clearance.submit.time")
                .description("Time taken to submit clearance")
                .register(meterRegistry);
        this.updateStatusTimer = Timer.builder("clearance.update.time")
                .description("Time taken to update clearance status")
                .register(meterRegistry);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ROLE_AGENT') or hasRole('ROLE_PORT_AUTHORITY')")
    @CacheEvict(allEntries = true)
    public ClearanceDTO submitClearance(@Valid ClearanceDTO clearanceDTO) {
        return submitClearanceTimer.record(() -> {
            log.info("Submitting new clearance request for vessel call: {}", 
                    clearanceDTO.getVesselCallId());

            clearanceValidator.validateSubmission(clearanceDTO);

            Clearance clearance = clearanceMapper.toEntity(clearanceDTO);
            clearance.setStatus(ClearanceStatus.PENDING);
            clearance.setSubmittedAt(LocalDateTime.now());

            Clearance savedClearance = clearanceRepository.save(clearance);

            auditService.logClearanceSubmission(savedClearance);
            
            meterRegistry.counter("clearance.submissions").increment();

            return clearanceMapper.toDTO(savedClearance);
        });
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ROLE_PORT_AUTHORITY')")
    @CacheEvict(allEntries = true)
    public ClearanceDTO updateClearanceStatus(Long clearanceId, 
                                            ClearanceStatus newStatus,
                                            String remarks) {
        return updateStatusTimer.record(() -> {
            log.info("Updating clearance status: {} to {}", clearanceId, newStatus);

            Clearance clearance = clearanceRepository.findById(clearanceId)
                    .orElseThrow(() -> new ClearanceNotFoundException(clearanceId));

            validateStatusTransition(clearance.getStatus(), newStatus);

            clearance.setStatus(newStatus);
            clearance.setRemarks(remarks);
            
            if (newStatus == ClearanceStatus.APPROVED || 
                newStatus == ClearanceStatus.REJECTED) {
                clearance.setApprovedAt(LocalDateTime.now());
            }

            Clearance updatedClearance = clearanceRepository.save(clearance);

            auditService.logStatusUpdate(updatedClearance, newStatus);
            
            meterRegistry.counter("clearance.status.updates", 
                    "status", newStatus.name()).increment();

            return clearanceMapper.toDTO(updatedClearance);
        });
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ROLE_AGENT', 'ROLE_PORT_AUTHORITY')")
    @Cacheable(key = "#clearanceId")
    public Optional<ClearanceDTO> getClearanceById(Long clearanceId) {
        log.debug("Retrieving clearance by ID: {}", clearanceId);
        return clearanceRepository.findById(clearanceId)
                .map(clearanceMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ROLE_AGENT', 'ROLE_PORT_AUTHORITY')")
    @Cacheable(key = "'vesselCall:' + #vesselCallId")
    public List<ClearanceDTO> getClearancesByVesselCall(Long vesselCallId) {
        log.debug("Retrieving clearances for vessel call: {}", vesselCallId);
        return clearanceRepository.findByVesselCallId(vesselCallId, Pageable.unpaged())
                .stream()
                .map(clearanceMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ROLE_PORT_AUTHORITY')")
    public boolean validateDepartureClearances(Long vesselCallId) {
        log.info("Validating departure clearances for vessel call: {}", vesselCallId);
        
        long pendingCount = clearanceRepository
                .countByVesselCallIdAndStatus(vesselCallId, ClearanceStatus.PENDING);
        long rejectedCount = clearanceRepository
                .countByVesselCallIdAndStatus(vesselCallId, ClearanceStatus.REJECTED);

        boolean isValid = pendingCount == 0 && rejectedCount == 0;
        
        meterRegistry.counter("clearance.departure.validations",
                "result", String.valueOf(isValid)).increment();

        return isValid;
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ROLE_PORT_AUTHORITY')")
    @CacheEvict(allEntries = true)
    public ClearanceDTO cancelClearance(Long clearanceId, String remarks) {
        log.info("Cancelling clearance: {}", clearanceId);

        Clearance clearance = clearanceRepository.findById(clearanceId)
                .orElseThrow(() -> new ClearanceNotFoundException(clearanceId));

        if (clearance.getStatus() != ClearanceStatus.PENDING) {
            throw new InvalidStatusTransitionException(
                    "Only PENDING clearances can be cancelled");
        }

        clearance.setStatus(ClearanceStatus.CANCELLED);
        clearance.setRemarks(remarks);

        Clearance cancelledClearance = clearanceRepository.save(clearance);
        
        auditService.logClearanceCancellation(cancelledClearance);
        
        meterRegistry.counter("clearance.cancellations").increment();

        return clearanceMapper.toDTO(cancelledClearance);
    }

    private void validateStatusTransition(ClearanceStatus currentStatus, 
                                        ClearanceStatus newStatus) {
        if (!isValidTransition(currentStatus, newStatus)) {
            throw new InvalidStatusTransitionException(
                    String.format("Invalid status transition from %s to %s", 
                            currentStatus, newStatus));
        }
    }

    private boolean isValidTransition(ClearanceStatus currentStatus, 
                                    ClearanceStatus newStatus) {
        switch (currentStatus) {
            case PENDING:
                return newStatus == ClearanceStatus.IN_PROGRESS || 
                       newStatus == ClearanceStatus.CANCELLED;
            case IN_PROGRESS:
                return newStatus == ClearanceStatus.APPROVED || 
                       newStatus == ClearanceStatus.REJECTED;
            default:
                return false;
        }
    }
}