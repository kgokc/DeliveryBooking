package app.kerem.delivery_slots;

import java.time.Instant;

public record BookingResponse(Long id, Long slotId, BookingStatus status, Instant createdAt) {

    static BookingResponse from(Booking booking) {
        return new BookingResponse(booking.getId(), booking.getSlot().getId(),
                booking.getStatus(), booking.getCreatedAt());
    }
}
