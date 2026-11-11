package com.plass.event;

import java.math.BigDecimal;

public record TierPriceResponse(SeatTier tier, BigDecimal price) {
}
