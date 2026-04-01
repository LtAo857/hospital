<template>
	<view class="page">
		<view v-if="loaded && hasCard" class="card">
			<view class="header">
				<text class="title">就诊卡信息</text>
				<text class="subtitle">仅供查看</text>
			</view>
			<view class="section">
				<view class="row">
					<text class="label">姓名</text>
					<text class="value">{{ card.name }}</text>
				</view>
				<view class="row">
					<text class="label">性别</text>
					<text class="value">{{ card.sex }}</text>
				</view>
				<view class="row">
					<text class="label">身份证号</text>
					<text class="value">{{ card.pid }}</text>
				</view>
				<view class="row">
					<text class="label">手机号</text>
					<text class="value">{{ card.tel }}</text>
				</view>
				<view class="row">
					<text class="label">出生日期</text>
					<text class="value">{{ card.birthday }}</text>
				</view>
				<view class="row column">
					<text class="label">疾病史</text>
					<view class="tags" v-if="card.medicalHistory.length > 0">
						<text class="tag" v-for="item in card.medicalHistory" :key="item">{{ item }}</text>
					</view>
					<text v-else class="value">无</text>
				</view>
				<view class="row">
					<text class="label">医保类型</text>
					<text class="value">{{ card.insuranceType }}</text>
				</view>
			</view>
		</view>
		<view v-else-if="loaded" class="empty-wrap">
			<u-empty mode="data" text="暂无就诊卡信息" icon="http://cdn.uviewui.com/uview/empty/data.png"></u-empty>
		</view>
	</view>
</template>

<script>
export default {
	data() {
		return {
			loaded: false,
			hasCard: false,
			card: {
				name: '',
				sex: '',
				pid: '',
				tel: '',
				birthday: '',
				medicalHistory: [],
				insuranceType: ''
			}
		};
	},
	methods: {
		loadUserInfoCard: function() {
			let that = this;
			that.ajax(
				that.api.searchUserInfoCard,
				'GET',
				{},
				function(resp) {
					let data = resp.data;
					that.loaded = true;
					if (data.msg == '没有查询到数据') {
						that.hasCard = false;
						return;
					}
					that.hasCard = true;
					that.card.name = data.name || '';
					that.card.sex = data.sex || '';
					that.card.pid = data.pid || '';
					that.card.tel = data.tel || '';
					that.card.birthday = data.birthday || '';
					that.card.medicalHistory = Array.isArray(data.medicalHistory) ? data.medicalHistory : [];
					that.card.insuranceType = data.insuranceType || '';
				},
				false
			);
		}
	},
	onShow: function() {
		this.loadUserInfoCard();
	}
};
</script>

<style lang="less">
.page {
	min-height: 100vh;
	background: #f7f8fa;
	padding: 30rpx;
	box-sizing: border-box;
}

.card {
	background: #ffffff;
	border-radius: 24rpx;
	padding: 30rpx;
	box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.05);
}

.header {
	margin-bottom: 24rpx;
}

.title {
	display: block;
	font-size: 34rpx;
	font-weight: 600;
	color: #222;
}

.subtitle {
	display: block;
	font-size: 24rpx;
	color: #999;
	margin-top: 8rpx;
}

.section {
	background: #fff;
}

.row {
	display: flex;
	justify-content: space-between;
	align-items: flex-start;
	padding: 24rpx 0;
	border-bottom: 1rpx solid #f0f0f0;
	gap: 24rpx;
}

.row:last-child {
	border-bottom: none;
}

.column {
	flex-direction: column;
}

.label {
	font-size: 28rpx;
	color: #666;
	flex-shrink: 0;
}

.value {
	font-size: 28rpx;
	color: #222;
	text-align: right;
	word-break: break-all;
}

.tags {
	display: flex;
	flex-wrap: wrap;
	gap: 16rpx;
	margin-top: 8rpx;
}

.tag {
	padding: 8rpx 20rpx;
	background: #eef4ff;
	color: #2b6cff;
	font-size: 24rpx;
	border-radius: 999rpx;
}

.empty-wrap {
	padding-top: 160rpx;
}
</style>
