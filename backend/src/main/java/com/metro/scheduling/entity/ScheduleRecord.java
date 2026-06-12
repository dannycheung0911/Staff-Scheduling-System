package com.metro.scheduling.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "schedule_record", indexes = {
    @Index(name = "idx_file_id", columnList = "fileId"),
    @Index(name = "idx_work_date", columnList = "workDate"),
    @Index(name = "idx_staff_name", columnList = "staffName")
})
public class ScheduleRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long fileId;

    @Column(length = 30)
    private String staffName;

    @Column(length = 30)
    private String category;      // 值班站长、值班员 等

    @Column(length = 20)
    private String certInfo;      // 人员持证信息

    private LocalDate workDate;

    @Column(length = 30)
    private String shiftCode;     // A1, A2, C1, C2, F1, F2, E2, 休, 白, 跟F1...

    private Double workHours;

    // 颜色标记（用于分组识别）：从Excel提取RGB
    @Column(length = 20)
    private String nameColor;

    // 是否被手动编辑过
    private Boolean manuallyEdited = false;
    private String editedBy;

    private Integer sortOrder;    // 保持原表顺序
}
