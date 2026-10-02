package app.kerem.delivery_slots;

import java.time.Instant;

public record SlotResponse(Long id, Instant startTime, Instant endTime, int capacity, int free) {

    static SlotResponse from(Slot slot) {
        return new SlotResponse(slot.getId(), slot.getStartTime(), slot.getEndTime(),
                slot.getCapacity(), slot.getFree());
    }
}
