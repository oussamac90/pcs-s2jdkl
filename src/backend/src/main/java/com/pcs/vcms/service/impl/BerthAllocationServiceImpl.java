package com.pcs.vcms.service.impl;

import com.pcs.vcms.dto.BerthAllocationDTO;
import com.pcs.vcms.entity.BerthAllocation;
import com.pcs.vcms.entity.BerthAllocation.BerthAllocationStatus;
import com.pcs.vcms.entity.Berth;
import com.pcs.vcms.entity.VesselCall;
import com.pcs.vcms.repository.BerthAllocationRepository;
import com.pcs.vcms.service.BerthAllocationService;
import com.pcs.vcms.util.BerthAllocationAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.websocket.client.WebSocketTemplate;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of BerthAllocationService providing intelligent berth allocation management
 * with optimization algorithms to achieve 30% improvement in berth utilization.
 *
 * @version 1.0
 * @since 2023-11-15
 */
@Service
@Slf4j
@Transactional
public class BerthAllocationServiceImpl implements BerthAllocationService {

    private final BerthAllocationRepository berthAllocationRepository;
    private final BerthAllocationAlgorithm berthAllocationAlgorithm;
    private final WebSocketTemplate webSocketTemplate;

    private static final String ALLOCATION_TOPIC = "/topic/berth-allocations";
    private static final String CONFLICT_TOPIC = "/topic/allocation-conflicts";

    @Autowired
    public BerthAllocationServiceImpl(
            BerthAllocationRepository berthAllocationRepository,
            BerthAllocationAlgorithm berthAllocationAlgorithm,
            WebSocketTemplate webSocketTemplate) {
        this.berthAllocationRepository = berthAllocationRepository;
        this.berthAllocationAlgorithm = berthAllocationAlgorithm;
        this.webSocketTemplate = webSocketTemplate;
    }

    @Override
    @Transactional
    public BerthAllocationDTO createBerthAllocation(BerthAllocationDTO allocationDTO) {
        log.info("Creating berth allocation for vessel call ID: {}", allocationDTO.getVesselCallId());

        // Validate time range
        allocationDTO.validateTimeRange();

        // Check for conflicts
        List<BerthAllocation> conflicts = berthAllocationRepository.findOverlappingAllocations(
                allocationDTO.getBerthId().intValue(),
                allocationDTO.getStartTime(),
                allocationDTO.getEndTime()
        );

        if (!conflicts.isEmpty()) {
            log.warn("Found {} conflicting allocations", conflicts.size());
            // Resolve conflicts using optimization algorithm
            List<BerthAllocation> resolvedAllocations = berthAllocationAlgorithm
                    .resolveAllocationConflicts(conflicts);
            
            // Notify about conflict resolution
            webSocketTemplate.convertAndSend(CONFLICT_TOPIC, resolvedAllocations);
            
            // Update resolved allocations
            resolvedAllocations.forEach(berthAllocationRepository::save);
        }

        // Create new allocation
        BerthAllocation allocation = convertToEntity(allocationDTO);
        allocation = berthAllocationRepository.save(allocation);

        // Notify subscribers about new allocation
        webSocketTemplate.convertAndSend(ALLOCATION_TOPIC, convertToDTO(allocation));

        log.info("Successfully created berth allocation with ID: {}", allocation.getId());
        return convertToDTO(allocation);
    }

    @Override
    @Transactional
    public BerthAllocationDTO updateBerthAllocation(Long id, BerthAllocationDTO allocationDTO) {
        log.info("Updating berth allocation ID: {}", id);

        BerthAllocation existingAllocation = berthAllocationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Berth allocation not found: " + id));

        // Validate time range
        allocationDTO.validateTimeRange();

        // Check for conflicts excluding current allocation
        List<BerthAllocation> conflicts = berthAllocationRepository.findOverlappingAllocations(
                allocationDTO.getBerthId().intValue(),
                allocationDTO.getStartTime(),
                allocationDTO.getEndTime()
        ).stream()
                .filter(a -> !a.getId().equals(id))
                .collect(Collectors.toList());

        if (!conflicts.isEmpty()) {
            log.warn("Found {} conflicting allocations during update", conflicts.size());
            // Resolve conflicts
            List<BerthAllocation> resolvedAllocations = berthAllocationAlgorithm
                    .resolveAllocationConflicts(conflicts);
            
            // Notify about conflict resolution
            webSocketTemplate.convertAndSend(CONFLICT_TOPIC, resolvedAllocations);
            
            // Update resolved allocations
            resolvedAllocations.forEach(berthAllocationRepository::save);
        }

        // Update existing allocation
        updateEntityFromDTO(existingAllocation, allocationDTO);
        BerthAllocation updatedAllocation = berthAllocationRepository.save(existingAllocation);

        // Notify subscribers about update
        webSocketTemplate.convertAndSend(ALLOCATION_TOPIC, convertToDTO(updatedAllocation));

        log.info("Successfully updated berth allocation ID: {}", id);
        return convertToDTO(updatedAllocation);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BerthAllocationDTO> getBerthAllocation(Long id) {
        log.debug("Retrieving berth allocation ID: {}", id);
        return berthAllocationRepository.findById(id)
                .map(this::convertToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BerthAllocationDTO> getAllocationsByVesselCall(Long vesselCallId) {
        log.debug("Retrieving allocations for vessel call ID: {}", vesselCallId);
        return berthAllocationRepository.findByVesselCall_Id(vesselCallId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void cancelBerthAllocation(Long id) {
        log.info("Cancelling berth allocation ID: {}", id);
        
        BerthAllocation allocation = berthAllocationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Berth allocation not found: " + id));
        
        allocation.setStatus(BerthAllocationStatus.CANCELLED);
        berthAllocationRepository.save(allocation);

        // Notify subscribers about cancellation
        webSocketTemplate.convertAndSend(ALLOCATION_TOPIC, convertToDTO(allocation));

        log.info("Successfully cancelled berth allocation ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BerthAllocationDTO> checkAllocationConflicts(BerthAllocationDTO allocationDTO) {
        log.debug("Checking conflicts for proposed allocation");
        return berthAllocationRepository.findOverlappingAllocations(
                allocationDTO.getBerthId().intValue(),
                allocationDTO.getStartTime(),
                allocationDTO.getEndTime()
        ).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private BerthAllocation convertToEntity(BerthAllocationDTO dto) {
        return BerthAllocation.builder()
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .status(dto.getStatus())
                .build();
    }

    private BerthAllocationDTO convertToDTO(BerthAllocation entity) {
        return BerthAllocationDTO.builder()
                .id(entity.getId())
                .vesselCallId(entity.getVesselCall().getId())
                .vesselName(entity.getVesselCall().getVessel().getName())
                .berthId(entity.getBerth().getId().longValue())
                .berthName(entity.getBerth().getName())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private void updateEntityFromDTO(BerthAllocation entity, BerthAllocationDTO dto) {
        entity.setStartTime(dto.getStartTime());
        entity.setEndTime(dto.getEndTime());
        entity.setStatus(dto.getStatus());
    }
}