package dao;

import database.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.Room;

public class RoomDAOImpl implements RoomDAO {
    @Override
    public void addRoom(Room room) throws SQLException {
        String sql = "INSERT INTO rooms (room_number, capacity, occupied) VALUES (?, ?, ?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, room.getRoomNumber());
            statement.setInt(2, room.getCapacity());
            statement.setInt(3, room.getOccupied());
            statement.executeUpdate();
        }
    }

    @Override
    public List<Room> getRooms() throws SQLException {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT room_number, capacity, occupied FROM rooms ORDER BY room_number";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                rooms.add(new Room(
                    resultSet.getInt("room_number"),
                    resultSet.getInt("capacity"),
                    resultSet.getInt("occupied")
                ));
            }
        }
        return rooms;
    }

    @Override
    public void allocateRoom(int roomNumber) throws SQLException {
        String sql = "UPDATE rooms SET occupied = occupied + 1 WHERE room_number = ? AND occupied < capacity";
        updateOccupancy(roomNumber, sql);
    }

    @Override
    public void vacateRoom(int roomNumber) throws SQLException {
        String sql = "UPDATE rooms SET occupied = occupied - 1 WHERE room_number = ? AND occupied > 0";
        updateOccupancy(roomNumber, sql);
    }

    private void updateOccupancy(int roomNumber, String sql) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, roomNumber);
            int changedRows = statement.executeUpdate();
            if (changedRows == 0) {
                throw new SQLException("Room does not exist or the requested operation is not possible.");
            }
        }
    }
}
