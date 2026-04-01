<template>
	<view class="page">
		<view class="doctor-card">
			<view class="doctor-main">
				<u-avatar :src="photo" size="56"></u-avatar>
				<view class="info">
					<view class="row">
						<text class="name">{{ name }}</text>
						<text class="job">{{ job }}</text>
					</view>
					<text class="remark">{{ remark || '暂无擅长方向介绍' }}</text>
					<view class="meta-row">
						<text class="meta-item">评分 {{ formatScore(avgScore) }}</text>
						<text class="meta-item">评价 {{ totalCount }}</text>
						<text class="meta-item">挂号费 {{ price || '--' }}</text>
					</view>
					<text class="meta-text" v-if="tel">电话：{{ tel }}</text>
				</view>
			</view>
			<view class="action-row">
				<uni-fav
					:checked="isFavorite"
					bg-color="#eef2f7"
					bg-color-checked="#34ba97"
					fg-color="#5f6b7a"
					fg-color-checked="#ffffff"
					:content-text="{ contentDefault: '收藏', contentFav: '已收藏' }"
					@click="toggleFavorite"
				/>
				<view class="link-btn" @tap="goEvaluation">查看评价</view>
			</view>
		</view>

		<view class="schedule-container">
			<text class="title">选择号源时段</text>
			<u-grid :border="false" col="4">
				<u-grid-item v-for="one in schedule" :key="one.slot">
					<text :class="one.style" @click="clickScheduleHandler(one)">{{ one.range }}</text>
				</u-grid-item>
			</u-grid>
			<u-parse :content="notice"></u-parse>
			<u-button type="primary" size="large" @click="submitHandler">确认挂号 ￥{{ price || '--' }}</u-button>
		</view>
	</view>
</template>

<script>
import uniFav from '@/uni_modules/uni-ui/components/uni-fav/uni-fav.vue';

const dayjs = require('dayjs');

