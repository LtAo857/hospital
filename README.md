# Star项目以便获取最新动态
qq交流群： **1081725203**
# 待完善：
        框架升级为SpringBoot3.x，集成LangChain4j。
        添加AI助手，便于患者预约挂号。
# 项目描述
        智慧医疗挂号系统，为患者、医生和管理者提供一站式医疗服务平台。
        移动端小程序为患者提供了挂号、视频问诊、消息通知、就诊评价、医生收藏等服务；同时为管理者和医生提供了 MIS 管理平台，用于维护医生、科室、排班、评价、收藏等业务数据。本项目集成了 TRTC 音视频能力，可支持患者端与医生端实时视频问诊。

# 最近更新
- 小程序医生详情页补充了电话、收藏状态、评分汇总等信息
- 小程序个人中心新增“我的收藏”入口，并支持查看收藏医生列表
- 后台新增“收藏管理”页面，可查看医生/患者收藏记录
- 后台医生管理页补充医生被收藏次数展示
- 当前版本已移除挂号前的人脸识别前置校验，挂号不再依赖人脸录入与人脸验证
- 新增患者侧 AI 挂号助手页，首页与个人中心都可进入助手流程
- Agent 一期已打通“查询 → 条件校验 → 确认 → 挂号”链路，并保留 Redis 会话记忆与风险确认

# 项目结构
```
hospital
├── hospital-api-mysql       # 管理系统后端（MySQL版）
├── hospital-api             # 管理系统后端（HBase版）
├── hospital-vue             # 管理系统前端（Vue3 + ElementPlus）
├── patient-wx-api-mysql     # 小程序后端（MySQL版）
├── patient-wx-api           # 小程序后端（HBase版）
├── patient-wx               # 患者端小程序（UniApp）
├── sql                      # 数据库建表SQL
├── docs                     # 部署文档
├── Minio                    # 静态资源（图片等）
└── video                    # 视频相关资源
```

# 开发者快速导航
> 日常开发建议优先关注 MySQL 版本链路：`hospital-api-mysql`、`patient-wx-api-mysql`、`hospital-vue`、`patient-wx`。HBase 目录更适合作为历史实现对照。

| 子项目 | 面向角色 | 主要职责 | 技术栈 | 建议先看 |
| --- | --- | --- | --- | --- |
| `hospital-api-mysql` | 管理员、医生、MIS | 管理科室、诊室、医生、排班、评价、收藏、视频问诊等后台能力 | Spring Boot 2.7、MyBatis、Sa-Token、Redis、Quartz、WebSocket | `src/main/java/com/example/hospital/api/HospitalApiApplication.java`、`src/main/resources/application.yml` |
| `patient-wx-api-mysql` | 患者小程序 | 登录注册、就诊卡、挂号、消息、评价、视频问诊等患者侧接口 | Spring Boot 2.7、MyBatis、Sa-Token、Redis、Quartz | `src/main/java/com/example/hospital/patient/wx/api/PatientWxApiApplication.java`、`src/main/resources/application.yml` |
| `hospital-vue` | 管理员、医生 | MIS 管理端页面、权限菜单、后台数据维护与统计 | Vue3、Vite、Element Plus、vue-router、jQuery Ajax、WebSocket | `src/main.js`、`src/router/index.js`、`src/views/` |
| `patient-wx` | 患者 | 小程序挂号、消息、评价、收藏、视频问诊、就诊卡 | uni-app、Vue2、uView、微信插件、TRTC | `main.js`、`pages.json`、`manifest.json` |

面向 Claude/协作者的项目级说明见根目录 `CLAUDE.md`。

# 功能模块
## 患者端小程序
- 门诊挂号：按科室/医生选择号源，在线预约挂号
- 视频问诊：基于TRTC的实时视频问诊，支持提交问诊资料
- 消息通知：挂号成功通知、就诊提醒（每日定时推送）、系统消息
- 就诊评价：对医生进行星级评分和文字评价
- 医生收藏：支持收藏/取消收藏医生，在个人中心查看“我的收藏”
- 电子处方：查看医生开具的电子处方
- 个人中心：就医信息卡管理、挂号记录、问诊订单、我的评价、我的收藏
- 医生详情：支持查看医生简介、电话、评分汇总、评价列表、收藏状态

