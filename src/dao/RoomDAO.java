package dao;

import java.sql.SQLException;
import java.util.List;
import model.Room;

public interface RoomDAO {
    void addRoom(Room room) throws SQLException;
    List<Room> getRooms() throws SQLException;
    void allocateRoom(int roomNumber) throws SQLException;
    void vacateRoom(int roomNumber) throws SQLException;
}
