package com.plass.event;

import com.jayway.jsonpath.JsonPath;
import com.plass.support.IntegrationTest;
import com.plass.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Public catalogue + the booking API, called over HTTP exactly like the frontend does. */
class EventApiTest extends IntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void searchFindsUpcomingEventsWithTheirAvailability() throws Exception {
        Event event = data.upcomingEvent();

        mvc.perform(get("/api/events").param("q", event.getTitle()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(event.getId().intValue()))
                .andExpect(jsonPath("$.content[0].city").value("Testville"))
                .andExpect(jsonPath("$.content[0].totalSeats").value(12))
                .andExpect(jsonPath("$.content[0].availableSeats").value(12));
    }

    @Test
    void eventDetailsAndSeatMapArePublic() throws Exception {
        Event event = data.upcomingEvent();

        mvc.perform(get("/api/events/{id}", event.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookable").value(true))
                .andExpect(jsonPath("$.prices.length()").value(3));

        mvc.perform(get("/api/events/{id}/seats", event.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(12))
                .andExpect(jsonPath("$[0].row").value("A"))
                .andExpect(jsonPath("$[0].tier").value("VIP"))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));
    }

    @Test
    void unknownEventsReturnAProblemDetail() throws Exception {
        mvc.perform(get("/api/events/{id}", 987654321))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void checkoutOverHttp() throws Exception {
        Event event = data.upcomingEvent();
        Long seatId = data.seatIds(event, "A1").getFirst();
        User buyer = data.user();

        String held = mvc.perform(post("/api/bookings")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerToken(buyer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"eventId": %d, "seatIds": [%d]}
                                """.formatted(event.getId(), seatId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.reference").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        Integer bookingId = JsonPath.read(held, "$.id");

        mvc.perform(post("/api/bookings/{id}/pay", bookingId).header(HttpHeaders.AUTHORIZATION, data.bearerToken(buyer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // Someone else trying to buy the same seat gets a clear 409 that names the seat.
        mvc.perform(post("/api/bookings")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerToken(data.user()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"eventId": %d, "seatIds": [%d]}
                                """.formatted(event.getId(), seatId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SEATS_UNAVAILABLE"))
                .andExpect(jsonPath("$.seats[0]").value("A1"));

        mvc.perform(get("/api/events/{id}/seats", event.getId()))
                .andExpect(jsonPath("$[0].status").value("BOOKED"));
    }
}
