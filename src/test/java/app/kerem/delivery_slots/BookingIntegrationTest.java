package app.kerem.delivery_slots;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class BookingIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");

    @Autowired
    MockMvc mvc;

    @Autowired
    SlotRepository slots;

    @Autowired
    BookingService service;

    private Slot newSlot(int capacity) {
        Instant start = Instant.now().plusSeconds(86400);
        return slots.save(new Slot(start, start.plusSeconds(3600), capacity));
    }

    @Test
    void secondBookingOfFullSlotReturns409() throws Exception {
        Slot slot = newSlot(1);

        mvc.perform(post("/slots/{id}/bookings", slot.getId())).andExpect(status().isCreated());
        mvc.perform(post("/slots/{id}/bookings", slot.getId())).andExpect(status().isConflict());
    }

    @Test
    void cancellingFreesThePlace() throws Exception {
        Slot slot = newSlot(1);
        Booking booking = service.book(slot.getId());

        mvc.perform(delete("/bookings/{id}", booking.getId())).andExpect(status().isOk());

        mvc.perform(post("/slots/{id}/bookings", slot.getId())).andExpect(status().isCreated());
    }

    @Test
    void concurrentBookingsNeverExceedCapacity() throws Exception {
        int capacity = 3;
        int attempts = 10;
        Slot slot = newSlot(capacity);

        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        List<Future<Boolean>> results = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            Callable<Boolean> attempt = () -> {
                try {
                    service.book(slot.getId());
                    return true;
                } catch (RuntimeException e) {
                    return false; // full, or lost the optimistic-lock race
                }
            };
            results.add(pool.submit(attempt));
        }
        long succeeded = 0;
        for (Future<Boolean> result : results) {
            if (result.get()) {
                succeeded++;
            }
        }
        pool.shutdown();

        assertThat(succeeded).isLessThanOrEqualTo(capacity);
        assertThat(slots.findById(slot.getId()).orElseThrow().getBooked()).isEqualTo((int) succeeded);
    }
}
