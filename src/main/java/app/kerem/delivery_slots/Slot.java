package app.kerem.delivery_slots;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
public class Slot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Instant startTime;
    private Instant endTime;
    private int capacity;
    private int booked;

    // Hibernate bumps this on every update; two concurrent writers on the same row
    // cannot both succeed, which protects the capacity rule under concurrency.
    @Version
    private long version;

    protected Slot() {
    }

    public Slot(Instant startTime, Instant endTime, int capacity) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.capacity = capacity;
    }

    public Long getId() {
        return id;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getBooked() {
        return booked;
    }

    public int getFree() {
        return capacity - booked;
    }

    /** The capacity rule: a slot can never hold more bookings than its capacity. */
    public void reserve() {
        if (booked >= capacity) {
            throw new SlotFullException(id);
        }
        booked++;
    }

    public void release() {
        if (booked <= 0) {
            throw new IllegalStateException("Slot " + id + " has no bookings to release");
        }
        booked--;
    }
}