export default {
	components: {
		uniFav
	},
	data() {
		return {
			workPlanId: null,
			scheduleId: null,
			date: '',
			deptSubId: null,
			doctorId: null,
			name: '',
			photo: '',
			job: '',
			remark: '',
			description: '',
			tel: '',
			price: '',
			avgScore: 0,
			totalCount: 0,
			isFavorite: false,
			slot: null,
			timeMap: {
				'1': '08:00',
				'2': '08:30',
				'3': '09:00',
				'4': '09:30',
				'5': '10:00',
				'6': '10:30',
				'7': '11:00',
				'8': '11:30',
				'9': '13:00',
				'10': '13:30',
				'11': '14:00',
				'12': '14:30',
				'13': '15:00',
				'14': '15:30',
				'15': '16:00'
			},
			schedule: [],
			notice: `
				<ol class='notice-list'>
					<li class='notice-item'>可预约未来七天内的门诊号源。</li>
					<li class='notice-item'>若排班有变动，将通过消息通知您。</li>
					<li class='notice-item'>如无法按时就诊，请尽早取消预约。</li>
				</ol>
			`
		};
	},
	methods: {
		searchDoctorInfoById: function() {
			let that = this;
			that.ajax(
				that.api.searchDoctorInfoById,
				'POST',
				{ id: that.doctorId },
				function(resp) {
					let result = resp.data;
					that.name = result.name;
					that.photo = that.doctorPhotoUrl(result.photo);
					that.job = result.job;
					that.remark = result.remark;
					that.description = result.description;
					that.tel = result.tel;
					that.price = result.price;
					that.avgScore = result.avgScore || 0;
					that.totalCount = result.totalCount || 0;
					that.isFavorite = !!result.isFavorite;
				},
				false
			);
		},
		searchDoctorWorkPlanSchedule: function() {
			let that = this;
			that.ajax(
				that.api.searchDoctorWorkPlanSchedule,
				'POST',
				{
					date: that.date,
					doctorId: that.doctorId
				},
				function(resp) {
					let result = resp.data.result || [];
					let now = dayjs();
					let today = now.format('YYYY-MM-DD');
					that.schedule = Object.keys(that.timeMap).map(function(slot) {
						let rangeStart = dayjs(`${that.date} ${that.timeMap[slot]}`);
						let item = result.find(function(element) {
							return `${element.slot}` === slot;
						});
						let isDisabled = !item || item.num === item.maximum || (that.date === today && now.isAfter(rangeStart));
						return {
							workPlanId: item ? item.workPlanId : null,
							scheduleId: item ? item.scheduleId : null,
							slot: slot,
							range: that.timeMap[slot],
							style: isDisabled ? 'item disable' : that.slot === slot ? 'item active' : 'item'
						};
					});
					let current = that.schedule.find(function(item) {
						return item.slot === that.slot && item.style === 'item active';
					});
					if (!current) {
						that.slot = null;
						that.workPlanId = null;
						that.scheduleId = null;
					}
				},
				false
			);
		},
		formatScore: function(score) {
			let value = Number(score || 0);
			if (!value) {
				return '--';
			}
			return value.toFixed(1);
		},
		ensureLogin: function() {
			let token = uni.getStorageSync('token');
			if (token) {
				return true;
			}
			uni.showToast({
				icon: 'none',
				title: '请先登录'
			});
			setTimeout(function() {
				uni.switchTab({
					url: '/pages/mine/mine'
				});
			}, 1200);
			return false;
		},
		toggleFavorite: function() {
			let that = this;
			if (!that.ensureLogin()) {
				return;
			}
			let url = that.isFavorite ? that.api.unfavoriteDoctor : that.api.favoriteDoctor;
			that.ajax(
				url,
				'POST',
				{ doctorId: that.doctorId },
				function(resp) {
					if (resp.data.result) {
						that.isFavorite = !that.isFavorite;
						uni.showToast({
							icon: 'none',
							title: that.isFavorite ? '收藏成功' : '已取消收藏'
						});
					} else {
						uni.showToast({
							icon: 'none',
							title: '请先创建就诊卡'
						});
					}
				},
				false
			);
		},
		goEvaluation: function() {
			uni.navigateTo({
				url: `/display/doctor_evaluation/doctor_evaluation?doctorId=${this.doctorId}`
			});
		},
		clickScheduleHandler: function(item) {
			if (item.style.indexOf('disable') > -1) {
				return;
			}
			this.workPlanId = item.workPlanId;
			this.scheduleId = item.scheduleId;
			this.slot = item.slot;
			this.schedule = this.schedule.map(function(one) {
				if (one.style.indexOf('disable') > -1) {
					return one;
				}
				return {
					...one,
					style: one.slot === item.slot ? 'item active' : 'item'
				};
			});
		},
		submitHandler: function() {
			let that = this;
			if (!that.slot || !that.workPlanId || !that.scheduleId) {
				uni.showToast({
					icon: 'none',
					title: '请选择号源时段'
				});
				return;
			}
			uni.showModal({
				title: '确认挂号',
				content: '是否确认提交本次挂号？',
				success: function(resp) {
					if (!resp.confirm) {
						return;
					}
					that.ajax(
						that.api.registerMedicalAppointment,
						'POST',
						{
							workPlanId: that.workPlanId,
							scheduleId: that.scheduleId,
							date: that.date,
							doctorId: that.doctorId,
							deptSubId: that.deptSubId,
							amount: that.price,
							slot: that.slot
						},
						function(registerResp) {
							let result = registerResp.data;
							if (!result.hasOwnProperty('outTradeNo')) {
								uni.showToast({
									icon: 'none',
									title: '该时段号源已满'
								});
								that.searchDoctorWorkPlanSchedule();
								return;
							}
							uni.showToast({
								icon: 'none',
								title: '挂号成功'
							});
							setTimeout(function() {
								uni.switchTab({
									url: '/pages/registration_list/registration_list'
								});
							}, 1500);
						}
					);
				}
			});
		}
	},
	onLoad: function(options) {
		this.date = options.date || '';
		this.doctorId = Number(options.doctorId || 0);
		this.deptSubId = options.deptSubId || null;
		if (this.doctorId > 0) {
			this.searchDoctorInfoById();
			this.searchDoctorWorkPlanSchedule();
		}
	}
};
</script>

<style lang="less">
@import url(doctor_schedule.less);
</style>
