package com.metro.scheduling.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "daily_shift_count", indexes = {
    @Index(name = "idx_dsc_file_date", columnList = "fileId, workDate")
})
public class DailyShiftCount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long fileId;
    private LocalDate workDate;

    @Column(length = 10)
    private String shiftCode;   // A1, A2, C1, C2, F1, F2, E2

    private Integer count;      // 当天该班次人数

    private Boolean alert;      // count < 1 则预警
}
