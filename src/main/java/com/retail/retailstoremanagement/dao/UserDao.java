package com.retail.retailstoremanagement.dao;
import com.retail.retailstoremanagement.model.AppUser;
import com.retail.retailstoremanagement.model.UserRole;
import java.sql.SQLException;
import java.util.List;
public interface UserDao {
    AppUser findByUsername(String username) throws SQLException;
    AppUser findById(long id) throws SQLException;
    int count() throws SQLException;
    AppUser createAdmin(String username, String passwordHash, String fullName) throws SQLException;
    AppUser create(String username, String passwordHash, String fullName, UserRole role) throws SQLException;
    List<AppUser> findAll() throws SQLException;
    AppUser update(long id, String fullName, UserRole role) throws SQLException;
    AppUser setActive(long id, boolean active) throws SQLException;
    void updatePassword(long id, String passwordHash) throws SQLException;
}
