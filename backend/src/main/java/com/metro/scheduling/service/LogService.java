package com.metro.scheduling.service;

import com.metro.scheduling.entity.OperationLog;
import com.metro.scheduling.repository.OperationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LogService {

    private final OperationLogRepository logRepository;

    public void log(Long userId, String username, String operation, String detail, String ip, boolean success) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setOperation(operation);
        log.setDetail(detail);
        log.setIpAddress(ip);
        log.setSuccess(success);
        log.setOperateTime(LocalDateTime.now());
        logRepository.save(log);
    }

    public void log(Long userId, String username, String operation, String detail) {
        log(userId, username, operation, detail, null, true);
    }
}
