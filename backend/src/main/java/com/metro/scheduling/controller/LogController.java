package com.metro.scheduling.controller;

import com.metro.scheduling.repository.OperationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final OperationLogRepository logRepo;

    @GetMapping
    public ResponseEntity<?> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "") String username) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "operateTime"));
        if (username.isEmpty()) {
            return ResponseEntity.ok(logRepo.findAll(pageable));
        }
        return ResponseEntity.ok(logRepo.findByUsernameContaining(username, pageable));
    }
}
