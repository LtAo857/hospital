# Star项目以便获取最新动态
qq交流群： **1081725203**
# 待完善：
        框架升级为SpringBoot3.x，集成LangChain4j。
        添加AI助手，便于患者预约挂号。
# 项目描述
        智慧医疗挂号系统，为患者、医生和管理者提供一站式医疗服务平台。
        移动端小程序为患者提供了挂号缴费、视频问诊、消息通知、就诊评价等服务，并且为管理者提供了 MIS 管理平台，可以管理医疗系统各个模块的业务数据。由于采用了先进的人脸识别和静态活体检测技术，可以识别挂号者是不是患者本人，杜绝黄牛倒卖专家号。本项目还采用了 TRTC 音视频服务，可以实现患者端和医生端的实时视频问诊功能，做到视频信号稳定清晰。

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

# 功能模块
## 患者端小程序
- 门诊挂号：按科室/医生选择号源，在线预约挂号
- 视频问诊：基于TRTC的实时视频问诊，支持提交问诊资料
- 消息通知：挂号成功通知、就诊提醒（每日定时推送）、系统消息
- 就诊评价：对医生进行星级评分和文字评价
- 电子处方：查看医生开具的电子处方
- 人脸识别：就诊时人脸核验，防止黄牛倒号
- 个人中心：就医信息卡管理、挂号记录、问诊订单、我的评价

## 管理系统（MIS）
- 科室管理：科室及子科室的增删改查
- 医生管理：医生信息、出诊计划管理
- 挂号管理：挂号记录查询与管理
- 视频问诊管理：问诊订单管理
- 系统管理：用户、角色、权限管理

# 开发环境
* 版本一：JDK15.02 + IDEA + HBase + Redis + RabbitMQ + Minio + HBuilderX
* 版本二：JDK8 + IDEA + MySQL + Redis + Minio + HBuilderX（MySQL版本，避免了部署的麻烦，更适合小白的一套智慧医疗挂号小程序）

# 项目技术栈
## 移动端：
        UniApp、Vue2.0、uView、人脸识别服务、OCR 插���、TRTC 服务

## 后端项目：
        SpringBoot、SpringMVC、MyBatis、SaToken、Quartz、WebSocket

## 前端项目：
        Vue3.0、ElementPlus、TRTC 服务

# 主要工作
+ 设计了后端项目全局异常处理、异步线程任务、WebSocket 消息推送。
+ 封装了前端MIS系统的同步/异步Ajax请求与异常处理。
+ 利用 TRTC 技术实现移动端和 Web 端的视频问诊。
+ 开发了门诊挂号、移动支付、线上问诊、出诊管理等模块。
+ 实现了消息通知系统，支持挂号成功通知、每日就诊提醒（Quartz定时任务）。
+ 实现了就诊评价系统，支持星级评分、文字评价、医生评价查看。

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
  * 腾讯云人脸识别
* 小程序appid和密钥
  * 获取方式：
    * 微信公众平台 https://mp.weixin.qq.com/?token=&lang=zh_CN
## 数据库部署
* 在 `sql/` 目录下获取建表SQL文件，导入到Navicat或其他MySQL客户端执行
