package dao;

import database.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import model.Room;
import model.RoomAllocation;

public class RoomDAOImpl implements RoomDAO {
    @Override
    public void addRoom(Room room) throws SQLException {
        validateRoom(room);
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "INSERT INTO rooms (room_number, capacity, occupied) VALUES (?, ?, 0)")) {
            statement.setInt(1, room.getRoomNumber());
            statement.setInt(2, room.getCapacity());
            statement.executeUpdate();
        }
    }

    @Override
    public void updateRoom(Room room) throws SQLException {
        validateRoom(room);
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                lockRoom(connection, room.getRoomNumber());
                int occupied = countStudentsInRoom(connection, room.getRoomNumber());
                if (room.getCapacity() < occupied) {
                    throw new SQLException("Capacity cannot be less than current occupancy.");
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE rooms SET capacity = ?, occupied = ? WHERE room_number = ?")) {
                    statement.setInt(1, room.getCapacity());
                    statement.setInt(2, occupied);
                    statement.setInt(3, room.getRoomNumber());
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Room was not found.");
                    }
                }
                connection.commit();
            } catch (SQLException exception) {
                rollback(connection, exception);
                throw exception;
            }
        }
    }

    @Override
    public void deleteRoom(int roomNumber) throws SQLException {
        if (roomNumber <= 0) {
            throw new SQLException("Room number must be positive.");
        }
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                lockRoom(connection, roomNumber);
                if (countStudentsInRoom(connection, roomNumber) > 0) {
                    throw new SQLException("Vacate all students before deleting this room.");
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT COUNT(*) FROM room_allocations WHERE room_number = ?")) {
                    statement.setInt(1, roomNumber);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        resultSet.next();
                        if (resultSet.getInt(1) > 0) {
                            throw new SQLException("Room allocation history exists; deletion is not allowed.");
                        }
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM rooms WHERE room_number = ?")) {
                    statement.setInt(1, roomNumber);
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Room was not found.");
                    }
                }
                connection.commit();
            } catch (SQLException exception) {
                rollback(connection, exception);
                throw exception;
            }
        }
    }

    @Override
    public List<Room> getRooms() throws SQLException {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT r.room_number, r.capacity, COUNT(s.student_id) AS occupied " +
            "FROM rooms r LEFT JOIN students s ON s.room_number = r.room_number " +
            "GROUP BY r.room_number, r.capacity ORDER BY r.room_number";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                rooms.add(new Room(resultSet.getInt("room_number"), resultSet.getInt("capacity"),
                    resultSet.getInt("occupied")));
            }
        }
        return rooms;
    }

    @Override
    public void allocateRoom(int studentId, int roomNumber) throws SQLException {
        if (studentId <= 0 || roomNumber <= 0) {
            throw new SQLException("Student ID and room number must be positive.");
        }
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int capacity = lockRoom(connection, roomNumber);
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT room_number FROM students WHERE student_id = ? FOR UPDATE")) {
                    statement.setInt(1, studentId);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                            throw new SQLException("Student was not found.");
                        }
                        if (resultSet.getInt("room_number") != 0) {
                            throw new SQLException("Student already has an active room allocation.");
                        }
                    }
                }
                if (countStudentsInRoom(connection, roomNumber) >= capacity) {
                    throw new SQLException("Room is full.");
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE students SET room_number = ? WHERE student_id = ? AND room_number = 0")) {
                    statement.setInt(1, roomNumber);
                    statement.setInt(2, studentId);
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Student allocation could not be saved.");
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO room_allocations (student_id, room_number) VALUES (?, ?)")) {
                    statement.setInt(1, studentId);
                    statement.setInt(2, roomNumber);
                    statement.executeUpdate();
                }
                updateStoredOccupancy(connection, roomNumber);
                addNotification(connection, studentId, "Room " + roomNumber + " has been allocated to you.");
                connection.commit();
            } catch (SQLException exception) {
                rollback(connection, exception);
                throw exception;
            }
        }
    }

    @Override
    public void vacateRoom(int studentId) throws SQLException {
        if (studentId <= 0) {
            throw new SQLException("Student ID must be positive.");
        }
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int roomNumber;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT room_number FROM students WHERE student_id = ?")) {
                    statement.setInt(1, studentId);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                            throw new SQLException("Student was not found.");
                        }
                        roomNumber = resultSet.getInt("room_number");
                    }
                }
                if (roomNumber == 0) {
                    throw new SQLException("Student does not have an active room allocation.");
                }
                lockRoom(connection, roomNumber);
                int allocationId;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT allocation_id FROM room_allocations WHERE student_id = ? " +
                            "AND room_number = ? AND vacated_at IS NULL FOR UPDATE")) {
                    statement.setInt(1, studentId);
                    statement.setInt(2, roomNumber);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                            throw new SQLException("Active allocation history is missing; no changes were made.");
                        }
                        allocationId = resultSet.getInt("allocation_id");
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT room_number FROM students WHERE student_id = ? FOR UPDATE")) {
                    statement.setInt(1, studentId);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next() || resultSet.getInt("room_number") != roomNumber) {
                            throw new SQLException("Student allocation changed; refresh and try again.");
                        }
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE students SET room_number = 0 WHERE student_id = ? AND room_number = ?")) {
                    statement.setInt(1, studentId);
                    statement.setInt(2, roomNumber);
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Room allocation could not be vacated.");
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE room_allocations SET vacated_at = CURRENT_TIMESTAMP " +
                            "WHERE allocation_id = ? AND vacated_at IS NULL")) {
                    statement.setInt(1, allocationId);
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Allocation history could not be closed.");
                    }
                }
                updateStoredOccupancy(connection, roomNumber);
                addNotification(connection, studentId, "Your allocation in room " + roomNumber + " was vacated.");
                connection.commit();
            } catch (SQLException exception) {
                rollback(connection, exception);
                throw exception;
            }
        }
    }

    @Override
    public List<RoomAllocation> getAllocationHistory(Integer studentId) throws SQLException {
        String sql = "SELECT a.allocation_id, a.student_id, s.name, a.room_number, " +
            "a.allocated_at, a.vacated_at FROM room_allocations a " +
            "JOIN students s ON s.student_id = a.student_id " +
            (studentId == null ? "" : "WHERE a.student_id = ? ") +
            "ORDER BY a.allocated_at DESC";
        List<RoomAllocation> history = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (studentId != null) {
                statement.setInt(1, studentId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Timestamp vacatedAt = resultSet.getTimestamp("vacated_at");
                    history.add(new RoomAllocation(resultSet.getInt("allocation_id"),
                        resultSet.getInt("student_id"), resultSet.getString("name"),
                        resultSet.getInt("room_number"),
                        resultSet.getTimestamp("allocated_at").toLocalDateTime(),
                        vacatedAt == null ? null : vacatedAt.toLocalDateTime()));
                }
            }
        }
        return history;
    }

    private int lockRoom(Connection connection, int roomNumber) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT capacity FROM rooms WHERE room_number = ? FOR UPDATE")) {
            statement.setInt(1, roomNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("Room was not found.");
                }
                return resultSet.getInt("capacity");
            }
        }
    }

    private int countStudentsInRoom(Connection connection, int roomNumber) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM students WHERE room_number = ?")) {
            statement.setInt(1, roomNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private void updateStoredOccupancy(Connection connection, int roomNumber) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE rooms SET occupied = ? WHERE room_number = ?")) {
            statement.setInt(1, countStudentsInRoom(connection, roomNumber));
            statement.setInt(2, roomNumber);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Room occupancy could not be updated.");
            }
        }
    }

    private void addNotification(Connection connection, int studentId, String message) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO notifications (student_id, message) VALUES (?, ?)")) {
            statement.setInt(1, studentId);
            statement.setString(2, message);
            statement.executeUpdate();
        }
    }

    private void validateRoom(Room room) throws SQLException {
        if (room == null || room.getRoomNumber() <= 0 || room.getCapacity() <= 0) {
            throw new SQLException("Room number and capacity must be positive.");
        }
    }

    private void rollback(Connection connection, SQLException cause) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            cause.addSuppressed(rollbackException);
        }
    }
}
