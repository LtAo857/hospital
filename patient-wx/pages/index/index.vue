<template>
	<view class="page">
		<u-swiper height="230" interval="10000" :list="list1" easingFunction="linear" radius="0" indicator></u-swiper>
		<view class="agent-entry" @tap="goAgent">
			<view>
				<text class="agent-title">AI挂号助手</text>
				<text class="agent-desc">按科室、日期、医生、时段一步步帮你完成挂号</text>
			</view>
			<u-icon name="arrow-right" size="18" color="#1296db"></u-icon>
		</view>

<!-- 		<view class="agent-entry react-entry" @tap="goReactAgent">
			<view>
				<text class="agent-title">传统Agent挂号助手</text>
				<text class="agent-desc">第二套 ReAct 架构，逐轮调用工具完成查询、确认和挂号</text>
			</view>
			<u-icon name="arrow-right" size="18" color="#2d6a9f"></u-icon>
		</view>

		<view class="agent-entry cc-entry" @tap="goCcAgent">
			<view>
				<text class="agent-title">CC Agent挂号助手</text>
				<text class="agent-desc">第三套 Claude Code 风格架构，先规划再自动串行推进到可确认结果</text>
			</view>
			<u-icon name="arrow-right" size="18" color="#1f7a5d"></u-icon>
		</view> -->

		<view class="doctor-container">
			<view class="title-row">
				<text class="title">名医专家</text>
				<u-icon label="更多" labelPos="left" size="15" name="arrow-right" @click="navigatorHandle('doctor')"></u-icon>
			</view>
			<u-tabs :list="tab.list" @click="clickTabHandle"></u-tabs>
			<view class="panel">
				<view class="doctor" v-for="one in doctor" :key="one.id" @tap="goDoctorDetail(one.id)">
					<u-avatar :src="one.photo" size="45"></u-avatar>
					<view class="info">
						<view class="row">
							<text class="name">{{ one.name }}</text>
							<text class="job">{{ one.job }}</text>
						</view>
						<view class="row">
							<text class="remark">{{ one.remark }}</text>
						</view>
						<view class="row">
							<rich-text class="desc">{{ one.description }}</rich-text>
						</view>
					</view>
				</view>
			</view>
		</view>

		<view class="dept-container">
			<view class="title-row">
				<text class="title">科室信息</text>
				<u-icon label="更多" labelPos="left" size="15" name="arrow-right" @click="navigatorHandle('dept')"></u-icon>
			</view>
			<u-tabs :list="tab.list" @click="clickTabHandle"></u-tabs>
			<view class="panel">
				<view class="doctor" v-for="one in dept" :key="one.id">
					<view class="info">
						<view class="row">
							<text class="name">{{ one.name }}</text>
							<text class="job"></text>
						</view>
						<view class="row">
							<text class="remark"></text>
						</view>
						<view class="row">
							<rich-text class="desc">{{ one.description }}</rich-text>
						</view>
					</view>
				</view>
			</view>
		</view>

		<view class="dept-container">
			<view class="title-row">
				<text class="title">疾病百科</text>
				<u-icon label="更多" labelPos="left" size="15" name="arrow-right" @click="navigatorHandle('illness')"></u-icon>
			</view>
			<u-tabs :list="tab.list" @click="clickTabHandle"></u-tabs>
			<view class="panel">
				<view class="doctor" v-for="one in illness" :key="one.id">
					<view class="info">
						<view class="row">
							<text class="name">{{ one.name }}</text>
							<text class="job"></text>
						</view>
						<view class="row">
							<text class="remark"></text>
						</view>
						<view class="row">
							<rich-text class="desc">{{ one.description }}</rich-text>
						</view>
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
export default {
	data() {
		return {
			page: 1,
			length: 50,
			img: {
				'icon-1': `${this.patientUrl}/page/index/icon-1.png`
			},
			list1: [
				`${this.patientUrl}/swiper/swiper-5.jpg`,
				`${this.patientUrl}/swiper/swiper-6.jpg`,
				`${this.patientUrl}/swiper/swiper-7.jpg`,
				`${this.patientUrl}/swiper/swiper-8.jpg`
			],
			otherBannerUrl: `${this.patientUrl}/banner/banner-1.jpg`,
			publicityBannerUrl: `${this.patientUrl}/banner/banner-2.jpg`,
			adBannerUrl: `${this.patientUrl}/banner/banner-7.jpg`,
			tab: {
				list: [],
				current: 0,
				swipperStyle: ''
			},
			doctor: [],
			dept: [],
			illness: [],
			rescue: [
				`${this.patientUrl}/rescue/rescue-1.jpg`,
				`${this.patientUrl}/rescue/rescue-2.jpg`,
				`${this.patientUrl}/rescue/rescue-3.jpg`,
				`${this.patientUrl}/rescue/rescue-4.jpg`,
				`${this.patientUrl}/rescue/rescue-5.jpg`,
				`${this.patientUrl}/rescue/rescue-6.jpg`
			]
		};
	},
	onShow() {
		this.searchDoctorInfo();
		this.searchMedicalDeptList();
		this.searchIllnessList();
	},
	methods: {
		goAgent: function() {
			uni.navigateTo({
				url: '/agent/chat/chat'
			});
		},
		goReactAgent: function() {
			uni.navigateTo({
				url: '/user/react_chat/react_chat'
			});
		},
		goCcAgent: function() {
			uni.navigateTo({
				url: '/user/cc_chat/cc_chat'
			});
		},
		navigatorHandle: function(name) {
			let url = null;
			if (name == 'doctor') {
				url = '/display/allDoctor/allDoctor';
			} else if (name == 'illness') {
				url = '/display/allInfo/allInfo';
			} else if (name == 'dept') {
				url = '/display/allDept/allDept';
			}

			uni.navigateTo({
				url: url
			});
		},
		searchDoctorInfo: function() {
			let that = this;
			let data = {
				page: that.page,
				length: that.length
			};
			that.ajax(
				that.api.searchDoctorInfo,
				'POST',
				data,
				function(resp) {
					let result = resp.data.result;
					for (let one of result.list) {
						one.photo = that.doctorPhotoUrl(one.photo);
					}
					that.doctor = result.list.slice(0, 3);
				},
				false
			);
		},
		searchMedicalDeptList: function() {
			let that = this;
			let data = {
				page: that.page,
				length: that.length
			};
			that.ajax(
				that.api.searchMedicalDeptList,
				'POST',
				data,
				function(resp) {
					let list = resp.data;
					that.dept = list.result.slice(0, 3);
				},
				false
			);
		},
		searchIllnessList: function() {
			let that = this;
			let data = {
				page: that.page,
				length: that.length
			};
			that.ajax(
				that.api.searchIllnessList,
				'POST',
				data,
				function(resp) {
					let result = resp.data.result;
					that.illness = result.list.slice(0, 3);
				},
				false
			);
		},
		clickTabHandle: function() {},
		click: function() {
			uni.navigateTo({
				url: '/registration/notice/notice'
			});
		},
		goDoctorDetail: function(doctorId) {
			uni.navigateTo({
				url: `/display/doctor_detail/doctor_detail?doctorId=${doctorId}`
			});
		}
	}
};
</script>

<style lang="less">
@import url(index.less);

.agent-entry {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin: 24rpx 24rpx 0;
	padding: 24rpx;
	border-radius: 20rpx;
	background: linear-gradient(135deg, #eef8ff, #ffffff);
	box-shadow: 0 8rpx 24rpx rgba(18, 150, 219, 0.08);
}

.react-entry {
	background: linear-gradient(135deg, #edf3f9, #ffffff);
	box-shadow: 0 8rpx 24rpx rgba(45, 106, 159, 0.08);
}

.agent-title {
	display: block;
	font-size: 32rpx;
	font-weight: 600;
	color: #222;
}

.agent-desc {
	display: block;
	margin-top: 8rpx;
	font-size: 24rpx;
	color: #666;
}
</style>
