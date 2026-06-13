package com.metro.scheduling.controller;

import com.metro.scheduling.entity.*;
import com.metro.scheduling.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    /**
     * 上传班表文件
     * @param scheduleType 前端传入: MONTHLY(月班表) 或 WEEKLY(周班表)
     */
    @PostMapping("/upload")
    public ResponseEntity<?> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "scheduleType", defaultValue = "MONTHLY") String scheduleType,
            Authentication auth) {
        try {
            if (!scheduleType.equals("MONTHLY") && !scheduleType.equals("WEEKLY")) {
                return ResponseEntity.badRequest().body(Map.of("message", "scheduleType 必须为 MONTHLY 或 WEEKLY"));
            }
            ScheduleFile result = scheduleService.uploadAndParse(file, scheduleType, auth.getName());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "解析失败: " + e.getMessage()));
        }
    }

    @GetMapping("/files")
    public ResponseEntity<List<ScheduleFile>> listFiles() {
        return ResponseEntity.ok(scheduleService.listFiles());
    }

    @GetMapping("/records/{fileId}")
    public ResponseEntity<List<ScheduleRecord>> getRecords(@PathVariable Long fileId) {
        return ResponseEntity.ok(scheduleService.getRecords(fileId));
    }

    @GetMapping("/counts/{fileId}")
    public ResponseEntity<List<DailyShiftCount>> getCounts(@PathVariable Long fileId) {
        return ResponseEntity.ok(scheduleService.getShiftCounts(fileId));
    }

    @GetMapping("/alerts/{fileId}")
    public ResponseEntity<List<DailyShiftCount>> getAlerts(@PathVariable Long fileId) {
        return ResponseEntity.ok(scheduleService.getAlerts(fileId));
    }

    @PutMapping("/record/{recordId}")
    public ResponseEntity<?> updateCell(
            @PathVariable Long recordId,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        try {
            String newCode = body.get("shiftCode");
            ScheduleRecord updated = scheduleService.updateCell(recordId, newCode, auth.getName());
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/file/{fileId}")
    public ResponseEntity<?> deleteFile(@PathVariable Long fileId, Authentication auth) {
        scheduleService.deleteFile(fileId, auth.getName());
        return ResponseEntity.ok(Map.of("message", "已删除"));
    }
}
