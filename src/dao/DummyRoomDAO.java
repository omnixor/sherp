package dao;

import database.DummyDatabase;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.Room;
import model.Student;
import model.RoomAllocation;
import java.time.LocalDateTime;

public class DummyRoomDAO implements RoomDAO {
    @Override
    public void addRoom(Room room) throws SQLException {
        if (room == null || room.getRoomNumber() <= 0 || room.getCapacity() <= 0) {
            throw new SQLException("Room number and capacity must be positive.");
        }
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
    public void updateRoom(Room updated) throws SQLException {
        Room room = findRoom(updated.getRoomNumber());
        if (room == null) {
            throw new SQLException("Room was not found.");
        }
        if (updated.getCapacity() <= 0 || updated.getCapacity() < room.getOccupied()) {
            throw new SQLException("Capacity cannot be less than current occupancy.");
        }
        room.setCapacity(updated.getCapacity());
    }

    @Override
    public void deleteRoom(int roomNumber) throws SQLException {
        Room room = findRoom(roomNumber);
        if (room == null) {
            throw new SQLException("Room was not found.");
        }
        if (room.getOccupied() > 0 || DummyDatabase.allocations.stream()
                .anyMatch(allocation -> allocation.getRoomNumber() == roomNumber)) {
            throw new SQLException("Rooms with active or historical allocations cannot be deleted.");
        }
        DummyDatabase.rooms.remove(room);
    }

    @Override
    public void allocateRoom(int studentId, int roomNumber) throws SQLException {
        if (studentId <= 0 || roomNumber <= 0) {
            throw new SQLException("Student ID and room number must be positive.");
        }
        Room room = findRoom(roomNumber);
        Student student = findStudent(studentId);
        if (room == null) {
            throw new SQLException("Room was not found.");
        }
        if (student == null) {
            throw new SQLException("Student was not found.");
        }
        if (student.getRoomNumber() != 0) {
            throw new SQLException("Student already has an active room allocation.");
        }
        if (!room.isAvailable()) {
            throw new SQLException("Room is full.");
        }
        room.setOccupied(room.getOccupied() + 1);
        student.setRoomNumber(roomNumber);
        DummyDatabase.allocations.add(new RoomAllocation(DummyDatabase.nextAllocationId(), studentId,
            student.getName(), roomNumber, LocalDateTime.now(), null));
        DummyDatabase.notifications.add(new model.HostelNotification(DummyDatabase.nextNotificationId(),
            studentId, "Room " + roomNumber + " has been allocated to you.", LocalDateTime.now(), false));
    }

    @Override
    public void vacateRoom(int studentId) throws SQLException {
        if (studentId <= 0) {
            throw new SQLException("Student ID must be positive.");
        }
        Student student = findStudent(studentId);
        if (student == null) {
            throw new SQLException("Student was not found.");
        }
        Room room = findRoom(student.getRoomNumber());
        if (room == null) {
            throw new SQLException("Student does not have an active room allocation.");
        }
        room.setOccupied(room.getOccupied() - 1);
        student.setRoomNumber(0);
        for (int index = DummyDatabase.allocations.size() - 1; index >= 0; index--) {
            RoomAllocation allocation = DummyDatabase.allocations.get(index);
            if (allocation.getStudentId() == studentId && allocation.isActive()) {
                DummyDatabase.allocations.set(index, new RoomAllocation(allocation.getAllocationId(),
                    allocation.getStudentId(), allocation.getStudentName(), allocation.getRoomNumber(),
                    allocation.getAllocatedAt(), LocalDateTime.now()));
                break;
            }
        }
        DummyDatabase.notifications.add(new model.HostelNotification(DummyDatabase.nextNotificationId(),
            studentId, "Your allocation in room " + room.getRoomNumber() + " has been vacated.",
            LocalDateTime.now(), false));
    }

    @Override
    public List<RoomAllocation> getAllocationHistory(Integer studentId) {
        List<RoomAllocation> history = new ArrayList<>();
        for (RoomAllocation allocation : DummyDatabase.allocations) {
            if (studentId == null || allocation.getStudentId() == studentId) {
                history.add(allocation);
            }
        }
        return history;
    }

    private Student findStudent(int studentId) {
        for (Student student : DummyDatabase.students) {
            if (student.getUserId() == studentId) {
                return student;
            }
        }
        return null;
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
