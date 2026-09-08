package dao;

import database.DummyDatabase;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.Student;

public class DummyStudentDAO implements StudentDAO {
    @Override
    public void addStudent(Student student) throws SQLException {
        if (searchStudent(student.getUserId()) != null) {
            throw new SQLException("Student ID already exists.");
        }
        DummyDatabase.students.add(student);
    }

    @Override
    public List<Student> getStudents() {
        return new ArrayList<>(DummyDatabase.students);
    }

    @Override
    public Student searchStudent(int studentId) {
        for (Student student : DummyDatabase.students) {
            if (student.getUserId() == studentId) {
                return student;
            }
        }
        return null;
    }

    @Override
    public void updateStudent(Student student) throws SQLException {
        Student existingStudent = searchStudent(student.getUserId());
        if (existingStudent == null) {
            throw new SQLException("Student was not found.");
        }
        existingStudent.setName(student.getName());
        existingStudent.setCourse(student.getCourse());
        existingStudent.setPhone(student.getPhone());
        existingStudent.setRoomNumber(student.getRoomNumber());
    }

    @Override
    public void deleteStudent(int studentId) throws SQLException {
        Student student = searchStudent(studentId);
        if (student == null) {
            throw new SQLException("Student was not found.");
        }
        DummyDatabase.students.remove(student);
    }
}
