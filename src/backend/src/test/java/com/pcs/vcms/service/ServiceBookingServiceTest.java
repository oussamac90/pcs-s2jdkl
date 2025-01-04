package com.pcs.vcms.service;

import com.pcs.vcms.audit.AuditService;
import com.pcs.vcms.dto.ServiceBookingDTO;
import com.pcs.vcms.entity.ServiceBooking;
import com.pcs.vcms.entity.ServiceBooking.ServiceType;
import com.pcs.vcms.entity.ServiceBooking.ServiceStatus;
import com.pcs.vcms.entity.VesselCall;
import com.pcs.vcms.exception.InsufficientResourcesException;
import com.pcs.vcms.exception.ResourceConflictException;
import com.pcs.vcms.exception.ResourceNotFoundException;
import com.pcs.vcms.repository.ServiceBookingRepository;
import com.pcs.vcms.repository.VesselCallRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.support.RetryTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceBookingServiceTest {

    @Mock
    private ServiceBookingRepository serviceBookingRepository;

    @Mock
    private VesselCallRepository vesselCallRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuditService auditService;

    @Mock
    private RetryTemplate retryTemplate;

    @InjectMocks
    private ServiceBookingServiceImpl serviceBookingService;

    @Captor
    private ArgumentCaptor<ServiceBooking> serviceBookingCaptor;

    private ServiceBookingDTO testBookingDTO;
    private VesselCall testVesselCall;
    private ServiceBooking testServiceBooking;
    private final LocalDateTime testServiceTime = LocalDateTime.now().plusHours(2);

    @BeforeEach
    void setUp() {
        testVesselCall = VesselCall.builder()
            .id(1L)
            .callSign("TEST-123")
            .eta(LocalDateTime.now())
            .etd(LocalDateTime.now().plusDays(1))
            .status(VesselCall.VesselCallStatus.PLANNED)
            .build();

        testBookingDTO = ServiceBookingDTO.builder()
            .vesselCallId(1L)
            .serviceType(ServiceType.TUGBOAT)
            .quantity(2)
            .serviceTime(testServiceTime)
            .remarks("Test booking")
            .build();

        testServiceBooking = ServiceBooking.builder()
            .id(1L)
            .vesselCall(testVesselCall)
            .serviceType(ServiceType.TUGBOAT)
            .status(ServiceStatus.REQUESTED)
            .quantity(2)
            .serviceTime(testServiceTime)
            .remarks("Test booking")
            .build();
    }

    @Test
    @DisplayName("Should successfully create service booking with resource check")
    void testCreateServiceBookingSuccess() {
        // Arrange
        when(vesselCallRepository.findById(1L)).thenReturn(Optional.of(testVesselCall));
        when(serviceBookingRepository.save(any(ServiceBooking.class))).thenReturn(testServiceBooking);
        when(serviceBookingService.checkResourceAvailability(any(), any(), any())).thenReturn(true);

        // Act
        ServiceBookingDTO result = serviceBookingService.createServiceBooking(testBookingDTO);

        // Assert
        assertNotNull(result);
        assertEquals(ServiceType.TUGBOAT, result.getServiceType());
        assertEquals(2, result.getQuantity());
        assertEquals(testServiceTime, result.getServiceTime());

        verify(serviceBookingRepository).save(serviceBookingCaptor.capture());
        verify(notificationService).sendServiceStatusUpdate(any(ServiceBooking.class));
        verify(auditService).logServiceBookingCreation(any(), any());
    }

    @Test
    @DisplayName("Should throw exception when resources are unavailable")
    void testCreateServiceBookingInsufficientResources() {
        // Arrange
        when(vesselCallRepository.findById(1L)).thenReturn(Optional.of(testVesselCall));
        when(serviceBookingService.checkResourceAvailability(any(), any(), any())).thenReturn(false);

        // Act & Assert
        assertThrows(InsufficientResourcesException.class, () -> 
            serviceBookingService.createServiceBooking(testBookingDTO));
        
        verify(serviceBookingRepository, never()).save(any());
        verify(notificationService, never()).sendServiceStatusUpdate(any());
    }

    @Test
    @DisplayName("Should handle concurrent booking conflicts with retry")
    void testConcurrentBookingConflictHandling() {
        // Arrange
        when(vesselCallRepository.findById(1L)).thenReturn(Optional.of(testVesselCall));
        when(serviceBookingService.checkResourceAvailability(any(), any(), any())).thenReturn(true);
        when(serviceBookingRepository.save(any(ServiceBooking.class)))
            .thenThrow(OptimisticLockingFailureException.class)
            .thenReturn(testServiceBooking);

        doAnswer(invocation -> {
            return invocation.getArgument(0);
        }).when(retryTemplate).execute(any(), any());

        // Act
        ServiceBookingDTO result = serviceBookingService.createServiceBooking(testBookingDTO);

        // Assert
        assertNotNull(result);
        verify(serviceBookingRepository, times(2)).save(any(ServiceBooking.class));
        verify(auditService).logConcurrencyConflict(any(), any());
    }

    @Test
    @DisplayName("Should successfully update service booking status")
    void testUpdateServiceBookingStatus() {
        // Arrange
        when(serviceBookingRepository.findById(1L)).thenReturn(Optional.of(testServiceBooking));
        when(serviceBookingRepository.save(any(ServiceBooking.class))).thenReturn(testServiceBooking);

        // Act
        ServiceBookingDTO result = serviceBookingService.updateServiceBookingStatus(1L, ServiceStatus.CONFIRMED);

        // Assert
        assertNotNull(result);
        assertEquals(ServiceStatus.CONFIRMED, result.getStatus());
        verify(notificationService).sendServiceStatusUpdate(any(ServiceBooking.class));
        verify(auditService).logServiceBookingStatusUpdate(any(), any(), any());
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent booking")
    void testUpdateServiceBookingStatusNotFound() {
        // Arrange
        when(serviceBookingRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
            serviceBookingService.updateServiceBookingStatus(1L, ServiceStatus.CONFIRMED));
        
        verify(serviceBookingRepository, never()).save(any());
        verify(notificationService, never()).sendServiceStatusUpdate(any());
    }

    @Test
    @DisplayName("Should successfully cancel service booking")
    void testCancelServiceBooking() {
        // Arrange
        testServiceBooking.setStatus(ServiceStatus.CONFIRMED);
        when(serviceBookingRepository.findById(1L)).thenReturn(Optional.of(testServiceBooking));
        when(serviceBookingRepository.save(any(ServiceBooking.class))).thenReturn(testServiceBooking);

        // Act
        ServiceBookingDTO result = serviceBookingService.cancelServiceBooking(1L, "Weather conditions");

        // Assert
        assertNotNull(result);
        assertEquals(ServiceStatus.CANCELLED, result.getStatus());
        verify(notificationService).sendServiceStatusUpdate(any(ServiceBooking.class));
        verify(auditService).logServiceBookingCancellation(any(), any(), any());
    }

    @Test
    @DisplayName("Should validate service time within vessel call window")
    void testValidateServiceTimeWindow() {
        // Arrange
        testBookingDTO.setServiceTime(testVesselCall.getEtd().plusHours(1));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            serviceBookingService.createServiceBooking(testBookingDTO));
        
        verify(serviceBookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should check resource availability correctly")
    void testCheckResourceAvailability() {
        // Arrange
        when(serviceBookingRepository.countConflictingBookings(
            any(ServiceType.class), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(0L);

        // Act
        boolean result = serviceBookingService.checkResourceAvailability(
            ServiceType.TUGBOAT, testServiceTime, 2);

        // Assert
        assertTrue(result);
        verify(serviceBookingRepository).countConflictingBookings(
            eq(ServiceType.TUGBOAT), any(LocalDateTime.class), any(LocalDateTime.class));
    }
}