package app.kerem.delivery_slots;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SlotTest {

    private Slot slotWithCapacity(int capacity) {
        Instant start = Instant.parse("2026-01-01T10:00:00Z");
        return new Slot(start, start.plusSeconds(3600), capacity);
    }

    @Test
    void reservesUpToCapacity() {
        Slot slot = slotWithCapacity(2);

        slot.reserve();
        slot.reserve();

        assertThat(slot.getBooked()).isEqualTo(2);
        assertThat(slot.getFree()).isZero();
    }

    @Test
    void rejectsReservationBeyondCapacity() {
        Slot slot = slotWithCapacity(1);
        slot.reserve();

        assertThatThrownBy(slot::reserve).isInstanceOf(SlotFullException.class);
        assertThat(slot.getBooked()).isEqualTo(1);
    }

    @Test
    void releaseFreesAPlace() {
        Slot slot = slotWithCapacity(1);
        slot.reserve();

        slot.release();

        assertThat(slot.getFree()).isEqualTo(1);
    }

    @Test
    void cannotReleaseFromEmptySlot() {
        Slot slot = slotWithCapacity(1);

        assertThatThrownBy(slot::release).isInstanceOf(IllegalStateException.class);
    }
}