## 管理系统（MIS）
- 科室管理：科室及子科室的增删改查
- 医生管理：医生信息、出诊计划管理、收藏次数查看
- 挂号管理：挂号记录查询与管理
- 评价管理：支持按医生/患者/评分/来源查询评价记录
- 收藏管理：支持按医生/患者/手机号/时间查询收藏记录
- 视频问诊管理：问诊订单管理
- 系统管理：用户、角色、权限管理

# 开发环境
* 版本一：JDK15.02 + IDEA + HBase + Redis + RabbitMQ + Minio + HBuilderX
* 版本二：JDK8 + IDEA + MySQL + Redis + Minio + HBuilderX（MySQL版本，避免了部署的麻烦，更适合小白的一套智慧医疗挂号小程序）

# 项目技术栈
## 移动端：
        UniApp、Vue2.0、uView、OCR服务、TRTC 服务

## 后端项目：
        SpringBoot、SpringMVC、MyBatis、SaToken、Quartz、WebSocket

## 前端项目：
        Vue3.0、ElementPlus、TRTC 服务

# 代码入口与联调关系
## 后端
- `hospital-api-mysql`
  - 启动入口：`hospital-api-mysql/src/main/java/com/example/hospital/api/HospitalApiApplication.java`
  - 关键配置：`hospital-api-mysql/src/main/resources/application.yml`
  - 默认端口：`8094`
  - 上下文路径：`/hospital-api`
  - 额外关注：WebSocket 推送位于 `hospital-api-mysql/src/main/java/com/example/hospital/api/socket/WebSocketService.java`
- `patient-wx-api-mysql`
  - 启动入口：`patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/PatientWxApiApplication.java`
  - 关键配置：`patient-wx-api-mysql/src/main/resources/application.yml`
  - 默认端口：`8092`
  - 上下文路径：`/patient-wx-api`
  - 额外关注：每日就诊提醒 Quartz 配置位于 `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/config/QuartzConfig.java`

## 管理端前端 `hospital-vue`
- 接口根地址定义在 `hospital-vue/src/main.js`，默认请求 `http://localhost:8094/hospital-api`
- 路由集中在 `hospital-vue/src/router/index.js`
- 页面主要位于 `hospital-vue/src/views/`
- 当前采用“全局 `$http` + 页面直接调接口”的组织方式，排查联调问题时优先查看 `src/main.js`

## 患者端小程序 `patient-wx`
- 接口根地址定义在 `patient-wx/main.js`，默认请求 `http://127.0.0.1:8092/patient-wx-api`
- 页面与分包定义在 `patient-wx/pages.json`
- 小程序与插件配置位于 `patient-wx/manifest.json`
- 当前统一通过 `main.js` 里的 `ajax()` 封装请求与 token 处理

## 联调关系
- `hospital-vue` 对接 `hospital-api-mysql`
- `patient-wx` 对接 `patient-wx-api-mysql`
- 两套前端都依赖后端 `application.yml` 中的数据库、Redis、文件存储等配置正确可用
- 视频问诊能力涉及前后端共同配置 TRTC 参数

# 启动建议
1. 先导入 `sql/` 下的 MySQL 建表脚本。
2. 分别检查 `hospital-api-mysql` 与 `patient-wx-api-mysql` 的 `application.yml`，替换为本地数据库、Redis、文件存储与第三方服务配置。
3. 先启动两个后端，再根据需要启动 `hospital-vue` 或使用 HBuilderX / 微信开发者工具打开 `patient-wx`。
4. 如果只是熟悉代码，建议按“后端入口类/配置 → 前端入口文件/路由或页面配置 → 目标业务页面/控制器”的顺序阅读。

