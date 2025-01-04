package com.pcs.vcms.controller;

import com.pcs.vcms.dto.PreArrivalNotificationDTO;
import com.pcs.vcms.service.PreArrivalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.ValidationException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for PreArrivalController that verifies all REST endpoints
 * with proper security context, validation scenarios, and error handling.
 */
@ExtendWith(MockitoExtension.class)
class PreArrivalControllerTest {

    @Mock
    private PreArrivalService preArrivalService;

    @InjectMocks
    private PreArrivalController preArrivalController;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    private PreArrivalNotificationDTO testNotification;

    @BeforeEach
    void setUp() {
        // Setup security context
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.getName()).thenReturn("testUser");

        // Setup test notification
        testNotification = PreArrivalNotificationDTO.builder()
                .id(1L)
                .vesselCallId(100L)
                .submittedBy("testUser")
                .cargoDetails("Test Cargo")
                .crewList("Test Crew")
                .submittedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testSubmitPreArrivalNotification_Success() {
        // Arrange
        when(preArrivalService.submitPreArrivalNotification(any(PreArrivalNotificationDTO.class)))
                .thenReturn(testNotification);

        // Act
        ResponseEntity<PreArrivalNotificationDTO> response = 
                preArrivalController.submitPreArrivalNotification(testNotification);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(testNotification, response.getBody());
        verify(preArrivalService, times(1)).submitPreArrivalNotification(testNotification);
    }

    @Test
    void testSubmitPreArrivalNotification_ValidationFailure() {
        // Arrange
        when(preArrivalService.submitPreArrivalNotification(any(PreArrivalNotificationDTO.class)))
                .thenThrow(new ValidationException("Invalid notification data"));

        // Act & Assert
        Exception exception = assertThrows(ValidationException.class, () -> 
                preArrivalController.submitPreArrivalNotification(testNotification));
        assertEquals("Invalid notification data", exception.getMessage());
        verify(preArrivalService, times(1)).submitPreArrivalNotification(testNotification);
    }

    @Test
    void testGetPreArrivalNotification_Found() {
        // Arrange
        when(preArrivalService.getPreArrivalNotification(1L))
                .thenReturn(Optional.of(testNotification));

        // Act
        ResponseEntity<PreArrivalNotificationDTO> response = 
                preArrivalController.getPreArrivalNotification(1L);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testNotification, response.getBody());
        verify(preArrivalService, times(1)).getPreArrivalNotification(1L);
    }

    @Test
    void testGetPreArrivalNotification_NotFound() {
        // Arrange
        when(preArrivalService.getPreArrivalNotification(1L))
                .thenReturn(Optional.empty());

        // Act
        ResponseEntity<PreArrivalNotificationDTO> response = 
                preArrivalController.getPreArrivalNotification(1L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(preArrivalService, times(1)).getPreArrivalNotification(1L);
    }

    @Test
    void testGetPreArrivalNotificationsByVesselCall_Success() {
        // Arrange
        List<PreArrivalNotificationDTO> notifications = new ArrayList<>();
        notifications.add(testNotification);
        when(preArrivalService.getPreArrivalNotificationsByVesselCall(100L))
                .thenReturn(notifications);

        // Act
        ResponseEntity<List<PreArrivalNotificationDTO>> response = 
                preArrivalController.getPreArrivalNotificationsByVesselCall(100L);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(testNotification, response.getBody().get(0));
        verify(preArrivalService, times(1)).getPreArrivalNotificationsByVesselCall(100L);
    }

    @Test
    void testGetPreArrivalNotificationsByVesselCall_EmptyList() {
        // Arrange
        when(preArrivalService.getPreArrivalNotificationsByVesselCall(100L))
                .thenReturn(new ArrayList<>());

        // Act
        ResponseEntity<List<PreArrivalNotificationDTO>> response = 
                preArrivalController.getPreArrivalNotificationsByVesselCall(100L);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
        verify(preArrivalService, times(1)).getPreArrivalNotificationsByVesselCall(100L);
    }

    @Test
    void testGetLatestPreArrivalNotification_Found() {
        // Arrange
        List<PreArrivalNotificationDTO> notifications = new ArrayList<>();
        notifications.add(testNotification);
        when(preArrivalService.getPreArrivalNotificationsByVesselCall(100L))
                .thenReturn(notifications);

        // Act
        ResponseEntity<PreArrivalNotificationDTO> response = 
                preArrivalController.getLatestPreArrivalNotification(100L);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testNotification, response.getBody());
        verify(preArrivalService, times(1)).getPreArrivalNotificationsByVesselCall(100L);
    }

    @Test
    void testGetLatestPreArrivalNotification_NotFound() {
        // Arrange
        when(preArrivalService.getPreArrivalNotificationsByVesselCall(100L))
                .thenReturn(new ArrayList<>());

        // Act
        ResponseEntity<PreArrivalNotificationDTO> response = 
                preArrivalController.getLatestPreArrivalNotification(100L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(preArrivalService, times(1)).getPreArrivalNotificationsByVesselCall(100L);
    }

    @Test
    void testSubmitPreArrivalNotification_UnauthorizedAccess() {
        // Arrange
        when(authentication.getName()).thenReturn(null);
        testNotification.setSubmittedBy("differentUser");

        // Act & Assert
        Exception exception = assertThrows(ResponseStatusException.class, () ->
                preArrivalController.submitPreArrivalNotification(testNotification));
        assertTrue(exception.getMessage().contains("401"));
        verify(preArrivalService, never()).submitPreArrivalNotification(any());
    }

    @Test
    void testSubmitPreArrivalNotification_NullNotification() {
        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                preArrivalController.submitPreArrivalNotification(null));
        assertEquals("Pre-arrival notification cannot be null", exception.getMessage());
        verify(preArrivalService, never()).submitPreArrivalNotification(any());
    }
}