package com.pcs.vcms.service;

import com.pcs.vcms.dto.BerthAllocationDTO;
import com.pcs.vcms.entity.Berth;
import com.pcs.vcms.entity.BerthAllocation;
import com.pcs.vcms.entity.VesselCall;
import com.pcs.vcms.entity.Vessel;
import com.pcs.vcms.repository.BerthAllocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for BerthAllocationService implementation.
 * Validates berth allocation algorithms, optimization logic, and conflict resolution.
 *
 * @version 1.0
 * @since 2023-11-15
 */
@ExtendWith(MockitoExtension.class)
public class BerthAllocationServiceTest {

    @Mock
    private BerthAllocationRepository berthAllocationRepository;

    @InjectMocks
    private BerthAllocationService berthAllocationService;

    @Captor
    private ArgumentCaptor<BerthAllocation> berthAllocationCaptor;

    private static final Long TEST_BERTH_ID = 1L;
    private static final Long TEST_VESSEL_CALL_ID = 1L;
    private static final LocalDateTime TEST_START_TIME = LocalDateTime.now().plusHours(1);
    private static final LocalDateTime TEST_END_TIME = TEST_START_TIME.plusHours(4);
    private static final double OPTIMIZATION_THRESHOLD = 0.30;

    private BerthAllocationDTO testAllocationDTO;
    private BerthAllocation testAllocation;
    private Berth testBerth;
    private VesselCall testVesselCall;

    @BeforeEach
    void setUp() {
        // Initialize test berth
        testBerth = Berth.builder()
                .id(TEST_BERTH_ID.intValue())
                .name("Test Berth")
                .length(200.0)
                .depth(12.0)
                .status(Berth.BerthStatus.AVAILABLE)
                .build();

        // Initialize test vessel
        Vessel testVessel = Vessel.builder()
                .id(1L)
                .name("Test Vessel")
                .imoNumber("1234567")
                .build();

        // Initialize test vessel call
        testVesselCall = VesselCall.builder()
                .id(TEST_VESSEL_CALL_ID)
                .vessel(testVessel)
                .status(VesselCall.VesselCallStatus.PLANNED)
                .eta(TEST_START_TIME)
                .etd(TEST_END_TIME)
                .build();

        // Initialize test allocation DTO
        testAllocationDTO = BerthAllocationDTO.builder()
                .berthId(TEST_BERTH_ID)
                .vesselCallId(TEST_VESSEL_CALL_ID)
                .startTime(TEST_START_TIME)
                .endTime(TEST_END_TIME)
                .status(BerthAllocation.BerthAllocationStatus.SCHEDULED)
                .build();

        // Initialize test allocation entity
        testAllocation = BerthAllocation.builder()
                .berth(testBerth)
                .vesselCall(testVesselCall)
                .startTime(TEST_START_TIME)
                .endTime(TEST_END_TIME)
                .status(BerthAllocation.BerthAllocationStatus.SCHEDULED)
                .build();
    }

    @Test
    void testCreateBerthAllocation_Success() {
        // Given
        when(berthAllocationRepository.findOverlappingAllocations(
                TEST_BERTH_ID.intValue(), TEST_START_TIME, TEST_END_TIME))
                .thenReturn(List.of());
        when(berthAllocationRepository.save(any(BerthAllocation.class)))
                .thenReturn(testAllocation);

        // When
        BerthAllocationDTO result = berthAllocationService.createBerthAllocation(testAllocationDTO);

        // Then
        assertNotNull(result);
        assertEquals(TEST_BERTH_ID, result.getBerthId());
        assertEquals(TEST_VESSEL_CALL_ID, result.getVesselCallId());
        assertEquals(BerthAllocation.BerthAllocationStatus.SCHEDULED, result.getStatus());
        verify(berthAllocationRepository).save(berthAllocationCaptor.capture());
        assertEquals(TEST_START_TIME, berthAllocationCaptor.getValue().getStartTime());
    }

