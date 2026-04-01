<template>
	<view>
		<view class="date-container">
			<view class="item" v-for="one in dateList" :key="one.date">
				<text class="day">{{ one.day }}</text>
				<view :class="one.date == date ? 'selector active' : 'selector'" @tap="clickDateHandle(one.date)">
					<text class="date">{{ one.dateOfMonth }}</text>
					<text :class="one.status == noSlotStatus ? 'status gray' : 'status'">{{ one.status }}</text>
				</view>
			</view>
		</view>
		<view class="doctor" v-for="one in doctorList" :key="one.id" @tap="goDoctorDetail(one.id)">
			<u-avatar :src="one.photo" size="45"></u-avatar>
			<view class="info">
				<view class="row">
					<text class="name">{{ one.name }}</text>
					<text class="job">{{ one.job }}</text>
					<button class="btn" @tap.stop="clickBtnHandle(one.id, date)">Book</button>
				</view>
				<view class="row">
					<text class="num">Total: {{ one.maximum }}</text>
					<text class="price">RMB {{ one.price }}</text>
				</view>
				<view class="row">
					<rich-text class="desc" :nodes="one.description"></rich-text>
				</view>
			</view>
		</view>
		<u-empty
			v-if="showEmpty"
			mode="list"
			text="No doctors"
			width="150"
			icon="http://cdn.uviewui.com/uview/empty/order.png"
		></u-empty>
	</view>
</template>

<script>
let dayjs = require('dayjs');

const LIMIT_REACHED = '\u5df2\u7ecf\u8fbe\u5230\u5f53\u5929\u6302\u53f7\u4e0a\u9650';
const DUPLICATED = '\u5df2\u7ecf\u6302\u8fc7\u8be5\u8bca\u5ba4\u7684\u53f7';
const NO_FACE_MODEL = '\u4e0d\u5b58\u5728\u9762\u90e8\u6a21\u578b';
const NO_FACE_AUTH = '\u5f53\u65e5\u6ca1\u6709\u4eba\u8138\u9a8c\u8bc1\u8bb0\u5f55';

export default {
	data() {
		return {
			deptSubId: null,
			showEmpty: false,
			date: dayjs().format('YYYY-MM-DD'),
			dateList: [],
			doctorList: [],
			noSlotStatus: '\u65e0\u53f7'
		};
	},
	methods: {
		searchCanRegisterInDateRange: function(ref, deptSubId) {
			let data = {
				deptSubId: deptSubId,
				startDate: dayjs().format('YYYY-MM-DD'),
				endDate: dayjs().add(6, 'day').format('YYYY-MM-DD')
			};
			ref.ajax(ref.api.searchCanRegisterInDateRange, 'POST', data, function(resp) {
				let result = resp.data.result || [];
				let weekNames = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
				for (let one of result) {
					one.day = weekNames[dayjs(one.date).day()];
					one.dateOfMonth = dayjs(one.date).date();
				}
				ref.dateList = result;
			}, false);
		},
		searchDeptSubDoctorPlanInDay: function(ref) {
			let data = {
				deptSubId: ref.deptSubId,
				date: ref.date
			};
			ref.ajax(ref.api.searchDeptSubDoctorPlanInDay, 'POST', data, function(resp) {
				let result = resp.data.result || [];
				for (let one of result) {
					one.photo = ref.doctorPhotoUrl(one.photo);
				}
				ref.doctorList = result;
				ref.showEmpty = result.length === 0;
			}, false);
		},
		clickDateHandle: function(date) {
			this.date = date;
			this.searchDeptSubDoctorPlanInDay(this);
		},
		clickBtnHandle: function(id, date) {
			let that = this;
			that.ajax(
				that.api.checkRegisterCondition,
				'POST',
				{
					deptSubId: that.deptSubId,
					date: that.date
				},
				function(resp) {
					let result = resp.data.result || '';
					if (result === LIMIT_REACHED) {
						uni.showToast({
							icon: 'none',
							title: 'Daily limit reached'
						});
					} else if (result === DUPLICATED) {
						uni.showToast({
							icon: 'none',
							title: 'Already booked today'
						});
					} else if (result === NO_FACE_MODEL) {
						uni.showModal({
							title: 'Notice',
							content: 'Face data is required before booking.',
							confirmText: 'Create',
							success: function(modalResp) {
								if (modalResp.confirm) {
									uni.navigateTo({
										url: '/user/face_camera/face_camera?mode=create'
									});
								}
							}
						});
					} else if (result === NO_FACE_AUTH) {
						uni.showModal({
							title: 'Notice',
							content: 'Face verification is required today.',
							confirmText: 'Verify',
							success: function(modalResp) {
								if (modalResp.confirm) {
									uni.navigateTo({
										url: '/user/face_camera/face_camera?mode=verificate'
									});
								}
							}
						});
					} else {
						uni.navigateTo({
							url: `/registration/doctor_schedule/doctor_schedule?deptSubId=${that.deptSubId}&doctorId=${id}&date=${date}`
						});
					}
				},
				false
			);
		},
		goDoctorDetail: function(doctorId) {
			uni.navigateTo({
				url: `/display/doctor_detail/doctor_detail?doctorId=${doctorId}&deptSubId=${this.deptSubId}&date=${this.date}`
			});
		}
	},
	onLoad: function(options) {
		this.deptSubId = options.deptSubId;
		uni.setNavigationBarTitle({
			title: options.deptSubName
		});
		this.searchCanRegisterInDateRange(this, this.deptSubId);
		this.searchDeptSubDoctorPlanInDay(this);
	}
};
</script>

<style lang="less">
@import url(dept_sub_plan.less);
</style>
