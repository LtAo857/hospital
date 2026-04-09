# Agent Tool 规范

## 只读工具
| Tool | 来源能力 | 说明 |
| --- | --- | --- |
| `getCurrentUser` | `UserService.searchUserInfo` | 查询当前登录用户 |
| `getUserCardStatus` | `UserInfoCardService.hasUserInfoCard` | 查询是否已创建就诊卡 |
| `getUserCardDetail` | `UserInfoCardService.searchUserInfoCard` | 查询就诊卡详情 |
| `searchDepartments` | `MedicalDeptService.searchMedicalDeptList` | 查询门诊科室 |
| `searchSubDepartments` | `MedicalDeptSubService.searchMedicalDeptSubList` | 查询科室下诊室 |
| `searchDoctorsInDay` | `RegistrationService.searchDeptSubDoctorPlanInDay` | 查询指定日期出诊医生 |
| `searchRegisterDates` | `RegistrationService.searchCanRegisterInDateRange` | 查询近 7 天可挂号日期 |
| `searchScheduleSlots` | `RegistrationService.searchDoctorWorkPlanSchedule` | 查询医生当日时段 |
| `listMessages` | `MessageService.searchMessageByPage` | 查询消息列表 |
| `getUnreadMessageCount` | `MessageService.searchUnreadCount` | 查询未读消息数 |

## 需确认工具
| Tool | 来源能力 | 说明 |
| --- | --- | --- |
| `checkRegistrationCondition` | `RegistrationService.checkRegisterCondition` | 检查每日上限、重复挂号等条件 |
| `createRegistrationOrder` | `RegistrationService.registerMedicalAppointment` | 确认后提交挂号 |

## 当前工具约束
- `searchDoctorsInDay`
  - 不再要求医生必须存在价格记录才能被查出
  - 医生无价格记录时，价格按 `0.00` 兜底返回
- `searchScheduleSlots`
  - 返回真实存在的时段记录
  - 自动挂号阶段会过滤掉当天已过时段
- `createRegistrationOrder`
  - 只能在用户确认后执行

## 本期不开放
- 取消挂号
- 收藏/取消收藏
- 提交评价
- 视频问诊写操作
- 人脸建模与认证

## 风险策略
- 只读操作：自动执行
- 挂号提交：强确认
- 超出边界能力：明确说明，不伪造执行结果
