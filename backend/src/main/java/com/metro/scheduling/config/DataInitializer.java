package com.metro.scheduling.config;

import com.metro.scheduling.entity.SysUser;
import com.metro.scheduling.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        new File("./uploads").mkdirs();
        new File("./data").mkdirs();

        if (!userRepository.existsByUsername("admin")) {
            SysUser admin = new SysUser();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRealName("系统管理员");
            admin.setRole("ADMIN");
            admin.setEnabled(true);
            admin.setCreateTime(LocalDateTime.now());
            userRepository.save(admin);
            System.out.println("默认管理员账号已创建: admin / admin123");
        }

        if (!userRepository.existsByUsername("manager")) {
            SysUser manager = new SysUser();
            manager.setUsername("manager");
            manager.setPassword(passwordEncoder.encode("manager123"));
            manager.setRealName("站区长");
            manager.setRole("MANAGER");
            manager.setEnabled(true);
            manager.setCreateTime(LocalDateTime.now());
            userRepository.save(manager);
            System.out.println("默认管理员账号已创建: manager / manager123");
        }
    }
}
