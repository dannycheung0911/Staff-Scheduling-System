package com.metro.scheduling.service;

import com.metro.scheduling.entity.*;
import com.metro.scheduling.repository.*;
import com.metro.scheduling.util.ExcelScheduleParser;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleFileRepository fileRepo;
    private final ScheduleRecordRepository recordRepo;
    private final DailyShiftCountRepository countRepo;
    private final ExcelScheduleParser parser;
    private final LogService logService;

    @Value("${app.upload-dir}")
    private String uploadDir;

    @Transactional
    public ScheduleFile uploadAndParse(MultipartFile file, String uploadedBy) throws Exception {
        // Save physical file
        String dir = uploadDir;
        new File(dir).mkdirs();
        String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path savePath = Paths.get(dir, filename);
        Files.copy(file.getInputStream(), savePath);

        // Parse
        ExcelScheduleParser.ParseResult result;
        try (var is = new FileInputStream(savePath.toFile())) {
            result = parser.parse(is, file.getOriginalFilename());
        }

        ScheduleFile info = result.getFileInfo();
        info.setFileName(filename);
        info.setFilePath(savePath.toString());
        info.setUploadedBy(uploadedBy);
        info.setUploadTime(java.time.LocalDateTime.now());
        fileRepo.save(info);

        // Save records
        result.getRecords().forEach(r -> r.setFileId(info.getId()));
        recordRepo.saveAll(result.getRecords());

        // Compute and save shift counts
        recalcShiftCounts(info.getId(), result.getShiftCounts(), result.getDateCols());

        logService.log(null, uploadedBy, "UPLOAD",
                "上传班表: " + file.getOriginalFilename() + " (fileId=" + info.getId() + ")");
        return info;
    }

    @Transactional
    public void recalcShiftCounts(Long fileId,
                                   Map<LocalDate, Map<String, Integer>> counts,
                                   List<LocalDate> dates) {
        countRepo.deleteByFileId(fileId);
        List<DailyShiftCount> toSave = new ArrayList<>();
        for (LocalDate date : dates) {
            Map<String, Integer> dayMap = counts.getOrDefault(date, Map.of());
            for (String shift : ExcelScheduleParser.MONITORED_SHIFTS) {
                DailyShiftCount dsc = new DailyShiftCount();
                dsc.setFileId(fileId);
                dsc.setWorkDate(date);
                dsc.setShiftCode(shift);
                int cnt = dayMap.getOrDefault(shift, 0);
                dsc.setCount(cnt);
                dsc.setAlert(cnt < 1);
                toSave.add(dsc);
            }
        }
        countRepo.saveAll(toSave);
    }

    /**
     * 编辑单元格后，重新计算该日期的班次汇总
     */
    @Transactional
    public List<DailyShiftCount> recalcDayFromRecords(Long fileId, LocalDate date) {
        List<ScheduleRecord> recs = recordRepo.findByFileIdAndWorkDate(fileId, date);

        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String s : ExcelScheduleParser.MONITORED_SHIFTS) counts.put(s, 0);

        for (ScheduleRecord r : recs) {
            String code = normalizeShift(r.getShiftCode());
            if (counts.containsKey(code)) {
                counts.merge(code, 1, Integer::sum);
            }
        }

        // Delete old counts for this date/file
        List<DailyShiftCount> existing = countRepo.findByFileIdAndWorkDate(fileId, date);
        countRepo.deleteAll(existing);

        List<DailyShiftCount> updated = new ArrayList<>();
        for (String shift : ExcelScheduleParser.MONITORED_SHIFTS) {
            DailyShiftCount dsc = new DailyShiftCount();
            dsc.setFileId(fileId);
            dsc.setWorkDate(date);
            dsc.setShiftCode(shift);
            int cnt = counts.get(shift);
            dsc.setCount(cnt);
            dsc.setAlert(cnt < 1);
            updated.add(dsc);
        }
        return countRepo.saveAll(updated);
    }

    /**
     * Normalize shift codes: "跟F1", "跟F2" → "F1", "F2" etc.
     */
    public String normalizeShift(String code) {
        if (code == null) return "";
        code = code.trim();
        // 跟X1 → X1
        if (code.startsWith("跟")) code = code.substring(1);
        // Remove trailing qualifiers like -03, -04
        code = code.replaceAll("-\\d+$", "");
        return code;
    }

    public List<ScheduleFile> listFiles() {
        return fileRepo.findAllByOrderByUploadTimeDesc();
    }

    public Optional<ScheduleFile> getFile(Long id) {
        return fileRepo.findById(id);
    }

    public List<ScheduleRecord> getRecords(Long fileId) {
        return recordRepo.findByFileIdOrderBySortOrderAsc(fileId);
    }

    public List<DailyShiftCount> getShiftCounts(Long fileId) {
        return countRepo.findByFileIdOrderByWorkDateAsc(fileId);
    }

    public List<DailyShiftCount> getAlerts(Long fileId) {
        return countRepo.findByFileIdAndAlertTrue(fileId);
    }

    @Transactional
    public ScheduleRecord updateCell(Long recordId, String newShiftCode, String username) {
        ScheduleRecord rec = recordRepo.findById(recordId)
                .orElseThrow(() -> new RuntimeException("记录不存在: " + recordId));
        String old = rec.getShiftCode();
        rec.setShiftCode(newShiftCode);
        rec.setManuallyEdited(true);
        rec.setEditedBy(username);
        recordRepo.save(rec);

        // Recalculate counts for that day
        recalcDayFromRecords(rec.getFileId(), rec.getWorkDate());

        logService.log(null, username, "EDIT_CELL",
                String.format("修改 [%s] %s %s: %s → %s",
                        rec.getStaffName(), rec.getWorkDate(), rec.getShiftCode(), old, newShiftCode));
        return rec;
    }

    @Transactional
    public void deleteFile(Long fileId, String username) {
        recordRepo.deleteByFileId(fileId);
        countRepo.deleteByFileId(fileId);
        fileRepo.deleteById(fileId);
        logService.log(null, username, "DELETE", "删除班表 fileId=" + fileId);
    }
}
