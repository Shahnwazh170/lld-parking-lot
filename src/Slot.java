import models.Vehicle;
import models.VehicleType;

public class Slot {
    private final int slotId;
    private final int floorNumber;
    private Vehicle parkedVehicle;
    private SlotStatus status;
    private final VehicleType slotType;

    public Slot(int id, int floorNumber, VehicleType type) {
        this.slotId = id;
        this.slotType = type;
        this.floorNumber = floorNumber;
    }

    public boolean isAvailable() {
        return this.status == SlotStatus.FREE;
    }

    public void parkVehicle(Vehicle vehicle) {
        if (isAvailable()) {
            this.parkedVehicle = vehicle;
            this.status = SlotStatus.OCCUPIED;
        }
    }

    public void removeVehicle() {
        this.parkedVehicle = null;
        this.status = SlotStatus.FREE;
    }

    public Vehicle getParkedVehicle() {
        return parkedVehicle;
    }

    public int getSlotId() {
        return slotId;
    }

    public VehicleType getSlotType() {
        return slotType;
    }

    public boolean isOfType(VehicleType type) {
        return type == this.slotType;
    }

    public int getFloorNumber() {
        return floorNumber;
    }
}
