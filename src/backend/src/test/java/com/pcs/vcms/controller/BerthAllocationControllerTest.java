package com.pcs.vcms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcs.vcms.dto.BerthAllocationDTO;
import com.pcs.vcms.entity.BerthAllocation.BerthAllocationStatus;
import com.pcs.vcms.service.BerthAllocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive test suite for BerthAllocationController that verifies REST API endpoints,
 * security constraints, performance requirements, and business rules.
 *
 * @version 1.0
 * @since 2023-11-15
 */
@WebMvcTest(BerthAllocationController.class)
@WithMockUser(roles = {"BERTH_OPERATOR"})
public class BerthAllocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BerthAllocationService berthAllocationService;

    @Autowired
    private ObjectMapper objectMapper;

    private StopWatch stopWatch;
    private BerthAllocationDTO testAllocation;

    @BeforeEach
    void setUp() {
        stopWatch = new StopWatch();
        testAllocation = BerthAllocationDTO.builder()
                .id(1L)
                .vesselCallId(100L)
                .vesselName("Test Vessel")
                .berthId(200L)
                .berthName("Test Berth")
                .startTime(LocalDateTime.now().plusHours(1))
                .endTime(LocalDateTime.now().plusHours(3))
                .status(BerthAllocationStatus.SCHEDULED)
                .build();
    }

    @Test
    void testCreateBerthAllocation_Success() throws Exception {
        // Given
        when(berthAllocationService.createBerthAllocation(any(BerthAllocationDTO.class)))
                .thenReturn(testAllocation);

        // When
        stopWatch.start();
        MvcResult result = mockMvc.perform(post("/api/v1/berth-allocations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testAllocation)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(testAllocation.getId()))
                .andExpect(jsonPath("$.vesselName").value(testAllocation.getVesselName()))
                .andReturn();
        stopWatch.stop();

        // Then
        assertThat(stopWatch.getTotalTimeSeconds()).isLessThan(3.0);
        verify(berthAllocationService).createBerthAllocation(any(BerthAllocationDTO.class));
    }

    @Test
    void testCreateBerthAllocation_Conflict() throws Exception {
        // Given
        when(berthAllocationService.checkAllocationConflicts(any(BerthAllocationDTO.class)))
                .thenReturn(Arrays.asList(testAllocation));

        // When
        mockMvc.perform(post("/api/v1/berth-allocations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testAllocation)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testUpdateBerthAllocation_Success() throws Exception {
        // Given
        when(berthAllocationService.updateBerthAllocation(eq(1L), any(BerthAllocationDTO.class)))
                .thenReturn(testAllocation);

        // When
        stopWatch.start();
        MvcResult result = mockMvc.perform(put("/api/v1/berth-allocations/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testAllocation)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        stopWatch.stop();

        // Then
        assertThat(stopWatch.getTotalTimeSeconds()).isLessThan(3.0);
    }

    @Test
    void testGetBerthAllocation_Success() throws Exception {
        // Given
        when(berthAllocationService.getBerthAllocation(1L))
                .thenReturn(Optional.of(testAllocation));

        // When
        stopWatch.start();
        MvcResult result = mockMvc.perform(get("/api/v1/berth-allocations/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        stopWatch.stop();

        // Then
        assertThat(stopWatch.getTotalTimeSeconds()).isLessThan(3.0);
    }

    @Test
    void testGetBerthAllocation_NotFound() throws Exception {
        // Given
        when(berthAllocationService.getBerthAllocation(1L))
                .thenReturn(Optional.empty());

        // When
        mockMvc.perform(get("/api/v1/berth-allocations/{id}", 1L))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = {"PORT_ADMIN"})
    void testGetAllocationsByVesselCall_Success() throws Exception {
        // Given
        List<BerthAllocationDTO> allocations = Arrays.asList(testAllocation);
        when(berthAllocationService.getAllocationsByVesselCall(100L))
                .thenReturn(allocations);

        // When
        stopWatch.start();
        MvcResult result = mockMvc.perform(get("/api/v1/berth-allocations/vessel-call/{vesselCallId}", 100L))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].vesselCallId").value(100L))
                .andReturn();
        stopWatch.stop();

        // Then
        assertThat(stopWatch.getTotalTimeSeconds()).isLessThan(3.0);
    }

    @Test
    void testCancelBerthAllocation_Success() throws Exception {
        // When
        stopWatch.start();
        mockMvc.perform(delete("/api/v1/berth-allocations/{id}", 1L))
                .andExpect(status().isNoContent());
        stopWatch.stop();

        // Then
        assertThat(stopWatch.getTotalTimeSeconds()).isLessThan(3.0);
        verify(berthAllocationService).cancelBerthAllocation(1L);
    }

    @Test
    void testCheckAllocationConflicts_Success() throws Exception {
        // Given
        List<BerthAllocationDTO> conflicts = Arrays.asList(testAllocation);
        when(berthAllocationService.checkAllocationConflicts(any(BerthAllocationDTO.class)))
                .thenReturn(conflicts);

        // When
        stopWatch.start();
        MvcResult result = mockMvc.perform(get("/api/v1/berth-allocations/conflicts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testAllocation)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        stopWatch.stop();

        // Then
        assertThat(stopWatch.getTotalTimeSeconds()).isLessThan(3.0);
    }

    @Test
    @WithMockUser(roles = {"UNAUTHORIZED"})
    void testUnauthorizedAccess() throws Exception {
        mockMvc.perform(get("/api/v1/berth-allocations/{id}", 1L))
                .andExpect(status().isForbidden());
    }

    @Test
    void testInvalidInput_BadRequest() throws Exception {
        // Given
        testAllocation.setStartTime(null); // Invalid input

        // When
        mockMvc.perform(post("/api/v1/berth-allocations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testAllocation)))
                .andExpect(status().isBadRequest());
    }
}