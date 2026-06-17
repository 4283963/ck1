-- 疫苗冷链运输协同系统 数据库初始化脚本
-- PostgreSQL 12+

-- 创建数据库
-- CREATE DATABASE vaccine_coldchain;

-- 切换到该数据库后执行以下脚本

-- 车辆表
CREATE TABLE IF NOT EXISTS vehicles (
    id BIGSERIAL PRIMARY KEY,
    plate_number VARCHAR(20) NOT NULL UNIQUE,
    driver_name VARCHAR(50) NOT NULL,
    driver_phone VARCHAR(20),
    vaccine_type VARCHAR(50) NOT NULL,
    vaccine_batch VARCHAR(50),
    vaccine_count INTEGER,
    origin_province VARCHAR(20) NOT NULL,
    origin_city VARCHAR(20),
    dest_province VARCHAR(20) NOT NULL,
    dest_city VARCHAR(20),
    departure_time TIMESTAMP,
    expected_arrival_time TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT '在途',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 运输记录表
CREATE TABLE IF NOT EXISTS transport_records (
    id BIGSERIAL PRIMARY KEY,
    vehicle_id BIGINT NOT NULL,
    plate_number VARCHAR(20) NOT NULL,
    temperature DECIMAL(5, 2) NOT NULL,
    latitude DECIMAL(10, 6) NOT NULL,
    longitude DECIMAL(10, 6) NOT NULL,
    location_address VARCHAR(200),
    report_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_transport_vehicle_id ON transport_records(vehicle_id);
CREATE INDEX IF NOT EXISTS idx_transport_report_time ON transport_records(report_time);

-- 预警表
CREATE TABLE IF NOT EXISTS alerts (
    id BIGSERIAL PRIMARY KEY,
    vehicle_id BIGINT NOT NULL,
    plate_number VARCHAR(20) NOT NULL,
    alert_type VARCHAR(50) NOT NULL,
    alert_level VARCHAR(20) NOT NULL,
    alert_message VARCHAR(500) NOT NULL,
    temperature DECIMAL(5, 2),
    latitude DECIMAL(10, 6),
    longitude DECIMAL(10, 6),
    is_resolved BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_time TIMESTAMP,
    resolved_by VARCHAR(50),
    alert_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_alerts_vehicle_id ON alerts(vehicle_id);
CREATE INDEX IF NOT EXISTS idx_alerts_is_resolved ON alerts(is_resolved);
