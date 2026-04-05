import exceptions.ParkingLotFullException;
import models.Car;

public class Main {
    public static void main(String[] args) throws ParkingLotFullException {
        EntryGate entryGate = new EntryGate();
        ExitGate exitGate = new ExitGate();

        System.out.println(entryGate.processEntry(new Car("MP20E567")));

    }
}
