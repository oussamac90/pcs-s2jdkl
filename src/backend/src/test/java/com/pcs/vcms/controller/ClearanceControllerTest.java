package com.pcs.vcms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcs.vcms.dto.ClearanceDTO;
import com.pcs.vcms.entity.Clearance.ClearanceType;
import com.pcs.vcms.entity.Clearance.ClearanceStatus;
import com.pcs.vcms.service.ClearanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ClearanceControllerTest {

    private static final String API_BASE_PATH = "/api/v1/clearances";
    private static final Long TEST_CLEARANCE_ID = 1L;
    private static final Long TEST_VESSEL_CALL_ID = 100L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ClearanceService clearanceService;

    private ClearanceDTO testClearanceDTO;

    @BeforeEach
    void setUp() {
        reset(clearanceService);

        testClearanceDTO = ClearanceDTO.builder()
                .id(TEST_CLEARANCE_ID)
                .vesselCallId(TEST_VESSEL_CALL_ID)
                .vesselName("Test Vessel")
                .type(ClearanceType.CUSTOMS)
                .status(ClearanceStatus.PENDING)
                .referenceNumber("CLR-123456-ABC")
                .submittedBy("test-user")
                .submittedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @WithMockUser(roles = "CLEARANCE_SUBMIT")
    void testSubmitClearance_Success() throws Exception {
        when(clearanceService.submitClearance(any(ClearanceDTO.class)))
                .thenReturn(testClearanceDTO);

        mockMvc.perform(post(API_BASE_PATH)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testClearanceDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(TEST_CLEARANCE_ID))
                .andExpect(jsonPath("$.vesselCallId").value(TEST_VESSEL_CALL_ID))
                .andExpect(jsonPath("$.type").value(testClearanceDTO.getType().toString()));

        verify(clearanceService).submitClearance(any(ClearanceDTO.class));
    }

    @Test
    @WithMockUser(roles = "CLEARANCE_SUBMIT")
    void testSubmitClearance_ValidationFailure() throws Exception {
        testClearanceDTO.setVesselCallId(null); // Invalid DTO

        mockMvc.perform(post(API_BASE_PATH)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testClearanceDTO)))
                .andExpect(status().isBadRequest());

        verify(clearanceService, never()).submitClearance(any(ClearanceDTO.class));
    }

    @Test
    @WithMockUser(roles = "CLEARANCE_UPDATE")
    void testUpdateClearanceStatus_Success() throws Exception {
        testClearanceDTO.setStatus(ClearanceStatus.APPROVED);
        when(clearanceService.updateClearanceStatus(eq(TEST_CLEARANCE_ID), 
                eq(ClearanceStatus.APPROVED), any()))
                .thenReturn(testClearanceDTO);

        mockMvc.perform(put(API_BASE_PATH + "/{id}/status", TEST_CLEARANCE_ID)
                .with(csrf())
                .param("status", "APPROVED")
                .param("remarks", "Approved after review"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        verify(clearanceService).updateClearanceStatus(eq(TEST_CLEARANCE_ID), 
                eq(ClearanceStatus.APPROVED), any());
    }

    @Test
    @WithMockUser(roles = "CLEARANCE_VIEW")
    void testGetClearanceById_Success() throws Exception {
        when(clearanceService.getClearanceById(TEST_CLEARANCE_ID))
                .thenReturn(Optional.of(testClearanceDTO));

        mockMvc.perform(get(API_BASE_PATH + "/{id}", TEST_CLEARANCE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TEST_CLEARANCE_ID));

        verify(clearanceService).getClearanceById(TEST_CLEARANCE_ID);
    }

    @Test
    @WithMockUser(roles = "CLEARANCE_VIEW")
    void testGetClearanceById_NotFound() throws Exception {
        when(clearanceService.getClearanceById(TEST_CLEARANCE_ID))
                .thenReturn(Optional.empty());

        mockMvc.perform(get(API_BASE_PATH + "/{id}", TEST_CLEARANCE_ID))
                .andExpect(status().isNotFound());

        verify(clearanceService).getClearanceById(TEST_CLEARANCE_ID);
    }

    @Test
    @WithMockUser(roles = "CLEARANCE_VIEW")
    void testGetClearancesByVesselCall_Success() throws Exception {
        when(clearanceService.getClearancesByVesselCall(TEST_VESSEL_CALL_ID))
                .thenReturn(Arrays.asList(testClearanceDTO));

        mockMvc.perform(get(API_BASE_PATH + "/vessel-call/{vesselCallId}", TEST_VESSEL_CALL_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].vesselCallId").value(TEST_VESSEL_CALL_ID));

        verify(clearanceService).getClearancesByVesselCall(TEST_VESSEL_CALL_ID);
    }

    @Test
    @WithMockUser(roles = "CLEARANCE_VIEW")
    void testGetClearances_WithFilters() throws Exception {
        Page<ClearanceDTO> page = new PageImpl<>(Arrays.asList(testClearanceDTO));
        when(clearanceService.getClearances(any(), any(), any(), any()))
                .thenReturn(page);

        mockMvc.perform(get(API_BASE_PATH)
                .param("page", "0")
                .param("size", "10")
                .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(TEST_CLEARANCE_ID));

        verify(clearanceService).getClearances(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "CLEARANCE_CANCEL")
    void testCancelClearance_Success() throws Exception {
        testClearanceDTO.setStatus(ClearanceStatus.CANCELLED);
        when(clearanceService.cancelClearance(eq(TEST_CLEARANCE_ID), any()))
                .thenReturn(testClearanceDTO);

        mockMvc.perform(post(API_BASE_PATH + "/{id}/cancel", TEST_CLEARANCE_ID)
                .with(csrf())
                .param("remarks", "Cancelled due to vessel schedule change"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(clearanceService).cancelClearance(eq(TEST_CLEARANCE_ID), any());
    }

    @Test
    @WithMockUser(roles = "CLEARANCE_VALIDATE")
    void testValidateDepartureClearances_Success() throws Exception {
        when(clearanceService.validateDepartureClearances(TEST_VESSEL_CALL_ID))
                .thenReturn(true);

        mockMvc.perform(get(API_BASE_PATH + "/validate-departure/{vesselCallId}", 
                TEST_VESSEL_CALL_ID))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(clearanceService).validateDepartureClearances(TEST_VESSEL_CALL_ID);
    }

    @Test
    void testUnauthorizedAccess() throws Exception {
        mockMvc.perform(get(API_BASE_PATH + "/{id}", TEST_CLEARANCE_ID))
                .andExpect(status().isUnauthorized());

        verify(clearanceService, never()).getClearanceById(any());
    }

    @Test
    @WithMockUser(roles = "INVALID_ROLE")
    void testInsufficientPermissions() throws Exception {
        mockMvc.perform(get(API_BASE_PATH + "/{id}", TEST_CLEARANCE_ID))
                .andExpect(status().isForbidden());

        verify(clearanceService, never()).getClearanceById(any());
    }
}