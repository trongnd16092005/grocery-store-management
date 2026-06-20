package com.retail.retailstoremanagement.service;
import com.retail.retailstoremanagement.dao.UserDao;
import com.retail.retailstoremanagement.model.AppUser;
import org.mindrot.jbcrypt.BCrypt;
import java.sql.SQLException;
import com.retail.retailstoremanagement.model.UserRole;
import java.util.List;

public class AuthService {
    private final UserDao dao; public AuthService(UserDao dao){this.dao=dao;}
    public AppUser login(String username,String password)throws SQLException{if(username==null||password==null)return null;AppUser u=dao.findByUsername(username.trim());return u!=null&&u.isActive()&&BCrypt.checkpw(password,u.getPasswordHash())?u:null;}
    public boolean needsSetup()throws SQLException{return dao.count()==0;}
    public AppUser setupAdmin(String username,String password,String fullName)throws SQLException{
        if(username==null||!username.matches("[A-Za-z0-9._-]{3,50}"))throw new ValidationException("Tên đăng nhập cần 3–50 ký tự.");
        if(password==null||password.length()<8)throw new ValidationException("Mật khẩu cần ít nhất 8 ký tự.");
        if(fullName==null||fullName.trim().length()<2)throw new ValidationException("Vui lòng nhập họ tên.");
        return dao.createAdmin(username.trim(),BCrypt.hashpw(password,BCrypt.gensalt(12)),fullName.trim());
    }
    public AppUser createUser(String username,String password,String fullName,String role)throws SQLException{
        validate(username,password,fullName);UserRole userRole;try{userRole=UserRole.valueOf(role);}catch(Exception e){throw new ValidationException("Vai trò không hợp lệ.");}
        return dao.create(username.trim(),BCrypt.hashpw(password,BCrypt.gensalt(12)),fullName.trim(),userRole);
    }
    public List<AppUser> findAll()throws SQLException{return dao.findAll();}
    private void validate(String username,String password,String fullName){if(username==null||!username.matches("[A-Za-z0-9._-]{3,50}"))throw new ValidationException("Tên đăng nhập cần 3–50 ký tự.");if(password==null||password.length()<8)throw new ValidationException("Mật khẩu cần ít nhất 8 ký tự.");if(fullName==null||fullName.trim().length()<2)throw new ValidationException("Vui lòng nhập họ tên.");}
}
