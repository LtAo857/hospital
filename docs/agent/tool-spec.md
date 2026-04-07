# Agent Tool 规范（一期）

## 只读工具
| Tool | 来源能力 | 说明 |
| --- | --- | --- |
| `getCurrentUser` | `UserService.searchUserInfo` | 查询当前登录用户 |
| `getUserCardStatus` | `UserInfoCardService.hasUserInfoCard` | 查询是否已创建就诊卡 |
| `getUserCardDetail` | `UserInfoCardService.searchUserInfoCard` | 查询就诊卡详情 |
| `searchDepartments` | `MedicalDeptService.searchMedicalDeptList` | 查询门诊科室 |
| `searchSubDepartments` | `MedicalDeptSubService.searchMedicalDeptSubList` | 查询某科室下诊室 |
| `searchDoctorsInDay` | `RegistrationService.searchDeptSubDoctorPlanInDay` | 查询指定日期出诊医生 |
| `searchRegisterDates` | `RegistrationService.searchCanRegisterInDateRange` | 查询未来 7 天可挂号日期 |
| `searchScheduleSlots` | `RegistrationService.searchDoctorWorkPlanSchedule` | 查询医生当日号源时段 |
| `listMessages` | `MessageService.searchMessageByPage` | 查询最近消息 |
| `getUnreadMessageCount` | `MessageService.searchUnreadCount` | 查询未读消息数 |

## 需确认工具
| Tool | 来源能力 | 说明 |
| --- | --- | --- |
| `checkRegistrationCondition` | `RegistrationService.checkRegisterCondition` | 检查每日上限、重复挂号等条件 |
| `createRegistrationOrder` | `RegistrationService.registerMedicalAppointment` | 确认后提交挂号 |

## 本期不开放
- 取消挂号
- 收藏/取消收藏
- 提交评价
- 视频问诊写操作
- 人脸建模与认证

## 风险策略
- 只读：自动执行
- 挂号提交：强制确认
- 未开放能力：只说明边界，不伪造执行结果
