# 🅿️ Parking Lot — LLD Interview Revision Guide

---

## 📋 INTERVIEW APPROACH SEQUENCE

Follow this sequence strictly in an actual interview. Do NOT jump to design directly.

```
1. REQUIREMENT GATHERING       (~5 min)
2. SCOPE CONFIRMATION          (~2 min)
3. CORE ENTITIES & RELATIONS   (~5 min)
4. CLASS DESIGN (LLD)          (~15 min)
5. ENTRY / EXIT FLOW           (~5 min)
6. DESIGN PATTERNS             (~5 min)
7. CONCURRENCY                 (~5 min)
8. EDGE CASES                  (~5 min)
```

---

## STEP 1 — REQUIREMENT GATHERING

**Never skip this. Always ask before designing.**

### Questions to ask the interviewer:

| Question | Why It Matters |
|---|---|
| Multiple vehicle types? (Bike, Car, Truck) | Drives slot typing and class hierarchy |
| Single floor or multi-floor? | Drives Floor abstraction |
| Are slots typed? Can a Bike park in a Car slot? | Drives slot allocation logic |
| Single entry/exit or multiple gates? | Drives Gate class design |
| Do we need payments / billing? | Scopes the problem — usually say no |
| Any display board requirements? | Opens Observer pattern discussion |

### Scope confirmation (say this out loud):
> "So to summarize — multi-floor parking lot, typed slots, typed vehicles,
> slot allocation on entry, slot release on exit, no payments.
> I'll start designing around these constraints."

---

## STEP 2 — CORE ENTITIES & RELATIONSHIPS

### Entities:
- `ParkingLot` — top-level, manages floors
- `Floor` — manages slots
- `Slot` — holds a vehicle, has a type and status
- `Vehicle` — abstract base, subclasses: Car, Bike, Truck
- `Ticket` — issued on entry, used on exit
- `EntryGate` / `ExitGate` — handle vehicle flow

### Relationships:
```
ParkingLot  ──has-many──►  Floor       (Composition)
Floor       ──has-many──►  Slot        (Composition)
Slot        ──holds──►     Vehicle     (Association)
ParkingLot  ──manages──►   Ticket map
EntryGate   ──uses──►      ParkingLot
ExitGate    ──uses──►      ParkingLot
```

**Composition** = child cannot exist without parent (Floor without ParkingLot makes no sense)
**Association** = Slot holds a Vehicle reference, but Vehicle exists independently

---

## STEP 3 — CLASS DESIGN

### VehicleType Enum
```java
public enum VehicleType { BIKE, CAR, TRUCK }
```

### Vehicle (Abstract)
```java
public abstract class Vehicle {
    private final String numberPlate;
    private final VehicleType type;
    // constructor, getters, toString
}
// Subclasses: Car, Bike, Truck — each calls super with fixed VehicleType
```
**Why abstract?** You never instantiate a raw Vehicle. Also, subclasses can hold type-specific behavior later (e.g., Truck needs 2 slots).

### SlotStatus Enum
```java
public enum SlotStatus { FREE, OCCUPIED }
```

### Slot
```java
public class Slot {
    private final int slotId;
    private final int floorNumber;   // IMPORTANT — carry floor context
    private final VehicleType slotType;
    private SlotStatus status = SlotStatus.FREE;  // always initialize!
    private Vehicle parkedVehicle;

    public boolean isAvailable() { return status == SlotStatus.FREE; }

    public void parkVehicle(Vehicle v) {
        if (!isAvailable()) throw new IllegalStateException("Already occupied");
        this.parkedVehicle = v;
        this.status = SlotStatus.OCCUPIED;
    }

    public void removeVehicle() {
        if (isAvailable()) throw new IllegalStateException("Already free");
        this.parkedVehicle = null;
        this.status = SlotStatus.FREE;
    }
}
```

**Key points:**
- `status` must be initialized to `FREE` — leaving it uninitialized defaults to `null` and breaks `isAvailable()`
- `parkVehicle` and `removeVehicle` must guard against invalid state — never silent fail
- `floorNumber` stored inside Slot so allocation result carries floor context

### Floor
```java
public class Floor {
    private final int floorNumber;
    private final List<Slot> slots;

    public Optional<Slot> findAvailableSlot(VehicleType type) {
        return slots.stream()
                .filter(s -> s.isAvailable() && s.isOfType(type))
                .findFirst();
    }
}
```

**Why Optional?** Forces the caller to handle the empty case explicitly. Never return null.

