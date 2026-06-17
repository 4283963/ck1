# 疫苗冷链运输协同系统

专门管理冷链疫苗跨省运输的协同系统，重点解决冷藏车温度传感器卡死或上传假数据的问题。

## 项目结构

```
ck1/
├── backend/                 # Spring Boot 后端
│   ├── src/main/java/com/coldchain/vaccine/
│   │   ├── config/          # 配置类（CORS等）
│   │   ├── controller/      # REST API 控制器
│   │   ├── dto/             # 数据传输对象
│   │   ├── entity/          # JPA 实体类
│   │   ├── repository/      # 数据访问层
│   │   └── service/         # 业务逻辑层
│   ├── src/main/resources/
│   │   ├── application.yml  # 应用配置
│   │   ├── schema.sql       # 数据库建表脚本
│   │   └── data.sql         # 示例数据
│   ├── mock_transport_client.py  # 模拟数据上报测试脚本
│   └── pom.xml
└── frontend/                # Vue 3 前端
    ├── src/
    │   ├── views/           # 页面组件
    │   ├── router/          # 路由配置
    │   ├── api/             # API 接口
    │   └── utils/           # 工具函数
    ├── index.html
    ├── vite.config.js
    └── package.json
```

## 核心功能

### 后端 (Spring Boot)
1. **数据接收接口** `POST /api/transport/report` - 每10秒接收冷藏车上报的GPS和温度数据
2. **温度异常检测** - 连续3次上报温度完全相同，自动触发"数据失真预警"
3. **车辆管理** - 车次基本信息的增删改查
4. **预警管理** - 查询未处理预警、标记预警已处理
5. **运输记录查询** - 按车次查询历史上报记录

### 前端 (Vue 3 + Element Plus)
1. **协同大屏** - 展示在运车辆统计、预警列表、车次列表
2. **预警弹窗** - 检测到数据失真时自动弹出红色预警
3. **车次详情** - 展示车辆基本信息、实时位置地图（Leaflet）、运输记录、预警记录
4. **实时刷新** - 自动轮询后端获取最新数据

## 快速开始

### 1. 启动 PostgreSQL 数据库

```bash
# 创建数据库
psql -U postgres -c "CREATE DATABASE vaccine_coldchain;"
```

数据库连接配置在 `backend/src/main/resources/application.yml`:
```yaml
url: jdbc:postgresql://localhost:5432/vaccine_coldchain
username: postgres
password: postgres
```

### 2. 启动后端服务

```bash
cd backend
mvn spring-boot:run
```
服务启动后访问: http://localhost:8080/api

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```
前端访问: http://localhost:5173

### 4. 运行模拟数据上报

```bash
cd backend
python3 mock_transport_client.py
```
该脚本会模拟5辆冷藏车每10秒上报一次数据，其中 `沪B·66666` 会模拟传感器卡死（温度不变），用于测试预警功能。

## API 接口列表

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/transport/report | 上报运输数据（GPS+温度） |
| GET | /api/transport/vehicle/{id} | 查询车辆的全部运输记录 |
| GET | /api/transport/vehicle/{id}/latest | 查询车辆最新运输记录 |
| GET | /api/vehicles | 获取全部车辆列表 |
| GET | /api/vehicles/{id} | 获取单辆车详情 |
| POST | /api/vehicles | 新增车辆 |
| PUT | /api/vehicles/{id} | 更新车辆信息 |
| DELETE | /api/vehicles/{id} | 删除车辆 |
| GET | /api/alerts | 获取全部预警列表 |
| GET | /api/alerts/unresolved | 获取未处理预警列表 |
| PUT | /api/alerts/{id}/resolve | 标记预警为已处理 |

## 预警触发逻辑

在 [TransportService.java](backend/src/main/java/com/coldchain/vaccine/service/TransportService.java) 中实现：

- 每收到一条上报数据，查询该车最近3条记录
- 如果3条记录的温度值完全相同（`BigDecimal.compareTo == 0`），且该车当前没有未处理的同类预警
- 则自动创建一条 `DATA_DISTORTION`（数据失真）预警，级别为 `HIGH`
- 前端轮询到新预警后，自动弹出红色预警弹窗

## 技术栈

**后端**:
- Java 17 + Spring Boot 3.2
- Spring Data JPA
- PostgreSQL
- Lombok

**前端**:
- Vue 3 (Composition API)
- Vue Router 4
- Element Plus
- Axios
- Leaflet (地图)
- Vite
