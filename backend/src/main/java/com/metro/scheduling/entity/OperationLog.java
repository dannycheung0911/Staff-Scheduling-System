package com.metro.scheduling.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "operation_log")
public class OperationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String username;

    @Column(length = 100)
    private String operation;  // LOGIN, UPLOAD, EDIT_CELL, DELETE

    @Column(length = 500)
    private String detail;

    @Column(length = 50)
    private String ipAddress;

    private LocalDateTime operateTime;

    private Boolean success = true;
}
