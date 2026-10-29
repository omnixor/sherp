package dao;

import java.sql.SQLException;
import java.util.List;
import model.Student;

public interface StudentDAO {
    void addStudent(Student student) throws SQLException;
    void addStudent(Student student, String password) throws SQLException;
    Student authenticate(int studentId, String password) throws SQLException;
    List<Student> getStudents() throws SQLException;
    Student searchStudent(int studentId) throws SQLException;
    void updateStudent(Student student) throws SQLException;
    void updateStudent(Student student, String password) throws SQLException;
    void deleteStudent(int studentId) throws SQLException;
}
