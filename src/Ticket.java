import models.Vehicle;

import java.time.LocalDateTime;

public class Ticket {
    private final String ticketId;
    private final Vehicle vehicle;
    private final Slot slot;
    private final LocalDateTime entryTime;

    public Ticket(String id, Vehicle vehicle, Slot slot) {
        this.ticketId = id;
        this.vehicle = vehicle;
        this.slot = slot;
        this.entryTime = LocalDateTime.now();
    }

    public String getTicketId() {
        return ticketId;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public Slot getSlot() {
        return slot;
    }

    public LocalDateTime getEntryTime() {
        return entryTime;
    }
}