    @Test
    void testCreateBerthAllocation_ConflictDetected() {
        // Given
        BerthAllocation conflictingAllocation = BerthAllocation.builder()
                .berth(testBerth)
                .startTime(TEST_START_TIME.plusHours(1))
                .endTime(TEST_END_TIME.plusHours(1))
                .status(BerthAllocation.BerthAllocationStatus.SCHEDULED)
                .build();

        when(berthAllocationRepository.findOverlappingAllocations(
                TEST_BERTH_ID.intValue(), TEST_START_TIME, TEST_END_TIME))
                .thenReturn(List.of(conflictingAllocation));

        // Then
        assertThrows(IllegalStateException.class, () -> 
            berthAllocationService.createBerthAllocation(testAllocationDTO)
        );
    }

    @Test
    void testOptimizeBerthSchedule() {
        // Given
        LocalDateTime optimizationStart = LocalDateTime.now();
        LocalDateTime optimizationEnd = optimizationStart.plusDays(1);
        List<BerthAllocation> currentAllocations = Arrays.asList(testAllocation);

        when(berthAllocationRepository.findByBerthIdAndStartTimeBetween(
                TEST_BERTH_ID.intValue(), optimizationStart, optimizationEnd))
                .thenReturn(currentAllocations);

        // When
        List<BerthAllocationDTO> optimizedSchedule = berthAllocationService.optimizeBerthSchedule(
                optimizationStart, optimizationEnd);

        // Then
        assertNotNull(optimizedSchedule);
        assertFalse(optimizedSchedule.isEmpty());
        verify(berthAllocationRepository).findByBerthIdAndStartTimeBetween(
                TEST_BERTH_ID.intValue(), optimizationStart, optimizationEnd);
    }

    @Test
    void testUpdateBerthAllocation_Success() {
        // Given
        when(berthAllocationRepository.findById(TEST_VESSEL_CALL_ID))
                .thenReturn(Optional.of(testAllocation));
        when(berthAllocationRepository.save(any(BerthAllocation.class)))
                .thenReturn(testAllocation);

        // When
        testAllocationDTO.setEndTime(TEST_END_TIME.plusHours(2));
        BerthAllocationDTO result = berthAllocationService.updateBerthAllocation(
                TEST_VESSEL_CALL_ID, testAllocationDTO);

        // Then
        assertNotNull(result);
        assertEquals(TEST_END_TIME.plusHours(2), result.getEndTime());
        verify(berthAllocationRepository).save(berthAllocationCaptor.capture());
        assertEquals(TEST_END_TIME.plusHours(2), berthAllocationCaptor.getValue().getEndTime());
    }

    @Test
    void testCheckAllocationConflicts() {
        // Given
        BerthAllocation conflictingAllocation = BerthAllocation.builder()
                .berth(testBerth)
                .startTime(TEST_START_TIME.plusHours(2))
                .endTime(TEST_END_TIME.plusHours(2))
                .status(BerthAllocation.BerthAllocationStatus.SCHEDULED)
                .build();

        when(berthAllocationRepository.findOverlappingAllocations(
                TEST_BERTH_ID.intValue(), TEST_START_TIME, TEST_END_TIME))
                .thenReturn(List.of(conflictingAllocation));

        // When
        List<BerthAllocationDTO> conflicts = berthAllocationService.checkAllocationConflicts(
                testAllocationDTO);

        // Then
        assertNotNull(conflicts);
        assertEquals(1, conflicts.size());
        verify(berthAllocationRepository).findOverlappingAllocations(
                TEST_BERTH_ID.intValue(), TEST_START_TIME, TEST_END_TIME);
    }

    @Test
    void testGetAllocationsByVesselCall() {
        // Given
        when(berthAllocationRepository.findByVesselCall_Id(TEST_VESSEL_CALL_ID))
                .thenReturn(Optional.of(testAllocation));

        // When
        List<BerthAllocationDTO> allocations = berthAllocationService.getAllocationsByVesselCall(
                TEST_VESSEL_CALL_ID);

        // Then
        assertNotNull(allocations);
        assertFalse(allocations.isEmpty());
        assertEquals(TEST_VESSEL_CALL_ID, allocations.get(0).getVesselCallId());
        verify(berthAllocationRepository).findByVesselCall_Id(TEST_VESSEL_CALL_ID);
    }
}