### ParkingLot (Singleton)
```java
public class ParkingLot {
    private final List<Floor> floors;

    private ParkingLot() { /* init floors */ }

    // Bill Pugh Singleton — lazy, thread-safe, no synchronized needed
    private static class Helper {
        private static final ParkingLot INSTANCE = new ParkingLot();
    }
    public static ParkingLot getInstance() { return Helper.INSTANCE; }

    public List<Floor> getFloors() {
        return Collections.unmodifiableList(floors); // never expose mutable internals
    }
}
```

### Ticket
```java
public class Ticket {
    private final String ticketId;      // UUID
    private final Vehicle vehicle;
    private final Slot slot;            // carries floor + slot info
    private final LocalDateTime entryTime;
}
```

**Why Ticket?** Records entry time (for billing later), slot assigned, and vehicle. Without it, state is lost on system crash.

### EntryGate / ExitGate
```java
public class EntryGate {
    private final ParkingLot parkingLot;  // private final — always

    public Ticket processEntry(Vehicle vehicle) throws ParkingLotFullException {
        Slot slot = parkingLot.allocateSlot(vehicle);
        return new Ticket(UUID.randomUUID().toString(), vehicle, slot);
    }
}

public class ExitGate {
    private final ParkingLot parkingLot;

    public void processExit(Ticket ticket) {
        parkingLot.deallocateSlot(ticket);
    }
}
```

---

## STEP 4 — SLOT ALLOCATION LOGIC

```java
public Slot allocateSlot(Vehicle vehicle) throws ParkingLotFullException {
    for (Floor floor : floors) {
        Optional<Slot> slot = floor.findAvailableSlot(vehicle.getType());
        if (slot.isPresent()) {
            slot.get().parkVehicle(vehicle);
            return slot.get();
        }
    }
    throw new ParkingLotFullException("No slot available for: " + vehicle.getType());
}
```

**Strategy:** First available, lowest floor first.
**On failure:** Throw custom exception — never return null.

### Deallocation
```java
public void deallocateSlot(Ticket ticket) {
    Floor floor = floors.stream()
            .filter(f -> f.getFloorNumber() == ticket.getSlot().getFloorNumber())
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Floor not found"));
    Slot slot = floor.getSlots().stream()
            .filter(s -> s.getSlotId() == ticket.getSlot().getSlotId())
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Slot not found"));
    slot.removeVehicle();
}
```

**Always use stream filter** to find floor by floorNumber — never `floors.get(floorNumber)` which assumes index == floorNumber.

---

## STEP 5 — DESIGN PATTERNS USED

### 1. Singleton — ParkingLot
One physical lot = one instance. Use Bill Pugh pattern.
```java
private static class Helper {
    private static final ParkingLot INSTANCE = new ParkingLot();
}
```

### 2. Strategy — Slot Allocation (extensibility)
If interviewer asks about different allocation strategies:
```java
public interface SlotAllocationStrategy {
    Optional<Slot> allocate(List<Floor> floors, VehicleType type);
}
public class FirstAvailableStrategy implements SlotAllocationStrategy { ... }
public class NearestToExitStrategy implements SlotAllocationStrategy { ... }
```
Inject into ParkingLot constructor. Switch strategies without changing ParkingLot.

### 3. Observer — Display Board (bonus)
If asked about available slot display:
```java
public interface SlotObserver {
    void onSlotStatusChanged(Slot slot);
}
public class DisplayBoard implements SlotObserver {
    public void onSlotStatusChanged(Slot slot) {
        // update count display
    }
}
```
Slot notifies observers on `parkVehicle` / `removeVehicle`.

---

## STEP 6 — CONCURRENCY

### The Race Condition
```
Thread 1 → findAvailableSlot() → finds Slot 3 as FREE
                                      ← context switch →
Thread 2 → findAvailableSlot() → finds Slot 3 as FREE   ← same slot!
Thread 2 → parkVehicle() → marks OCCUPIED
                                      ← context switch →
Thread 1 → parkVehicle() → OCCUPIED → 💥 IllegalStateException
```

**Root cause:** find + mark are two separate operations but must be atomic. This is called a **check-then-act race condition**.

### The Fix
```java
public synchronized Slot allocateSlot(Vehicle vehicle) throws ParkingLotFullException { ... }
public synchronized void deallocateSlot(Ticket ticket) { ... }
public synchronized int getAvailableSlots() { ... }  // reads also need the lock!
```

**Why synchronize reads too?**
A thread reading slot counts mid-allocation gets stale/inconsistent data. Any method reading shared mutable state needs the same lock as methods writing it.

### Why not lock at Slot level only?
Slot-level lock only protects the write. The read (`findAvailableSlot`) already happened outside the lock — two threads can independently read the same slot as FREE before either writes. The lock must wrap both read and write together.

