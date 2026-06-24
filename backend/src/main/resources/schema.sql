-- =====================================================
-- 延安三路站排班系统 - 数据库初始化脚本
-- 支持 MySQL 8.0+ / H2
-- =====================================================

CREATE DATABASE IF NOT EXISTS scheduling
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE scheduling;

-- =====================================================
-- 1. 系统用户表
-- =====================================================
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username       VARCHAR(50)  NOT NULL COMMENT '登录账号',
    password       VARCHAR(200) NOT NULL COMMENT 'BCrypt 加密密码',
    real_name      VARCHAR(50)           COMMENT '真实姓名',
    role           VARCHAR(20)  NOT NULL DEFAULT 'VIEWER'
                                COMMENT '角色: ADMIN / MANAGER / VIEWER',
    enabled        TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用',
    create_time    DATETIME              COMMENT '创建时间',
    last_login_time DATETIME             COMMENT '最后登录时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户';

-- 默认账号 (密码均为 BCrypt 加密)
-- admin / admin123
-- manager / manager123
INSERT INTO sys_user (username, password, real_name, role, enabled, create_time) VALUES
('admin',
 '$2a$10$7QwjLKv3mHzVY3k5X9BvE.Oe1s2GkqWmNpR6TdYhFjIuAlCbMoZXW',
 '系统管理员', 'ADMIN', 1, NOW()),
('manager',
 '$2a$10$N3pKlQ8RtYvXwZ2mJ5HcDOkL7sEiTgUbFqMoWhVnCrYdBeXpAjKIS',
 '站区长', 'MANAGER', 1, NOW());


-- =====================================================
-- 2. 操作日志表
-- =====================================================
DROP TABLE IF EXISTS operation_log;
CREATE TABLE operation_log (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id       BIGINT                COMMENT '操作人ID',
    username      VARCHAR(50)           COMMENT '操作人账号',
    operation     VARCHAR(100)          COMMENT '操作类型: LOGIN/UPLOAD/EDIT_CELL/DELETE',
    detail        VARCHAR(500)          COMMENT '操作详情',
    ip_address    VARCHAR(50)           COMMENT '客户端IP',
    operate_time  DATETIME              COMMENT '操作时间',
    success       TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否成功',
    PRIMARY KEY (id),
    KEY idx_username (username),
    KEY idx_operate_time (operate_time),
    KEY idx_operation (operation)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志';


-- =====================================================
-- 3. 班表文件表
-- =====================================================
DROP TABLE IF EXISTS schedule_file;
CREATE TABLE schedule_file (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    file_name      VARCHAR(200) NOT NULL COMMENT '服务器存储文件名',
    original_name  VARCHAR(200)          COMMENT '原始文件名',
    schedule_type  VARCHAR(10)           COMMENT '类型: MONTHLY / WEEKLY',
    year           INT                   COMMENT '年份',
    month          INT                   COMMENT '月份',
    week_range     VARCHAR(100)          COMMENT '周班表范围, 如 6月15日-6月21日',
    station_name   VARCHAR(100)          COMMENT '站点名称',
    uploaded_by    VARCHAR(50)           COMMENT '上传人账号',
    upload_time    DATETIME              COMMENT '上传时间',
    file_path      VARCHAR(500)          COMMENT '物理文件路径',
    PRIMARY KEY (id),
    KEY idx_year_month (year, month),
    KEY idx_upload_time (upload_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班表文件';


-- =====================================================
-- 4. 排班记录表（每人每天一行）
-- =====================================================
DROP TABLE IF EXISTS schedule_record;
CREATE TABLE schedule_record (
    id              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    file_id         BIGINT      NOT NULL COMMENT '所属班表文件ID',
    staff_name      VARCHAR(30)          COMMENT '员工姓名',
    category        VARCHAR(30)          COMMENT '类别: 值班站长/值班员/站务员等',
    cert_info       VARCHAR(100)         COMMENT '人员持证信息',
    work_date       DATE        NOT NULL COMMENT '工作日期',
    shift_code      VARCHAR(30)          COMMENT '班次: A1/A2/C1/C2/F1/F2/E2/跟F1/休/白/年/婚...',
    work_hours      DOUBLE               COMMENT '当日工时',
    name_color      VARCHAR(20)          COMMENT '姓名颜色(用于班组识别), 如 #FF0000',
    manually_edited TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '是否被手动编辑过',
    edited_by       VARCHAR(50)          COMMENT '最后编辑人',
    sort_order      INT                  COMMENT '原表行顺序',
    PRIMARY KEY (id),
    KEY idx_file_id (file_id),
    KEY idx_work_date (work_date),
    KEY idx_staff_name (staff_name),
    KEY idx_file_date (file_id, work_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排班记录';


-- =====================================================
-- 5. 每日班次计数表（预警核心）
-- =====================================================
DROP TABLE IF EXISTS daily_shift_count;
CREATE TABLE daily_shift_count (
    id          BIGINT     NOT NULL AUTO_INCREMENT COMMENT '主键',
    file_id     BIGINT     NOT NULL COMMENT '所属班表文件ID',
    work_date   DATE       NOT NULL COMMENT '统计日期',
    shift_code  VARCHAR(10) NOT NULL COMMENT '班次: A1/A2/C1/C2/F1/F2/E2',
    count       INT        NOT NULL DEFAULT 0 COMMENT '当天该班次在岗人数',
    alert       TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否预警(count<1)',
    PRIMARY KEY (id),
    UNIQUE KEY uk_file_date_shift (file_id, work_date, shift_code),
    KEY idx_file_id (file_id),
    KEY idx_alert (file_id, alert)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日班次人数统计（预警）';


-- =====================================================
-- 常用查询示例
-- =====================================================

-- 查询某班表所有预警（人数为0的班次）
-- SELECT work_date, shift_code, count
-- FROM daily_shift_count
-- WHERE file_id = 1 AND alert = 1
-- ORDER BY work_date, shift_code;

-- 查询某天各班次到岗情况
-- SELECT shift_code, count, alert
-- FROM daily_shift_count
-- WHERE file_id = 1 AND work_date = '2026-06-15'
-- ORDER BY shift_code;

-- 查询某员工某月排班
-- SELECT work_date, shift_code, work_hours
-- FROM schedule_record
-- WHERE file_id = 1 AND staff_name = '苗法盛'
-- ORDER BY work_date;

-- 查询某天休息且持有A类资质的员工（可用于替班推荐）
-- SELECT DISTINCT r.staff_name, r.cert_info
-- FROM schedule_record r
-- WHERE r.file_id = 1
--   AND r.work_date = '2026-06-15'
--   AND r.shift_code IN ('休', '年', '婚')
--   AND r.cert_info LIKE '%ATS%';

-- 查询操作日志（最近50条）
-- SELECT username, operation, detail, ip_address, operate_time, success
-- FROM operation_log
-- ORDER BY operate_time DESC
-- LIMIT 50;
