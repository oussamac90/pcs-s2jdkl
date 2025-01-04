package com.pcs.vcms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcs.vcms.dto.ServiceBookingDTO;
import com.pcs.vcms.entity.ServiceBooking.ServiceType;
import com.pcs.vcms.entity.ServiceBooking.ServiceStatus;
import com.pcs.vcms.service.ServiceBookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(ServiceBookingController.class)
class ServiceBookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ServiceBookingService serviceBookingService;

    @MockBean
    private WebSocketHandler webSocketHandler;

    private ObjectMapper objectMapper;
    private ServiceBookingDTO testBookingDTO;
    private final LocalDateTime testTime = LocalDateTime.now().plusDays(1);

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // For LocalDateTime serialization

        testBookingDTO = ServiceBookingDTO.builder()
                .id(1L)
                .vesselCallId(100L)
                .vesselName("Test Vessel")
                .serviceType(ServiceType.PILOTAGE)
                .status(ServiceStatus.REQUESTED)
                .quantity(1)
                .serviceTime(testTime)
                .remarks("Test booking")
                .build();
    }

    @Test
    @WithMockUser(roles = "SERVICE_BOOKING_CREATE")
    void testCreateServiceBooking_Success() throws Exception {
        when(serviceBookingService.createServiceBooking(any(ServiceBookingDTO.class)))
                .thenReturn(testBookingDTO);

        mockMvc.perform(post("/api/v1/service-bookings")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testBookingDTO)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/service-bookings/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.vesselName").value("Test Vessel"));

        verify(serviceBookingService).createServiceBooking(any(ServiceBookingDTO.class));
    }

    @Test
    @WithMockUser(roles = "SERVICE_BOOKING_READ")
    void testGetServiceBooking_Success() throws Exception {
        when(serviceBookingService.getServiceBooking(1L)).thenReturn(testBookingDTO);

        mockMvc.perform(get("/api/v1/service-bookings/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.vesselName").value("Test Vessel"));

        verify(serviceBookingService).getServiceBooking(1L);
    }

    @Test
    @WithMockUser(roles = "SERVICE_BOOKING_READ")
    void testGetServiceBookingsForVesselCall_Success() throws Exception {
        List<ServiceBookingDTO> bookings = Arrays.asList(testBookingDTO);
        Page<ServiceBookingDTO> page = new PageImpl<>(bookings);

        when(serviceBookingService.getServiceBookingsForVesselCall(eq(100L), any(PageRequest.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/service-bookings/vessel-call/100")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(serviceBookingService).getServiceBookingsForVesselCall(eq(100L), any(PageRequest.class));
    }

    @Test
    @WithMockUser(roles = "SERVICE_BOOKING_READ")
    void testGetServiceBookingsByTypeAndStatus_Success() throws Exception {
        List<ServiceBookingDTO> bookings = Arrays.asList(testBookingDTO);
        Page<ServiceBookingDTO> page = new PageImpl<>(bookings);

        when(serviceBookingService.getServiceBookingsByTypeAndStatus(
                eq(ServiceType.PILOTAGE), eq(ServiceStatus.REQUESTED), any(PageRequest.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/service-bookings/search")
                .param("serviceType", "PILOTAGE")
                .param("status", "REQUESTED")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].serviceType").value("PILOTAGE"))
                .andExpect(jsonPath("$.content[0].status").value("REQUESTED"));

        verify(serviceBookingService).getServiceBookingsByTypeAndStatus(
                eq(ServiceType.PILOTAGE), eq(ServiceStatus.REQUESTED), any(PageRequest.class));
    }

    @Test
    @WithMockUser(roles = "SERVICE_BOOKING_CANCEL")
    void testCancelServiceBooking_Success() throws Exception {
        ServiceBookingDTO cancelledBooking = ServiceBookingDTO.builder()
                .id(1L)
                .status(ServiceStatus.CANCELLED)
                .build();

        when(serviceBookingService.cancelServiceBooking(eq(1L), anyString()))
                .thenReturn(cancelledBooking);

        mockMvc.perform(put("/api/v1/service-bookings/1/cancel")
                .with(csrf())
                .param("reason", "Test cancellation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(serviceBookingService).cancelServiceBooking(1L, "Test cancellation");
    }

    @Test
    @WithMockUser(roles = "SERVICE_BOOKING_UPDATE")
    void testUpdateServiceBookingStatus_Success() throws Exception {
        ServiceBookingDTO updatedBooking = ServiceBookingDTO.builder()
                .id(1L)
                .status(ServiceStatus.CONFIRMED)
                .build();

        when(serviceBookingService.updateServiceBookingStatus(eq(1L), eq(ServiceStatus.CONFIRMED)))
                .thenReturn(updatedBooking);

        mockMvc.perform(put("/api/v1/service-bookings/1/status")
                .with(csrf())
                .param("newStatus", "CONFIRMED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        verify(serviceBookingService).updateServiceBookingStatus(1L, ServiceStatus.CONFIRMED);
    }

    @Test
    @WithMockUser(roles = "SERVICE_BOOKING_CREATE")
    void testCreateServiceBooking_ValidationFailure() throws Exception {
        ServiceBookingDTO invalidBooking = ServiceBookingDTO.builder()
                .vesselCallId(null) // Required field
                .build();

        mockMvc.perform(post("/api/v1/service-bookings")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidBooking)))
                .andExpect(status().isBadRequest());

        verify(serviceBookingService, never()).createServiceBooking(any());
    }

    @Test
    void testEndpoint_UnauthorizedAccess() throws Exception {
        mockMvc.perform(get("/api/v1/service-bookings/1"))
                .andExpect(status().isUnauthorized());

        verify(serviceBookingService, never()).getServiceBooking(anyLong());
    }

    @Test
    @WithMockUser(roles = "SERVICE_BOOKING_READ")
    void testGetServiceBooking_NotFound() throws Exception {
        when(serviceBookingService.getServiceBooking(999L))
                .thenThrow(new ResourceNotFoundException("Service booking not found"));

        mockMvc.perform(get("/api/v1/service-bookings/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "SERVICE_BOOKING_CREATE")
    void testCreateServiceBooking_ConflictingResource() throws Exception {
        when(serviceBookingService.createServiceBooking(any()))
                .thenThrow(new ResourceConflictException("Resource conflict"));

        mockMvc.perform(post("/api/v1/service-bookings")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testBookingDTO)))
                .andExpect(status().isConflict());
    }
}