### Trade-off
`synchronized` on instance method = lock on `this` = global lot lock.
Thread on Floor 0 blocks Thread on Floor 2 even though they'd never conflict.

**Next level:** Per-floor locking — threads on different floors don't block each other. Mention this as an optimization if interviewer pushes for throughput.

---

## STEP 7 — EDGE CASES

| Edge Case | Handling |
|---|---|
| Lot is full | `ParkingLotFullException` thrown, entry denied |
| Invalid ticket on exit | `orElseThrow` in deallocateSlot |
| Same vehicle enters twice | Add `Map<numberPlate, Ticket>` registry, check on entry |
| Double-park on same slot | `parkVehicle` throws `IllegalStateException` |
| Double-free on same slot | `removeVehicle` throws `IllegalStateException` |
| Floor with no matching slots | `findAvailableSlot` returns `Optional.empty()`, handled upstream |
| Concurrent exit on same ticket | `deallocateSlot` synchronized, `removeVehicle` guards against free slot |
| Invalid floor number | `orElseThrow` with `IllegalArgumentException` |

---

## COMMON FOLLOW-UP QUESTIONS

**Q: Why abstract Vehicle instead of just using VehicleType enum?**
> Subclasses give you an extension point. Car needing 2 slots, Truck having weight — these behaviors go in the subclass cleanly. With just an enum, you'd need if-else chains scattered across the codebase.

**Q: Why Singleton for ParkingLot?**
> One physical lot = one source of truth. Multiple instances would manage separate state, leading to double-allocation of the same slot.

**Q: Why store floorNumber inside Slot?**
> Without it, `allocateSlot` returns a Slot with no floor context. Ticket can't tell the driver where their car is parked. Floor context must travel with the Slot through the entire call chain.

**Q: Why not return null from findAvailableSlot?**
> Null forces every caller to do null checks. `Optional` makes the empty case explicit and compiler-enforced. Null is a silent failure waiting to happen.

**Q: Why synchronize reads like getAvailableSlots?**
> A read happening mid-write sees partial state — a slot being marked occupied while the count is being summed. Reads on shared mutable state need the same lock as writes.

**Q: How would you add Electric Vehicles with charging slots?**
> Add `ELECTRIC_VEHICLE` to VehicleType, create `ElectricVehicle` subclass, add `CHARGING_SLOT` type. Replace direct equality in `isOfType()` with a compatibility matrix method. No other class needs to change.

**Q: How would you support different allocation strategies?**
> Extract into Strategy pattern — `SlotAllocationStrategy` interface with implementations like `FirstAvailableStrategy`, `NearestToExitStrategy`. Inject into ParkingLot constructor. Open/Closed principle — add strategies without modifying ParkingLot.

**Q: What about system crash between allocation and ticket creation?**
> In-memory design can't recover from this. In a real system: write ticket to durable storage *before* confirming slot as occupied. On recovery, reconcile ticket store with slot state.

---

## COMMON MISTAKES TO AVOID

| Mistake | Correct Approach |
|---|---|
| Jumping to design without requirement gathering | Always ask 4-5 scoping questions first |
| Returning null from find methods | Return `Optional` |
| Leaving `SlotStatus` uninitialized | Always initialize to `SlotStatus.FREE` |
| Silent fail in parkVehicle/removeVehicle | Throw `IllegalStateException` |
| `floors.get(floorNumber)` for lookup | Stream filter by `getFloorNumber()` |
| Exposing mutable internal list via getter | Return `Collections.unmodifiableList()` |
| Only synchronizing writes, not reads | All access to shared mutable state needs the lock |
| Package-private fields in Gate classes | Always `private final` |
| Unused `throws` in method signature | Only declare exceptions you actually throw |

---

## QUICK REFERENCE — CLASS SUMMARY

```
ParkingLot          Singleton (Bill Pugh), manages List<Floor>
Floor               Has floorNumber + List<Slot>, finds available slot
Slot                Has slotId, floorNumber, slotType, status, parkedVehicle
Vehicle             Abstract — Car, Bike, Truck extend it
Ticket              ticketId, vehicle, slot, entryTime
EntryGate           processEntry(Vehicle) → Ticket
ExitGate            processExit(Ticket)
VehicleType         Enum: BIKE, CAR, TRUCK
SlotStatus          Enum: FREE, OCCUPIED
ParkingLotFullException  Checked exception on no available slot
```

---

*Prepared from mock interview session — covers design, OOP, patterns, concurrency, and edge cases.*
