package com.plass.admin;

import java.time.Duration;
import java.time.Instant;

import com.jayway.jsonpath.JsonPath;
import com.plass.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminApiTest extends IntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void adminCanCreateAVenueAndAnEventAndThenCancelIt() throws Exception {
        String token = data.bearerToken(data.admin());

        String venue = mvc.perform(post("/api/admin/venues")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "New Hall", "city": "Eskişehir", "address": "Odunpazarı", "rows": 5, "seatsPerRow": 10}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.capacity").value(50))
                .andReturn().getResponse().getContentAsString();
        Integer venueId = JsonPath.read(venue, "$.id");

        String event = mvc.perform(post("/api/admin/events")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Opening Night", "description": "First show", "category": "THEATRE",
                                 "venueId": %d, "startsAt": "%s",
                                 "vipPrice": 500, "premiumPrice": 350, "standardPrice": 200}
                                """.formatted(venueId, Instant.now().plus(Duration.ofDays(30)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.totalSeats").value(50))
                .andExpect(jsonPath("$.availableSeats").value(50))
                .andReturn().getResponse().getContentAsString();
        Integer eventId = JsonPath.read(event, "$.id");

        mvc.perform(post("/api/admin/events/{id}/cancel", eventId).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mvc.perform(get("/api/events/{id}", eventId))
                .andExpect(jsonPath("$.bookable").value(false));
    }

    @Test
    void eventsMustBeValid() throws Exception {
        mvc.perform(post("/api/admin/events")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerToken(data.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "", "category": "THEATRE", "venueId": 1, "startsAt": "2001-01-01T10:00:00Z",
                                 "vipPrice": -5, "premiumPrice": 350, "standardPrice": 200}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.description").exists())
                .andExpect(jsonPath("$.errors.startsAt").exists())
                .andExpect(jsonPath("$.errors.vipPrice").exists());
    }

    @Test
    void dashboardCoversTheLastFourteenDays() throws Exception {
        mvc.perform(get("/api/admin/dashboard").header(HttpHeaders.AUTHORIZATION, data.bearerToken(data.admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salesLast14Days.length()").value(14))
                .andExpect(jsonPath("$.totalRevenue").exists());
    }
}