# Agent 一期改造说明
- 当前仓库已在 `patient-wx-api-mysql` 新增患者侧 Agent 编排层，入口接口为 `POST /agent/chat`。
- 当前仓库已在 `patient-wx` 新增独立页面 `agent/chat/chat`，作为“AI挂号助手”首期入口。
- 首页 `patient-wx/pages/index/index.vue` 与个人中心 `patient-wx/pages/mine/mine.vue` 已接入 AI 助手入口。
- 一期采用“无模型 Agent 编排骨架”：基于规则引擎、Tool 封装、Redis 会话记忆与确认机制驱动，不依赖真实模型 API。
- 一期已覆盖：科室查询、诊室查询、医生查询、号源日期/时段查询、就诊卡状态查询、消息查询、确认后挂号。
- 一期挂号写操作保持强确认，用户需先完成条件校验，再在助手中确认后才会真正执行挂号。
- 一期暂不开放：取消挂号、评价提交、收藏写操作、视频问诊写操作、人脸认证流程。
- 详细设计见 `docs/agent/system-prompt.md`、`docs/agent/tool-spec.md`、`docs/agent/architecture.md`。

# 敏感配置提醒
- 两个后端的 `application.yml` 中包含数据库、Redis、微信、腾讯云/TRTC 等示例配置位置。
- 这些值只适合作为配置项定位参考，不能直接视为可用凭证；本地运行前请全部替换为自己的环境参数。
- 更新文档或截图时，不要扩散任何真实密钥、密码、AppSecret、SecretKey。

# 主要工作
+ 设计了后端项目全局异常处理、异步线程任务、WebSocket 消息推送。
+ 封装了前端MIS系统的同步/异步Ajax请求与异常处理。
+ 利用 TRTC 技术实现移动端和 Web 端的视频问诊。
+ 开发了门诊挂号、移动支付、线上问诊、出诊管理等模块。
+ 实现了消息通知系统，支持挂号成功通知、每日就诊提醒（Quartz定时任务）。
+ 实现了就诊评价系统，支持星级评分、文字评价、医生评价查看。
+ 实现了医生收藏功能，支持小程序收藏、个人中心查看、后台收藏管理。

![输入图片说明](Minio/patient-wx/banner/banner-1.jpg)
![输入图片说明](Minio/patient-wx/swiper/swiper-1.jpg)
![输入图片说明](swiper/1721383236227.jpg)
![输入图片说明](swiper/swiper-5.jpg)

# 部署流程（具体见gitee代码里的docs文档，mysql版本）
## Web工具
* 下载地址
  * 通过网盘分享的文件：Web开发工具
  链接: https://pan.baidu.com/s/1k0F6OpL350b-qjsCfwFOHg?pwd=aa2w 提取码: aa2w
* 文档
    * https://blog.csdn.net/weixin_53175511/article/details/150985343?fromshare=blogdetail&sharetype=blogdetail&sharerId=150985343&sharerefer=PC&sharesource=weixin_53175511&sharefrom=from_link
## 后端部署
* 软件
  * IDEA 2025
* 环境
    * JDK 8
    * Maven
    * Redis
    * MySQL
* 代码
  * hospital-api-mysql 为管理系统的后端代码
  * patient-wx-api-mysql 为小程序的后端代码
  * 将代码导入到IDEA，同时修改application.yml文件中的数据库、Redis等配置，即可运行
## 前端部署
* 环境
    * Node.js（建议通过nvm版本控制器下载）20.19.3
* 软件
    * VSCode
      * `npm i` 安装依赖
      * `npm run dev` 运行前端代码
## 小程序部署
* 软件
   * HBuilderX
   * 微信开发者工具
* 环境
  * OCR扫描插件
* 小程序appid和密钥
  * 获取方式：
    * 微信公众平台 https://mp.weixin.qq.com/?token=&lang=zh_CN
## 数据库部署
* 在 `sql/` 目录下获取建表SQL文件，导入到Navicat或其他MySQL客户端执行
