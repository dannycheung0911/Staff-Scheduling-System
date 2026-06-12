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
     */
    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file,
                                    Authentication auth) {
        try {
            ScheduleFile result = scheduleService.uploadAndParse(file, auth.getName());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "解析失败: " + e.getMessage()));
        }
    }

    /**
     * 获取所有班表文件列表
     */
    @GetMapping("/files")
    public ResponseEntity<List<ScheduleFile>> listFiles() {
        return ResponseEntity.ok(scheduleService.listFiles());
    }

    /**
     * 获取某班表的所有排班记录
     */
    @GetMapping("/records/{fileId}")
    public ResponseEntity<List<ScheduleRecord>> getRecords(@PathVariable Long fileId) {
        return ResponseEntity.ok(scheduleService.getRecords(fileId));
    }

    /**
     * 获取某班表各天班次计数（含预警状态）
     */
    @GetMapping("/counts/{fileId}")
    public ResponseEntity<List<DailyShiftCount>> getCounts(@PathVariable Long fileId) {
        return ResponseEntity.ok(scheduleService.getShiftCounts(fileId));
    }

    /**
     * 获取预警列表（count=0的班次）
     */
    @GetMapping("/alerts/{fileId}")
    public ResponseEntity<List<DailyShiftCount>> getAlerts(@PathVariable Long fileId) {
        return ResponseEntity.ok(scheduleService.getAlerts(fileId));
    }

    /**
     * 编辑单元格班次
     */
    @PutMapping("/record/{recordId}")
    public ResponseEntity<?> updateCell(@PathVariable Long recordId,
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

    /**
     * 删除班表
     */
    @DeleteMapping("/file/{fileId}")
    public ResponseEntity<?> deleteFile(@PathVariable Long fileId, Authentication auth) {
        scheduleService.deleteFile(fileId, auth.getName());
        return ResponseEntity.ok(Map.of("message", "已删除"));
    }
}
