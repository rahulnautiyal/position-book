package com.example.positionbook.api.controller;

import com.example.positionbook.api.response.ApiProblemResponse;
import com.example.positionbook.api.response.PositionDetailsResponse;
import com.example.positionbook.api.response.PositionResponse;
import com.example.positionbook.api.request.TradeEventRequest;
import com.example.positionbook.domain.TradeEvent;
import com.example.positionbook.domain.TradeEventType;
import com.example.positionbook.service.PositionBookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1")
@Validated
@Tag(name = "Position Book", description = "Trade event ingestion and real-time position queries")
public class PositionBookController {
    private final PositionBookService service;

    public PositionBookController(PositionBookService service) {
        this.service = service;
    }

    @Operation(
            summary = "Process a trade event",
            description = "Processes a BUY, SELL or CANCEL event. CANCEL identifies the original event by eventId; its account, security and quantity are ignored.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Event processed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or unknown cancellation", content = @Content(schema = @Schema(implementation = ApiProblemResponse.class))),
            @ApiResponse(responseCode = "409", description = "Duplicate trade ID or repeated cancellation", content = @Content(schema = @Schema(implementation = ApiProblemResponse.class)))
    })
    @PostMapping("/trade-events")
    public ResponseEntity<Void> process(@Valid @RequestBody TradeEventRequest request) {
        TradeEvent event = new TradeEvent(
                request.eventId(),
                request.type(),
                normalize(request.tradingAccount()),
                normalize(request.securityId()),
                request.type() == TradeEventType.CANCEL
                        ? 0L
                        : request.quantity() == null ? 0L : request.quantity());
        service.process(event);
        return ResponseEntity.status(201).build();
    }

    @Operation(summary = "Get a current position", description = "Returns the aggregated quantity for a trading account and security.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Position found"),
            @ApiResponse(responseCode = "400", description = "Invalid path parameter", content = @Content(schema = @Schema(implementation = ApiProblemResponse.class))),
            @ApiResponse(responseCode = "404", description = "Position does not exist", content = @Content(schema = @Schema(implementation = ApiProblemResponse.class)))
    })
    @GetMapping("/positions/{tradingAccount}/{securityId}")
    public PositionResponse getPosition(
            @Parameter(description = "Trading account identifier", required = true, example = "ACC1")
            @PathVariable @NotBlank @Size(max = 100) String tradingAccount,
            @Parameter(description = "Security identifier", required = true, example = "SEC1")
            @PathVariable @NotBlank @Size(max = 100) String securityId) {
        return PositionResponse.from(service.getPosition(tradingAccount.trim(), securityId.trim()));
    }

    @Operation(summary = "Get position and event drilldown", description = "Returns the current position and all events contributing to that account/security position in processing order.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Position and drilldown found"),
            @ApiResponse(responseCode = "400", description = "Invalid path parameter", content = @Content(schema = @Schema(implementation = ApiProblemResponse.class))),
            @ApiResponse(responseCode = "404", description = "Position does not exist", content = @Content(schema = @Schema(implementation = ApiProblemResponse.class)))
    })
    @GetMapping("/positions/{tradingAccount}/{securityId}/details")
    public PositionDetailsResponse getDetails(
            @PathVariable @NotBlank @Size(max = 100) String tradingAccount,
            @PathVariable @NotBlank @Size(max = 100) String securityId) {
        var result = service.getPositionDetails(tradingAccount.trim(), securityId.trim());
        return PositionDetailsResponse.of(result.position(), result.events());
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
