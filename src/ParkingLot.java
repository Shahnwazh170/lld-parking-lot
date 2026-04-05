import exceptions.ParkingLotFullException;
import models.Vehicle;
import models.VehicleType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ParkingLot {
    private final List<Floor> floors;

    private ParkingLot() {
        floors = new ArrayList<>();
        int NUMBER_OF_FLOORS = 3;

        for (int i = 0; i < NUMBER_OF_FLOORS; i++) {
            Floor floor = new Floor(i);
            floors.add(floor);
        }
    }

    private static class Helper {
        private static final ParkingLot INSTANCE = new ParkingLot();

    }

    public static ParkingLot getInstance() {
        return Helper.INSTANCE;
    }

    public List<Floor> getFloors() {
        return Collections.unmodifiableList(floors);
    }


    public synchronized Slot allocateSlot(Vehicle vehicle) throws ParkingLotFullException {
        for (Floor floor : floors) {
            Optional<Slot> slot = floor.findAvailableSlot(vehicle.getType());
            if (slot.isPresent()) {
                slot.get().parkVehicle(vehicle);
                return slot.get();
            }
        }
        throw new ParkingLotFullException("No available slot for vehicle: " + vehicle.getType());
    }

    public synchronized void deallocateSlot(Ticket ticket) {
        Floor floor = floors.stream()
                .filter(i -> i.getFloorNumber() == ticket.getSlot().getFloorNumber())
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Invalid ticket: floor not found"));
        Slot slot = floor.getSlots().stream()
                .filter(i -> ticket.getSlot().getSlotId() == i.getSlotId())
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Invalid ticket: slot not found"));
        slot.removeVehicle();
    }

    public synchronized int getAvailableSlots() {
        int count = 0;
        for (Floor floor : floors) {
            count += floor.countAvailableSlot();
        }
        return count;
    }

    public synchronized int getAvailableSlots(VehicleType vehicleType) {
        int count = 0;
        for (Floor floor : floors) {
            count += floor.countAvailableSlot(vehicleType);
        }
        return count;
    }

    public synchronized int getAvailableSlotsForFloor(int floorNumber) {
        Optional<Floor> floor = floors.stream().filter(i -> floorNumber == i.getFloorNumber()).findFirst();
        return floor.map(Floor::countAvailableSlot).orElse(0);
    }

    public synchronized int getAvailableSlotsForFloor(int floorNumber, VehicleType vehicleType) {
        Optional<Floor> floor = floors.stream().filter(i -> floorNumber == i.getFloorNumber()).findFirst();
        return floor.map(i -> i.countAvailableSlot(vehicleType)).orElse(0);
    }
}

/*
 * ParkingLot - manages floors
 * Floor - manages slots
 * Slot - has vehicle
 * Ticket - has car and slot
 * models.Vehicle
 * EntryGate - looks for available slot from floor 1 to N
 * ExitGate
 *
 * */