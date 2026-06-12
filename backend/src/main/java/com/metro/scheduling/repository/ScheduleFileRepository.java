package com.metro.scheduling.repository;

import com.metro.scheduling.entity.ScheduleFile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ScheduleFileRepository extends JpaRepository<ScheduleFile, Long> {
    List<ScheduleFile> findByYearAndMonthOrderByUploadTimeDesc(Integer year, Integer month);
    List<ScheduleFile> findAllByOrderByUploadTimeDesc();
}
