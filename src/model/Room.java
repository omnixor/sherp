package model;

public class Room {
    private int roomNumber;
    private int capacity;
    private int occupied;

    public Room(int roomNumber, int capacity, int occupied) {
        this.roomNumber = roomNumber;
        this.capacity = capacity;
        this.occupied = occupied;
    }

    public int getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(int roomNumber) {
        this.roomNumber = roomNumber;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getOccupied() {
        return occupied;
    }

    public void setOccupied(int occupied) {
        this.occupied = occupied;
    }

    public boolean isAvailable() {
        return occupied < capacity;
    }
}
