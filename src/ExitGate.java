public class ExitGate {
    ParkingLot parkingLot;

    public ExitGate() {
        this.parkingLot = ParkingLot.getInstance();
    }

    public void processExit(Ticket ticket){
        parkingLot.deallocateSlot(ticket);
    }
}
