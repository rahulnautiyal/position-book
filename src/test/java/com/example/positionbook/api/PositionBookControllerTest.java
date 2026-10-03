package com.example.positionbook.api;

import com.example.positionbook.api.controller.PositionBookController;
import com.example.positionbook.domain.Position;
import com.example.positionbook.domain.TradeEvent;
import com.example.positionbook.exception.BadTradeEventException;
import com.example.positionbook.exception.ConflictException;
import com.example.positionbook.exception.ResourceNotFoundException;
import com.example.positionbook.service.PositionBookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PositionBookController.class)
class PositionBookControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean PositionBookService service;

    @Test
    void acceptsValidTradeEventAndReturnsCreated() throws Exception {
        mvc.perform(post("/api/v1/trade-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\":1,\"type\":\"BUY\",\"tradingAccount\":\"ACC1\",\"securityId\":\"SEC1\",\"quantity\":100}"))
                .andExpect(status().isCreated());
    }

    @Test
    void acceptsCancelWithoutMeaninglessFields() throws Exception {
        mvc.perform(post("/api/v1/trade-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\":1,\"type\":\"CANCEL\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void rejectsInvalidBuyRequestWithProblemDetails() throws Exception {
        mvc.perform(post("/api/v1/trade-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\":0,\"type\":\"BUY\",\"tradingAccount\":\"\",\"securityId\":\"SEC1\",\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        mvc.perform(post("/api/v1/trade-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void returnsConflictForDuplicateTrade() throws Exception {
        doThrow(new ConflictException("Trade event ID already exists: 1"))
                .when(service).process(any(TradeEvent.class));

        mvc.perform(post("/api/v1/trade-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\":1,\"type\":\"BUY\",\"tradingAccount\":\"ACC1\",\"securityId\":\"SEC1\",\"quantity\":100}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TRADE_EVENT_CONFLICT"));
    }

    @Test
    void returnsBadRequestForBusinessValidationFailure() throws Exception {
        doThrow(new BadTradeEventException("Cannot cancel unknown event ID: 99"))
                .when(service).process(any(TradeEvent.class));

        mvc.perform(post("/api/v1/trade-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\":99,\"type\":\"CANCEL\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TRADE_EVENT"));
    }

    @Test
    void returnsPosition() throws Exception {
        when(service.getPosition("ACC1", "SEC1")).thenReturn(new Position("ACC1", "SEC1", 150));

        mvc.perform(get("/api/v1/positions/ACC1/SEC1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.tradingAccount").value("ACC1"))
                .andExpect(jsonPath("$.securityId").value("SEC1"))
                .andExpect(jsonPath("$.quantity").value(150));
    }

    @Test
    void returnsNotFoundAsProblemDetails() throws Exception {
        when(service.getPosition("ACC1", "SEC1"))
                .thenThrow(new ResourceNotFoundException("Position not found"));

        mvc.perform(get("/api/v1/positions/ACC1/SEC1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POSITION_NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void rejectsOversizedPathParameter() throws Exception {
        String account = "A".repeat(101);
        mvc.perform(get("/api/v1/positions/{account}/SEC1", account))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void cancelPayloadIsNormalizedBeforeService() throws Exception {
        mvc.perform(post("/api/v1/trade-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\":21,\"type\":\"CANCEL\",\"tradingAccount\":\"  ignored  \",\"securityId\":\" ignored \",\"quantity\":999}"))
                .andExpect(status().isCreated());
    }
}
