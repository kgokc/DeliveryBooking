package app.kerem.delivery_slots;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

    private final SlotRepository slots;
    private final BookingRepository bookings;

    public BookingService(SlotRepository slots, BookingRepository bookings) {
        this.slots = slots;
        this.bookings = bookings;
    }

    @Transactional
    public Booking book(Long slotId) {
        Slot slot = slots.findById(slotId)
                .orElseThrow(() -> new NotFoundException("Slot " + slotId + " not found"));
        slot.reserve();
        return bookings.save(new Booking(slot));
    }

    @Transactional
    public Booking cancel(Long bookingId) {
        Booking booking = bookings.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking " + bookingId + " not found"));
        // Cancelling twice must not free a place twice.
        if (booking.getStatus() == BookingStatus.ACTIVE) {
            booking.getSlot().release();
            booking.cancel();
        }
        return booking;
    }
}
