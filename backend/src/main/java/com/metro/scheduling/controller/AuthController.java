package com.metro.scheduling.controller;

import com.metro.scheduling.entity.SysUser;
import com.metro.scheduling.repository.UserRepository;
import com.metro.scheduling.service.LogService;
import com.metro.scheduling.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final LogService logService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> req, HttpServletRequest httpReq) {
        String username = req.get("username");
        String password = req.get("password");
        String ip = getClientIp(httpReq);

        SysUser user = userRepository.findByUsername(username).orElse(null);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            logService.log(null, username, "LOGIN", "登录失败: " + username, ip, false);
            return ResponseEntity.status(401).body(Map.of("message", "用户名或密码错误"));
        }
        if (!user.getEnabled()) {
            return ResponseEntity.status(403).body(Map.of("message", "账号已被禁用"));
        }

        user.setLastLoginTime(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
        logService.log(user.getId(), username, "LOGIN", "登录成功", ip, true);

        return ResponseEntity.ok(Map.of(
                "token", token,
                "username", user.getUsername(),
                "realName", user.getRealName() != null ? user.getRealName() : username,
                "role", user.getRole()
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<?> currentUser(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        String username = jwtUtil.getUsernameFromToken(token);
        return userRepository.findByUsername(username)
                .map(u -> ResponseEntity.ok(Map.of(
                        "username", u.getUsername(),
                        "realName", u.getRealName() != null ? u.getRealName() : u.getUsername(),
                        "role", u.getRole()
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    private String getClientIp(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        return (ip != null && !ip.isEmpty()) ? ip.split(",")[0].trim() : req.getRemoteAddr();
    }
}
