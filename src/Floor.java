import exceptions.ParkingLotFullException;
import models.Vehicle;
import models.VehicleType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Floor {
    private final int floorNumber;
    private final List<Slot> slots;

    public Floor(int floorNumber) {
        this.floorNumber = floorNumber;
        List<Slot> slots = new ArrayList<>();
        int NUMBER_OF_SLOTS = 10;
        for (int j = 0; j < 3; j++) {
            slots.add(new Slot(j, floorNumber, VehicleType.BIKE));
        }
        for (int j = 3; j < 6; j++) {
            slots.add(new Slot(j, floorNumber, VehicleType.CAR));
        }
        for (int j = 6; j < 9; j++) {
            slots.add(new Slot(j, floorNumber, VehicleType.TRUCK));
        }
        this.slots = slots;
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public List<Slot> getSlots() {
        return slots;
    }

    public Optional<Slot> findAvailableSlot(VehicleType type) throws ParkingLotFullException {
        return slots.stream()
                .filter(slot -> slot.isAvailable() && slot.isOfType(type))
                .findFirst();
    }

    public int countAvailableSlot() {
        List<Slot> availableSlot = slots.stream()
                .filter(Slot::isAvailable).toList();
        return availableSlot.size();
    }

    public int countAvailableSlot(VehicleType type) {
        List<Slot> availableSlot = slots.stream()
                .filter(slot -> slot.isAvailable() && slot.isOfType(type)).toList();
        return availableSlot.size();
    }
}
