package app.kerem.delivery_slots;

public class SlotFullException extends RuntimeException {

    public SlotFullException(Long slotId) {
        super("Slot " + slotId + " is full");
    }
}
