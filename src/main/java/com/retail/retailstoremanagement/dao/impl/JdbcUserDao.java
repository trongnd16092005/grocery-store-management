package com.retail.retailstoremanagement.dao.impl;
import com.retail.retailstoremanagement.dao.UserDao;
import com.retail.retailstoremanagement.model.*;
import com.retail.retailstoremanagement.util.DatabaseConnection;
import java.sql.*;
import java.time.OffsetDateTime;

public class JdbcUserDao implements UserDao {
    public AppUser findByUsername(String username)throws SQLException{try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement("SELECT * FROM app_users WHERE LOWER(username)=LOWER(?)")){s.setString(1,username);try(ResultSet r=s.executeQuery()){return r.next()?map(r):null;}}}
    public int count()throws SQLException{try(Connection c=DatabaseConnection.getConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT COUNT(*) FROM app_users")){r.next();return r.getInt(1);}}
    public AppUser createAdmin(String username,String hash,String fullName)throws SQLException{String sql="INSERT INTO app_users(username,password_hash,full_name,role,active) SELECT ?,?,?,'ADMIN',TRUE WHERE NOT EXISTS(SELECT 1 FROM app_users) RETURNING *";try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setString(1,username);s.setString(2,hash);s.setString(3,fullName);try(ResultSet r=s.executeQuery()){if(!r.next())throw new SQLException("Hệ thống đã có tài khoản.");return map(r);}}}
    public AppUser create(String username,String hash,String fullName,UserRole role)throws SQLException{try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement("INSERT INTO app_users(username,password_hash,full_name,role) VALUES (?,?,?,?) RETURNING *")){s.setString(1,username);s.setString(2,hash);s.setString(3,fullName);s.setString(4,role.name());try(ResultSet r=s.executeQuery()){r.next();return map(r);}}}
    public java.util.List<AppUser> findAll()throws SQLException{java.util.List<AppUser> users=new java.util.ArrayList<>();try(Connection c=DatabaseConnection.getConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT * FROM app_users ORDER BY created_at")){while(r.next())users.add(map(r));}return users;}
    private AppUser map(ResultSet r)throws SQLException{AppUser u=new AppUser();u.setId(r.getLong("id"));u.setUsername(r.getString("username"));u.setPasswordHash(r.getString("password_hash"));u.setFullName(r.getString("full_name"));u.setRole(UserRole.valueOf(r.getString("role")));u.setActive(r.getBoolean("active"));u.setCreatedAt(r.getObject("created_at",OffsetDateTime.class));u.setUpdatedAt(r.getObject("updated_at",OffsetDateTime.class));return u;}
}
