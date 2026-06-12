package com.metro.scheduling.repository;

import com.metro.scheduling.entity.DailyShiftCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;

public interface DailyShiftCountRepository extends JpaRepository<DailyShiftCount, Long> {
    List<DailyShiftCount> findByFileIdOrderByWorkDateAsc(Long fileId);

    List<DailyShiftCount> findByFileIdAndAlertTrue(Long fileId);

    List<DailyShiftCount> findByFileIdAndWorkDate(Long fileId, LocalDate workDate);

    @Modifying
    @Query("DELETE FROM DailyShiftCount d WHERE d.fileId = :fileId")
    void deleteByFileId(Long fileId);
}
