import exceptions.ParkingLotFullException;
import models.Vehicle;

import java.util.UUID;

public class EntryGate {
    ParkingLot parkingLot;

    public EntryGate() {
        this.parkingLot = ParkingLot.getInstance();
    }

    public int getAvailableSlots(){
        return parkingLot.getAvailableSlots();
    }

    public Ticket processEntry(Vehicle vehicle) throws ParkingLotFullException {
        Slot slot = parkingLot.allocateSlot(vehicle);
        System.out.printf("Your %s parked on floor %d and slot %d.", vehicle.getType(),
                slot.getFloorNumber(), slot.getSlotId());
        return new Ticket(UUID.randomUUID().toString(), vehicle, slot);
    }
}
