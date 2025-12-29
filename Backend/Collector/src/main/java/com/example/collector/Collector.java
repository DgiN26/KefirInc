package com.example.collector;


import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "collectors")
public class Collector {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "collector_id", unique = true, nullable = false)
    private String collectorId; // COLLECTOR_1, COLLECTOR_2

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "status")
    private String status; // ACTIVE, INACTIVE, BUSY

    @Column(name = "current_location")
    private String currentLocation;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "last_activity")
    private LocalDateTime lastActivity;

    // Конструкторы
    public Collector() {}

    public Collector(String collectorId, String name, String status) {
        this.collectorId = collectorId;
        this.name = name;
        this.status = status;
        this.createdDate = LocalDateTime.now();
        this.lastActivity = LocalDateTime.now();
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCollectorId() { return collectorId; }
    public void setCollectorId(String collectorId) { this.collectorId = collectorId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStatus() { return status; }
    public void setStatus(String status) {
        this.status = status;
        this.lastActivity = LocalDateTime.now();
    }

    public String getCurrentLocation() { return currentLocation; }
    public void setCurrentLocation(String currentLocation) { this.currentLocation = currentLocation; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public LocalDateTime getLastActivity() { return lastActivity; }
    public void setLastActivity(LocalDateTime lastActivity) { this.lastActivity = lastActivity; }
}