package com.example.positionbook;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PositionBookIntegrationTest {
    @Autowired MockMvc mvc;

    @Test
    void suppliedCancellationScenarioWorksEndToEnd() throws Exception {
        mvc.perform(post("/api/v1/trade-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\":21,\"type\":\"BUY\",\"tradingAccount\":\"ACC1\",\"securityId\":\"SEC1\",\"quantity\":100}"))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/trade-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\":21,\"type\":\"CANCEL\",\"tradingAccount\":\"IGNORED\",\"securityId\":\"IGNORED\",\"quantity\":999}"))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/trade-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\":22,\"type\":\"BUY\",\"tradingAccount\":\"ACC1\",\"securityId\":\"SEC1\",\"quantity\":5}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/v1/positions/ACC1/SEC1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(5));

        mvc.perform(get("/api/v1/positions/ACC1/SEC1/details"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events.length()").value(3))
                .andExpect(jsonPath("$.events[0].eventId").value(21))
                .andExpect(jsonPath("$.events[1].type").value("CANCEL"))
                .andExpect(jsonPath("$.events[2].eventId").value(22));
    }

    @Test
    void unknownPositionIsReturnedAsProblemDetails() throws Exception {
        mvc.perform(get("/api/v1/positions/UNKNOWN/UNKNOWN"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.title").value("Position not found"))
                .andExpect(jsonPath("$.code").value("POSITION_NOT_FOUND"));
    }
}
