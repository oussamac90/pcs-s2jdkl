package com.pcs.vcms.service;

import com.pcs.vcms.dto.VesselCallDTO;
import com.pcs.vcms.entity.Vessel;
import com.pcs.vcms.entity.VesselCall;
import com.pcs.vcms.entity.VesselCall.VesselCallStatus;
import com.pcs.vcms.exception.DuplicateCallSignException;
import com.pcs.vcms.exception.EntityNotFoundException;
import com.pcs.vcms.exception.ValidationException;
import com.pcs.vcms.mapper.VesselCallMapper;
import com.pcs.vcms.repository.VesselCallRepository;
import com.pcs.vcms.repository.VesselRepository;
import com.pcs.vcms.security.SecurityContext;
import com.pcs.vcms.service.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VesselCallServiceTest {

    @Mock
    private VesselCallRepository vesselCallRepository;

    @Mock
    private VesselRepository vesselRepository;

    @Mock
    private VesselCallMapper vesselCallMapper;

    @Mock
    private NotificationService notificationService;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private VesselCallServiceImpl vesselCallService;

    @Captor
    private ArgumentCaptor<VesselCall> vesselCallCaptor;

    private VesselCallDTO testVesselCallDTO;
    private VesselCall testVesselCall;
    private Vessel testVessel;

    @BeforeEach
    void setUp() {
        testVessel = Vessel.builder()
                .id(1L)
                .imoNumber("1234567")
                .name("Test Vessel")
                .build();

        testVesselCall = VesselCall.builder()
                .id(1L)
                .vessel(testVessel)
                .callSign("TEST123")
                .status(VesselCallStatus.PLANNED)
                .eta(LocalDateTime.now().plusDays(1))
                .etd(LocalDateTime.now().plusDays(2))
                .build();

        testVesselCallDTO = VesselCallDTO.builder()
                .vesselId(1L)
                .vesselName("Test Vessel")
                .callSign("TEST123")
                .status(VesselCallStatus.PLANNED)
                .eta(LocalDateTime.now().plusDays(1))
                .etd(LocalDateTime.now().plusDays(2))
                .build();
    }

    @Nested
    @DisplayName("Create Vessel Call Tests")
    class CreateVesselCallTests {

        @Test
        @DisplayName("Should successfully create vessel call with valid data")
        void testCreateVesselCall_Success() {
            // Given
            when(securityContext.hasPermission("VESSEL_CALL_CREATE")).thenReturn(true);
            when(vesselRepository.findById(1L)).thenReturn(Optional.of(testVessel));
            when(vesselCallRepository.findByCallSign(testVesselCallDTO.getCallSign())).thenReturn(Optional.empty());
            when(vesselCallMapper.toEntity(testVesselCallDTO)).thenReturn(testVesselCall);
            when(vesselCallRepository.save(any(VesselCall.class))).thenReturn(testVesselCall);
            when(vesselCallMapper.toDTO(testVesselCall)).thenReturn(testVesselCallDTO);

            // When
            VesselCallDTO result = vesselCallService.createVesselCall(testVesselCallDTO);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getCallSign()).isEqualTo(testVesselCallDTO.getCallSign());
            verify(notificationService).sendVesselCallCreatedNotification(any());
            verify(vesselCallRepository).save(vesselCallCaptor.capture());
            assertThat(vesselCallCaptor.getValue().getStatus()).isEqualTo(VesselCallStatus.PLANNED);
        }

        @Test
        @DisplayName("Should throw exception when creating vessel call with duplicate call sign")
        void testCreateVesselCall_DuplicateCallSign() {
            // Given
            when(securityContext.hasPermission("VESSEL_CALL_CREATE")).thenReturn(true);
            when(vesselCallRepository.findByCallSign(testVesselCallDTO.getCallSign()))
                    .thenReturn(Optional.of(testVesselCall));

            // When/Then
            assertThatThrownBy(() -> vesselCallService.createVesselCall(testVesselCallDTO))
                    .isInstanceOf(DuplicateCallSignException.class)
                    .hasMessageContaining("Call sign already exists");
        }

        @Test
        @DisplayName("Should throw exception when user lacks create permission")
        void testCreateVesselCall_NoPermission() {
            // Given
            when(securityContext.hasPermission("VESSEL_CALL_CREATE")).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> vesselCallService.createVesselCall(testVesselCallDTO))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("Update Status Tests")
    class UpdateStatusTests {

        @Test
        @DisplayName("Should successfully update vessel call status")
        void testUpdateStatus_Success() {
            // Given
            VesselCall updatedCall = testVesselCall.toBuilder()
                    .status(VesselCallStatus.ARRIVED)
                    .ata(LocalDateTime.now())
                    .build();
            
            when(securityContext.hasPermission("VESSEL_CALL_UPDATE")).thenReturn(true);
            when(vesselCallRepository.findById(1L)).thenReturn(Optional.of(testVesselCall));
            when(vesselCallRepository.save(any(VesselCall.class))).thenReturn(updatedCall);
            when(vesselCallMapper.toDTO(updatedCall)).thenReturn(
                    testVesselCallDTO.toBuilder().status(VesselCallStatus.ARRIVED).build());

            // When
            VesselCallDTO result = vesselCallService.updateStatus(1L, VesselCallStatus.ARRIVED);

            // Then
            assertThat(result.getStatus()).isEqualTo(VesselCallStatus.ARRIVED);
            verify(notificationService).sendStatusUpdateNotification(any());
        }

        @Test
        @DisplayName("Should throw exception for invalid status transition")
        void testUpdateStatus_InvalidTransition() {
            // Given
            testVesselCall.setStatus(VesselCallStatus.DEPARTED);
            when(securityContext.hasPermission("VESSEL_CALL_UPDATE")).thenReturn(true);
            when(vesselCallRepository.findById(1L)).thenReturn(Optional.of(testVesselCall));

            // When/Then
            assertThatThrownBy(() -> vesselCallService.updateStatus(1L, VesselCallStatus.PLANNED))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Invalid status transition");
        }
    }

    @Nested
    @DisplayName("Find By Date Range Tests")
    class FindByDateRangeTests {

        @Test
        @DisplayName("Should return vessel calls within date range")
        void testFindByDateRange_Success() {
            // Given
            LocalDateTime startDate = LocalDateTime.now();
            LocalDateTime endDate = startDate.plusDays(7);
            PageRequest pageRequest = PageRequest.of(0, 10);
            Page<VesselCall> vesselCallPage = new PageImpl<>(List.of(testVesselCall));
            
            when(securityContext.hasPermission("VESSEL_CALL_READ")).thenReturn(true);
            when(vesselCallRepository.findByEtaBetween(startDate, endDate, pageRequest))
                    .thenReturn(vesselCallPage);
            when(vesselCallMapper.toDTO(testVesselCall)).thenReturn(testVesselCallDTO);

            // When
            Page<VesselCallDTO> result = vesselCallService.findByDateRange(startDate, endDate, pageRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getCallSign()).isEqualTo(testVesselCallDTO.getCallSign());
        }

        @Test
        @DisplayName("Should throw exception for invalid date range")
        void testFindByDateRange_InvalidRange() {
            // Given
            LocalDateTime startDate = LocalDateTime.now();
            LocalDateTime endDate = startDate.minusDays(1);
            PageRequest pageRequest = PageRequest.of(0, 10);

            // When/Then
            assertThatThrownBy(() -> 
                vesselCallService.findByDateRange(startDate, endDate, pageRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("End date must be after start date");
        }
    }
}