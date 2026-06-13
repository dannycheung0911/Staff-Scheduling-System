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
import java.time.LocalDateTime;
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
    public ScheduleFile uploadAndParse(MultipartFile file, String scheduleType, String uploadedBy) throws Exception {
        // 1. 保存物理文件
        new File(uploadDir).mkdirs();
        String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path savePath = Paths.get(uploadDir, filename);
        Files.copy(file.getInputStream(), savePath);

        // 2. 解析 Excel（传入前端选择的类型）
        ExcelScheduleParser.ParseResult result;
        try (var is = new FileInputStream(savePath.toFile())) {
            result = parser.parse(is, file.getOriginalFilename(), scheduleType);
        }

        // 3. 保存文件信息
        ScheduleFile info = result.getFileInfo();
        info.setFileName(filename);
        info.setFilePath(savePath.toString());
        info.setUploadedBy(uploadedBy);
        info.setUploadTime(LocalDateTime.now());
        fileRepo.save(info);

        // 4. 保存员工排班记录
        result.getRecords().forEach(r -> r.setFileId(info.getId()));
        recordRepo.saveAll(result.getRecords());

        // 5. 保存班次汇总（预警数据）
        saveShiftCounts(info.getId(), result.getDates(), result.getShiftCountMap());

        logService.log(null, uploadedBy, "UPLOAD",
                "上传班表: " + file.getOriginalFilename() + " (" + scheduleType + ", fileId=" + info.getId() + ")");
        return info;
    }

    private void saveShiftCounts(Long fileId, List<LocalDate> dates, Map<String, Integer> countMap) {
        countRepo.deleteByFileId(fileId);
        List<DailyShiftCount> toSave = new ArrayList<>();
        for (LocalDate date : dates) {
            for (String shift : ExcelScheduleParser.MONITORED_SHIFTS) {
                String key = shift + "_" + date;
                int cnt = countMap.getOrDefault(key, 0);
                DailyShiftCount dsc = new DailyShiftCount();
                dsc.setFileId(fileId);
                dsc.setWorkDate(date);
                dsc.setShiftCode(shift);
                dsc.setCount(cnt);
                dsc.setAlert(cnt < 1);
                toSave.add(dsc);
            }
        }
        countRepo.saveAll(toSave);
    }

    /** 编辑单元格后重新计算该天汇总 */
    @Transactional
    public List<DailyShiftCount> recalcDayFromRecords(Long fileId, LocalDate date) {
        List<ScheduleRecord> recs = recordRepo.findByFileIdAndWorkDate(fileId, date);

        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String s : ExcelScheduleParser.MONITORED_SHIFTS) counts.put(s, 0);

        for (ScheduleRecord r : recs) {
            String code = normalizeShift(r.getShiftCode());
            if (counts.containsKey(code)) counts.merge(code, 1, Integer::sum);
        }

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

    /** 跟F1 → F1，跟E2 → E2，A1-04 → A1 */
    public String normalizeShift(String code) {
        if (code == null) return "";
        code = code.trim();
        if (code.startsWith("跟")) code = code.substring(1);
        code = code.replaceAll("[-_]\\d+$", "");
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

        recalcDayFromRecords(rec.getFileId(), rec.getWorkDate());

        logService.log(null, username, "EDIT_CELL",
                String.format("[%s] %s %s: %s → %s", rec.getStaffName(), rec.getWorkDate(),
                        rec.getShiftCode(), old, newShiftCode));
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
