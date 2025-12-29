package yand.User;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "firstname", length = 50)
    private String firstname;

    @Column(name = "age")
    private Integer age;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Column(name = "city")
    private String city;

    @Column(name = "mag")
    private String magaz;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "status")
    private String status = "active";

    @Column(name = "role")
    private String role = "client";



    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public int getId() {
        return id;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /*public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }*/

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public Integer getAge() {
        return age;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getMagaz() {
        return magaz;
    }

    public void setMagaz(String magaz) {
        this.magaz = magaz;
    }

    public User(int id, String name, String firstname, Integer age, String city, String magaz) {
        this.id = id;
        this.username = name;
        this.firstname = firstname;
        this.age = age;
        this.city = city;
        this.magaz = magaz;
    }

    public User(String email) {
        this.email = email;
    }

    public User() {
    }

    public enum UserStatus {
        active("active"),
        inactive("inactive"),
        banned("banned");
        //blocked("blocked"),
        //pending("pending");

        private final String value;

        UserStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum UserRole {
    admin("admin"),
    client("client"),
    office("office"),
    courier("courier"),
    collector("collector");

    private final String value;

    UserRole(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
}
