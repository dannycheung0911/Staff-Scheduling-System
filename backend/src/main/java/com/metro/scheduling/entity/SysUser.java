package com.metro.scheduling.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "sys_user")
public class SysUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(length = 50)
    private String realName;

    @Column(length = 20)
    private String role;  // ADMIN, MANAGER, VIEWER

    private Boolean enabled = true;

    private LocalDateTime createTime;

    private LocalDateTime lastLoginTime;
}
