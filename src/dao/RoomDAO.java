package dao;

import java.sql.SQLException;
import java.util.List;
import model.Room;
import model.RoomAllocation;

public interface RoomDAO {
    void addRoom(Room room) throws SQLException;
    List<Room> getRooms() throws SQLException;
    void updateRoom(Room room) throws SQLException;
    void deleteRoom(int roomNumber) throws SQLException;
    void allocateRoom(int studentId, int roomNumber) throws SQLException;
    void vacateRoom(int studentId) throws SQLException;
    List<RoomAllocation> getAllocationHistory(Integer studentId) throws SQLException;
}
