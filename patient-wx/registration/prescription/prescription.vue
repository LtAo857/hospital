<template>
	<view class="main">
		<view class="hospital">北京市神州网络医院</view>
		<view class="title">电子处方笺</view>
		<view class="qrcode">
			<uqrcode ref="qrcode" canvas-id="qrcode" size="140" :value="uuid"></uqrcode>
		</view>
		<view class="info-container">
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
			<view class="rp" v-for="(one,index) of rpList" :key="index">
				<view class="row">
					<text class="name">{{ index + 1 }}. {{ one.name || '' }}</text>
					<text class="num">×{{ one.num || '' }}</text>
				</view>
				<view class="row">
					<text class="spec">{{ one.spec || '' }}</text>
				</view>
				<view class="row">
					<text class="method">{{ one.method || '' }}</text>
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
			uuid: '',
			patient: {
				name: '',
				sex: '',
				age: null
			},
			deptSub: '',
			diagnosis: '',
			rpList: [],
			doctor: ''
		};
	},
	methods: {
		loadPrescription(registrationId) {
			let that = this;
			that.ajax(that.api.searchPrescriptionByRegistrationId, 'POST', {
				registrationId: Number(registrationId)
			}, function(resp) {
				let result = resp.data.result;
				if (!result) {
					uni.showToast({
						icon: 'none',
						title: '暂无电子处方'
					});
					setTimeout(() => {
						uni.navigateBack({
							delta: 1
						});
					}, 1200);
					return;
				}
				that.uuid = result.uuid || '';
				that.patient.name = result.patientName || '';
				that.patient.sex = result.patientSex || '';
				that.patient.age = result.patientAge;
				that.deptSub = result.deptSub || '';
				that.diagnosis = result.diagnosis || '';
				that.rpList = result.rpList || [];
				that.doctor = result.doctorName || '';
			}, false);
		}
	},
	onLoad(options) {
		let registrationId = options.registrationId;
		if (!registrationId) {
			uni.showToast({
				icon: 'none',
				title: '处方参数缺失'
			});
			setTimeout(() => {
				uni.navigateBack({
					delta: 1
				});
			}, 1200);
			return;
		}
		this.loadPrescription(registrationId);
	}
};
</script>

<style lang="less">
@import url('prescription.less');
</style>
