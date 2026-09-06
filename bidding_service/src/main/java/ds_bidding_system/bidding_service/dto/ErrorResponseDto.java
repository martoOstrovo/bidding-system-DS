package ds_bidding_system.bidding_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ErrorResponse", description = "Schema to hold error and exception response information")
public class ErrorResponseDto {
    @Schema(description = "API path invoked by the client", example = "uri=/api/get/550e8400-e29b-41d4-a716-446655440000")
    private String apiPath;

    @Schema(description = "HTTP response status name", example = "NOT_FOUND")
    private HttpStatus errorCode;

    @Schema(description = "Error message explaining the failure", example = "Bid listing with id 550e8400-e29b-41d4-a716-446655440000 couldn't be found.")
    private String errorMessage;

    @Schema(description = "Local date and time when the error occurred", example = "2026-09-06T20:00:00")
    private LocalDateTime errorTime;
}
