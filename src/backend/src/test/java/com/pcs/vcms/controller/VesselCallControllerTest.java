package com.pcs.vcms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcs.vcms.dto.VesselCallDTO;
import com.pcs.vcms.entity.VesselCall.VesselCallStatus;
import com.pcs.vcms.service.VesselCallService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VesselCallController.class)
class VesselCallControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VesselCallService vesselCallService;

    private Clock fixedClock;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.of(2023, 11, 15, 10, 0);
        fixedClock = Clock.fixed(now.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        objectMapper.findAndRegisterModules();
    }

    private VesselCallDTO createTestVesselCallDTO() {
        return VesselCallDTO.builder()
                .id(1L)
                .vesselId(100L)
                .vesselName("Test Vessel")
                .imoNumber("IMO1234567")
                .callSign("TESTCALL")
                .status(VesselCallStatus.PLANNED)
                .eta(now.plusDays(1))
                .etd(now.plusDays(2))
                .build();
    }

    @Test
    @WithMockUser(roles = "VESSEL_AGENT")
    void testCreateVesselCall_Success() throws Exception {
        VesselCallDTO testDTO = createTestVesselCallDTO();
        when(vesselCallService.createVesselCall(any(VesselCallDTO.class))).thenReturn(testDTO);

        mockMvc.perform(post("/api/v1/vessel-calls")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(testDTO.getId()))
                .andExpect(jsonPath("$.callSign").value(testDTO.getCallSign()))
                .andExpect(jsonPath("$.status").value(testDTO.getStatus().toString()));
    }

    @Test
    @WithMockUser(roles = "VESSEL_AGENT")
    void testCreateVesselCall_ValidationFailure() throws Exception {
        VesselCallDTO invalidDTO = VesselCallDTO.builder()
                .vesselId(100L)
                .callSign("") // Invalid: empty call sign
                .build();

        mockMvc.perform(post("/api/v1/vessel-calls")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @WithMockUser(roles = "PORT_AUTHORITY")
    void testUpdateVesselCall_Success() throws Exception {
        VesselCallDTO testDTO = createTestVesselCallDTO();
        when(vesselCallService.updateVesselCall(eq(1L), any(VesselCallDTO.class))).thenReturn(testDTO);

        mockMvc.perform(put("/api/v1/vessel-calls/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testDTO.getId()));
    }

    @Test
    @WithMockUser(roles = "SERVICE_PROVIDER")
    void testGetVesselCall_Success() throws Exception {
        VesselCallDTO testDTO = createTestVesselCallDTO();
        when(vesselCallService.getVesselCall(1L)).thenReturn(Optional.of(testDTO));

        mockMvc.perform(get("/api/v1/vessel-calls/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testDTO.getId()));
    }

    @Test
    @WithMockUser(roles = "SERVICE_PROVIDER")
    void testGetVesselCall_NotFound() throws Exception {
        when(vesselCallService.getVesselCall(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/vessel-calls/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "PORT_AUTHORITY")
    void testFindByStatus_Success() throws Exception {
        VesselCallDTO testDTO = createTestVesselCallDTO();
        Page<VesselCallDTO> page = new PageImpl<>(Arrays.asList(testDTO));
        
        when(vesselCallService.findByStatus(eq(VesselCallStatus.PLANNED), any(PageRequest.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/vessel-calls/status/PLANNED")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(testDTO.getId()));
    }

    @Test
    @WithMockUser(roles = "PORT_AUTHORITY")
    void testFindByDateRange_Success() throws Exception {
        VesselCallDTO testDTO = createTestVesselCallDTO();
        Page<VesselCallDTO> page = new PageImpl<>(Arrays.asList(testDTO));
        
        when(vesselCallService.findByDateRange(any(), any(), any(PageRequest.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/vessel-calls/date-range")
                .param("startDate", now.toString())
                .param("endDate", now.plusDays(7).toString())
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(testDTO.getId()));
    }

    @Test
    @WithMockUser(roles = "PORT_AUTHORITY")
    void testUpdateStatus_Success() throws Exception {
        VesselCallDTO testDTO = createTestVesselCallDTO();
        testDTO.setStatus(VesselCallStatus.ARRIVED);
        
        when(vesselCallService.updateStatus(eq(1L), eq(VesselCallStatus.ARRIVED)))
                .thenReturn(testDTO);

        mockMvc.perform(patch("/api/v1/vessel-calls/1/status")
                .with(csrf())
                .param("status", "ARRIVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARRIVED"));
    }

    @Test
    @WithMockUser(roles = "PORT_AUTHORITY")
    void testCancelVesselCall_Success() throws Exception {
        VesselCallDTO testDTO = createTestVesselCallDTO();
        testDTO.setStatus(VesselCallStatus.CANCELLED);
        
        when(vesselCallService.cancelVesselCall(eq(1L), any(String.class)))
                .thenReturn(testDTO);

        mockMvc.perform(delete("/api/v1/vessel-calls/1")
                .with(csrf())
                .param("reason", "Weather conditions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void testUnauthorizedAccess() throws Exception {
        mockMvc.perform(get("/api/v1/vessel-calls/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "INVALID_ROLE")
    void testInvalidRole_AccessDenied() throws Exception {
        mockMvc.perform(get("/api/v1/vessel-calls/1"))
                .andExpect(status().isForbidden());
    }
}