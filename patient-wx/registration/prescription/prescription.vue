<template>
	<view class="main">
		<view class="hospital">北京市神州网络医院</view>
		<view class="title">电子处方笺</view>
		<view class="qrcode">
			<uqrcode ref="qrcode" canvas-id="qrcode" size="140" :value="uuid"></uqrcode>
		</view>
		<view class="info-container">
<!-- 			<view class="row1" >
				<view>宠物名称：{{ patient.petName }}</view>
				<view>性别：{{ patient.sex }}</view>
				<view>年龄：{{ patient.age }}岁</view>
		
			</view> -->
			<view class="row">
	<text>就诊人姓名：{{ patient.name }}</text>
			</view>
				
			<view class="row">
				<text>诊室：{{ deptSub }}</text>
			</view>
			<view class="row">
				<text>临床诊断：{{ diagnosis }}</text>
			</view>
		</view>
		<view class="rp-container">
			<view class="rp-title">Rp:</view>
			<view class="rp" v-for="one of rpList" :key="one">
				<view class="row">
					<text class="name">1. {{ one.name }}</text>
					<text class="num">×{{ one.num }}</text>
				</view>
				<view class="row">
					<text class="spec">{{ one.spec }}</text>
				</view>
				<view class="row">
					<text class="method">{{ one.method }}</text>
				</view>
			</view>
		</view>
		<view class="responsible-person">
			<view>
				<text class="person">处方医师：{{ doctor }}</text>
			</view>
		</view>
	</view>
</template>

<script>
export default {
	data() {
		return {
			uuid: '111111',
			patient: {
				name: "小明",
				sex: "公",
				age: 3,
				petName:"测试狗",
				breed:"柯基"
			},
			deptSub: "肠胃科",
			diagnosis: "上火",
			rpList: [
				{
					"name":"甲硝唑片",
					"spec":"200mg×24片",
					"method": "1片/次；每日三次；口服",
					"num":1
				},
				{
					"name":"头孢拉定胶囊",
					"spec":"250mg×24片",
					"method": "1片/次；每日两次；口服",
					"num":1
				},
			],
			doctor: "王老五"
		};
	},
	methods: {},
	onLoad(options) {
	  //   let that = this;
	  //   let registrationId = options.registrationId;
	  //   let data = {
	  //       registrationId: registrationId
	  //   };
	  //   that.ajax(that.api.searchPrescriptionByRegistrationId, 'POST', data, function(resp) {
	  //       let result = resp.data.result;
	  //       that.uuid = "111111";
			// that.patient.name = "小明";
			// that.patient.sex = "男";
			// that.patient.age = 45;
			// that.deptSub = "牙科";
			// that.diagnosis = "上火";
			// that.rpList = [
			// 	{
			// 		"name":"甲硝唑片",
			// 		"spec":"200mg×24片",
			// 		"method": "1片/次；每日三次；口服",
			// 		"num":1
			// 	},
			// 	{
			// 		"name":"头孢拉定胶囊",
			// 		"spec":"250mg×24片",
			// 		"method": "1片/次；每日两次；口服",
			// 		"num":1
			// 	},

			// ];
			// that.doctor = "王老五";
	  //       // that.patient.name = result.patientName;
	  //       // that.patient.sex = result.patientSex;
	  //       // that.patient.age = result.patientAge;
	  //       // that.deptSub = result.deptSub;
	  //       // that.diagnosis = result.diagnosis;
	  //       // that.rpList = result.rp;
	  //       // that.doctor = result.doctorName;
	  //   });
	}

};
</script>

<style lang="less">
@import url('prescription.less');
</style>
