package com.metro.scheduling.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "schedule_file")
public class ScheduleFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    private String originalName;

    @Column(length = 10)
    private String scheduleType;  // MONTHLY, WEEKLY

    private Integer year;
    private Integer month;

    // 周班表额外信息
    private String weekRange;  // e.g. "6月15日-6月21日"

    private String stationName;

    private String uploadedBy;
    private LocalDateTime uploadTime;

    // 文件路径，用于重新解析
    private String filePath;
}
