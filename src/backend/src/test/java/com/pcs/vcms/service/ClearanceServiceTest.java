package com.pcs.vcms.service;

import com.pcs.vcms.dto.ClearanceDTO;
import com.pcs.vcms.entity.Clearance;
import com.pcs.vcms.entity.Clearance.ClearanceStatus;
import com.pcs.vcms.entity.Clearance.ClearanceType;
import com.pcs.vcms.entity.VesselCall;
import com.pcs.vcms.repository.ClearanceRepository;
import com.pcs.vcms.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for ClearanceService implementation.
 * Tests digital clearance workflows, regulatory compliance checks,
 * security controls and performance optimizations.
 */
@ExtendWith({MockitoExtension.class, SpringExtension.class})
class ClearanceServiceTest {

    @Mock
    private ClearanceRepository clearanceRepository;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private ClearanceServiceImpl clearanceService;

    private ClearanceDTO testClearanceDTO;
    private Clearance testClearance;
    private static final String TEST_USER = "test.user";
    private static final Long TEST_ID = 1L;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
        
        testClearanceDTO = ClearanceDTO.builder()
                .id(TEST_ID)
                .vesselCallId(1L)
                .vesselName("Test Vessel")
                .type(ClearanceType.CUSTOMS)
                .status(ClearanceStatus.PENDING)
                .referenceNumber("CLR-123456-ABC")
                .submittedBy(TEST_USER)
                .submittedAt(LocalDateTime.now())
                .build();

        testClearance = Clearance.builder()
                .id(TEST_ID)
                .type(ClearanceType.CUSTOMS)
                .status(ClearanceStatus.PENDING)
                .referenceNumber("CLR-123456-ABC")
                .submittedBy(TEST_USER)
                .submittedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Clearance Submission Tests")
    class ClearanceSubmissionTests {

        @Test
        @WithMockUser(roles = "PORT_AUTHORITY")
        @DisplayName("Should successfully submit clearance with valid data")
        void testSubmitClearanceSuccess() {
            when(clearanceRepository.save(any(Clearance.class))).thenReturn(testClearance);

            ClearanceDTO result = clearanceService.submitClearance(testClearanceDTO);

            assertNotNull(result);
            assertEquals(TEST_ID, result.getId());
            assertEquals(ClearanceStatus.PENDING, result.getStatus());
            verify(clearanceRepository).save(any(Clearance.class));
        }

        @Test
        @DisplayName("Should reject clearance submission with invalid reference number")
        void testSubmitClearanceInvalidReference() {
            testClearanceDTO.setReferenceNumber("INVALID-REF");

            assertThrows(IllegalArgumentException.class, () -> 
                clearanceService.submitClearance(testClearanceDTO));
        }

        @Test
        @WithMockUser(roles = "VESSEL_AGENT")
        @DisplayName("Should reject clearance submission without proper authorization")
        void testSubmitClearanceUnauthorized() {
            assertThrows(SecurityException.class, () -> 
                clearanceService.submitClearance(testClearanceDTO));
        }
    }

    @Nested
    @DisplayName("Clearance Status Update Tests")
    class ClearanceStatusUpdateTests {

        @ParameterizedTest
        @EnumSource(ClearanceStatus.class)
        @DisplayName("Should validate status transitions")
        void testStatusTransitions(ClearanceStatus newStatus) {
            when(clearanceRepository.findById(TEST_ID)).thenReturn(Optional.of(testClearance));
            when(clearanceRepository.save(any(Clearance.class))).thenReturn(testClearance);

            try {
                ClearanceDTO result = clearanceService.updateClearanceStatus(
                    TEST_ID, newStatus, "Test remarks");
                
                assertNotNull(result);
                assertEquals(newStatus, result.getStatus());
            } catch (IllegalStateException e) {
                // Expected for invalid transitions
                assertTrue(isInvalidTransition(testClearance.getStatus(), newStatus));
            }
        }

        @Test
        @DisplayName("Should maintain audit trail on status update")
        void testStatusUpdateAuditTrail() {
            when(clearanceRepository.findById(TEST_ID)).thenReturn(Optional.of(testClearance));
            when(clearanceRepository.save(any(Clearance.class))).thenReturn(testClearance);

            ClearanceDTO result = clearanceService.updateClearanceStatus(
                TEST_ID, ClearanceStatus.IN_PROGRESS, "Status update test");

            assertNotNull(result.getUpdatedAt());
            assertNotNull(result.getRemarks());
        }
    }

    @Nested
    @DisplayName("Clearance Query Tests")
    class ClearanceQueryTests {

        @Test
        @DisplayName("Should retrieve clearances with pagination")
        void testGetClearancesWithPagination() {
            PageRequest pageRequest = PageRequest.of(0, 10);
            List<Clearance> clearances = new ArrayList<>();
            clearances.add(testClearance);
            Page<Clearance> clearancePage = new PageImpl<>(clearances);

            when(clearanceRepository.findAll(any(PageRequest.class))).thenReturn(clearancePage);

            Page<ClearanceDTO> result = clearanceService.getClearances(
                pageRequest, null, null, null);

            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            verify(clearanceRepository).findAll(pageRequest);
        }

        @Test
        @DisplayName("Should retrieve clearances by vessel call")
        void testGetClearancesByVesselCall() {
            List<Clearance> clearances = new ArrayList<>();
            clearances.add(testClearance);

            when(clearanceRepository.findByVesselCallId(anyLong())).thenReturn(clearances);

            List<ClearanceDTO> result = clearanceService.getClearancesByVesselCall(1L);

            assertNotNull(result);
            assertFalse(result.isEmpty());
            assertEquals(TEST_ID, result.get(0).getId());
        }
    }

    @Nested
    @DisplayName("Regulatory Compliance Tests")
    class RegulatoryComplianceTests {

        @Test
        @DisplayName("Should validate regulatory compliance")
        void testValidateRegulatoryCompliance() {
            testClearanceDTO.setType(ClearanceType.CUSTOMS);
            
            boolean result = clearanceService.validateRegulatoryCompliance(testClearanceDTO);

            assertTrue(result);
        }

        @Test
        @DisplayName("Should validate departure clearances")
        void testValidateDepartureClearances() {
            List<Clearance> clearances = new ArrayList<>();
            clearances.add(testClearance);

            when(clearanceRepository.findByVesselCallId(anyLong())).thenReturn(clearances);

            boolean result = clearanceService.validateDepartureClearances(1L);

            assertFalse(result); // Should be false as not all clearances are approved
        }
    }

    private boolean isInvalidTransition(ClearanceStatus current, ClearanceStatus next) {
        if (current == ClearanceStatus.PENDING) {
            return !(next == ClearanceStatus.IN_PROGRESS || next == ClearanceStatus.CANCELLED);
        }
        if (current == ClearanceStatus.IN_PROGRESS) {
            return !(next == ClearanceStatus.APPROVED || next == ClearanceStatus.REJECTED);
        }
        return true; // All other transitions are invalid
    }
}