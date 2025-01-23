import App from './App'

// #ifndef VUE3
import Vue from 'vue'
Vue.config.productionTip = false

//使用uView组件库
import uView from 'uview-ui'
Vue.use(uView)

App.mpType = 'app'
const app = new Vue({
	...App
})
app.$mount()
// #endif

// #ifdef VUE3
// import {
// 	createSSRApp
// } from 'vue'
// export function createApp() {
// 	const app = createSSRApp(App)
// 	return {
// 		app
// 	}
// }
// #endif


let minioUrl = "http://62.234.37.187:9000/hospital"
Vue.prototype.minioUrl = minioUrl

let patientUrl = minioUrl + "/patient-wx"


Vue.prototype.patientUrl = patientUrl

Vue.prototype.tencent = {
	trtc: {
		appid: "1400796505"
	}
}

let baseUrl = "http://127.0.0.1:8092/patient-wx-api"
// let baseUrl = "http://172.20.10.8:8092/patient-wx-api"


Vue.prototype.api = {
    loginOrRegister: baseUrl + "/user/loginOrRegister",
    insertUserInfoCard: baseUrl + "/user/info/card/insert",
    updateUserInfoCard: baseUrl + "/user/info/card/update",
    searchUserInfo:baseUrl + "/user/searchUserInfo",
    searchUserInfoCard: baseUrl + "/user/info/card/searchUserInfoCard",
    updateUserInfoCard: baseUrl + "/user/info/card/update",
    hasUserInfoCard: baseUrl + "/user/info/card/hasUserInfoCard",
    searchMedicalDeptList: baseUrl + "/medical/dept/searchMedicalDeptList",
	searchIllnessList: baseUrl + "/illness/searchIllnessInfoList",
    searchMedicalDeptSubList: baseUrl + "/medical/dept/sub/searchMedicalDeptSubList",
    searchCanRegisterInDateRange: baseUrl + "/registration/searchCanRegisterInDateRange",
    searchDeptSubDoctorPlanInDay: baseUrl + "/registration/searchDeptSubDoctorPlanInDay",
    checkRegisterCondition: baseUrl + "/registration/checkRegisterCondition",  
    createFaceModel: baseUrl + "/face/auth/createFaceModel",
    verifyFaceModel: baseUrl + "/face/auth/verifyFaceModel",
	searchDoctorInfo: baseUrl + "/doctor/searchDoctorInfo",
    searchDoctorWorkPlanSchedule: baseUrl + "/registration/searchDoctorWorkPlanSchedule",
    searchDoctorInfoById: baseUrl + "/doctor/searchDoctorInfoById",
    registerMedicalAppointment: baseUrl + "/registration/registerMedicalAppointment",
    searchRegisterPaymentResult: baseUrl + "/registration/searchPaymentResult",
    searchRegistrationByPage: baseUrl + "/registration/searchRegistrationByPage",
    repayRegistration: baseUrl + "/registration/repayRegistration",
    searchRegistrationInfo: baseUrl + "/registration/searchRegistrationInfo",
    searchDeptAndSub: baseUrl + "/medical/dept/searchDeptAndSub",

    searchOnlineDoctorList: baseUrl + "/video_diagnose/searchOnlineDoctorList",
    createVideoDiagnose: baseUrl + "/video_diagnose/createVideoDiagnose",
    searchVideoDiagnosePaymentResult: baseUrl + "/video_diagnose/searchPaymentResult",
    uploadVideoDiagnoseImage: baseUrl + "/video_diagnose/uploadImage",
    searchImageByVideoDiagnoseId: baseUrl + "/video_diagnose/searchImageByVideoDiagnoseId",
    deleteVideoDiagnoseImage: baseUrl + "/video_diagnose/deleteImage",
    searchRoomId: baseUrl + "/video_diagnose/searchRoomId",
    searchUserSig: baseUrl + "/video_diagnose/searchUserSig",
    searchVideoDiagnoseByPage: baseUrl + "/video_diagnose/searchByPage",
    searchPrescriptionByRegistrationId: baseUrl + "/prescription/searchPrescriptionByRegistrationId",
	
	
	searchByPageAndId:baseUrl + "/pet/searchByPageAndId",
	addPetInfo:baseUrl + "/pet/insert",
	updatePetInfo:baseUrl + "/pet/update",
		deletePetInfo:baseUrl + "/pet/deleteByIds",
	searchById:baseUrl + "/pet/searchById",
}


Vue.prototype.ajax = function(url, method, data, fun, load) {
	let timer = null
	if (load == true || load == undefined) {
		uni.showLoading({
			title: "执行中"
		})
		timer = setTimeout(function() {
			uni.hideLoading()
		}, 60 * 1000)
	}
	uni.request({
		"url": url,
		"method": method,
		"header": {
			token: uni.getStorageSync("token")
		},
		"data": data,
		success: function(resp) {
			if (load == true || load == undefined) {
				clearTimeout(timer)
				uni.hideLoading()
			}
			if (resp.statusCode == 401) {
				uni.removeStorageSync("token")
				uni.showToast({
					icon: "error",
					title: "请登录小程序"
				})
				setTimeout(() => {
					uni.switchTab({
						"url":"/pages/mine/mine"
					})
				}, 2000);
			} else if (resp.statusCode == 200 && resp.data.code == 200) {
				let data = resp.data
				if (data.hasOwnProperty("token")) {
					let token = data.token
					uni.setStorageSync("token", token)
				}
				fun(resp)
			} else {
				uni.showToast({
					icon: "none",
					title: "执行异常"
				})
				console.error(resp.data)
			}
		},
		fail: function(error) {
			if (load == true || load == undefined) {
				clearTimeout(timer)
				uni.hideLoading()
			}
		}
	})
}
