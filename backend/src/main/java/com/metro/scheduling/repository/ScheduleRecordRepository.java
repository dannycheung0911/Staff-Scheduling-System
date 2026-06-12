package com.metro.scheduling.repository;

import com.metro.scheduling.entity.ScheduleRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;

public interface ScheduleRecordRepository extends JpaRepository<ScheduleRecord, Long> {
    List<ScheduleRecord> findByFileIdOrderBySortOrderAsc(Long fileId);

    List<ScheduleRecord> findByFileIdAndWorkDate(Long fileId, LocalDate workDate);

    @Modifying
    @Query("DELETE FROM ScheduleRecord r WHERE r.fileId = :fileId")
    void deleteByFileId(Long fileId);

    @Query("SELECT DISTINCT r.staffName FROM ScheduleRecord r WHERE r.fileId = :fileId ORDER BY r.staffName")
    List<String> findDistinctStaffByFileId(Long fileId);

    List<ScheduleRecord> findByFileIdAndStaffName(Long fileId, String staffName);
}
