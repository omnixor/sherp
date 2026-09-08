package dao;

import database.DummyDatabase;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.Room;

public class DummyRoomDAO implements RoomDAO {
    @Override
    public void addRoom(Room room) throws SQLException {
        if (findRoom(room.getRoomNumber()) != null) {
            throw new SQLException("Room number already exists.");
        }
        DummyDatabase.rooms.add(room);
    }

    @Override
    public List<Room> getRooms() {
        return new ArrayList<>(DummyDatabase.rooms);
    }

    @Override
    public void allocateRoom(int roomNumber) throws SQLException {
        Room room = findRoom(roomNumber);
        if (room == null || !room.isAvailable()) {
            throw new SQLException("Room does not exist or is already full.");
        }
        room.setOccupied(room.getOccupied() + 1);
    }

    @Override
    public void vacateRoom(int roomNumber) throws SQLException {
        Room room = findRoom(roomNumber);
        if (room == null || room.getOccupied() == 0) {
            throw new SQLException("Room does not exist or is already empty.");
        }
        room.setOccupied(room.getOccupied() - 1);
    }

    private Room findRoom(int roomNumber) {
        for (Room room : DummyDatabase.rooms) {
            if (room.getRoomNumber() == roomNumber) {
                return room;
            }
        }
        return null;
    }
}
