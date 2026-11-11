package com.plass.event;

import java.util.List;

public record EventFiltersResponse(List<String> cities, List<EventCategory> categories) {
}
