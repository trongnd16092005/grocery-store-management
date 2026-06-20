package com.retail.retailstoremanagement.dao;
import com.retail.retailstoremanagement.model.AppUser;
import com.retail.retailstoremanagement.model.UserRole;
import java.sql.SQLException;
import java.util.List;
public interface UserDao {
    AppUser findByUsername(String username) throws SQLException;
    int count() throws SQLException;
    AppUser createAdmin(String username,String passwordHash,String fullName) throws SQLException;
    AppUser create(String username,String passwordHash,String fullName,UserRole role) throws SQLException;
    List<AppUser> findAll() throws SQLException;
}
