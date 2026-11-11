package com.plass.venue;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateVenueRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 60) String city,
        @NotBlank @Size(max = 255) String address,
        @NotNull @Min(1) @Max(26) Integer rows,
        @NotNull @Min(1) @Max(40) Integer seatsPerRow) {
}
