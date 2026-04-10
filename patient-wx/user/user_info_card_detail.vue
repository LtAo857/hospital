<template>
	<view class="page">
		<view v-if="loaded && hasCard" class="card">
			<view class="header">
				<view class="header-main">
					<text class="title">{{ copy.title }}</text>
					<text class="subtitle">{{ copy.subtitle }}</text>
				</view>
				<view class="edit-btn" @tap="goEditCard">{{ copy.edit }}</view>
			</view>
			<view class="section">
				<view class="row">
					<text class="label">{{ copy.name }}</text>
					<text class="value">{{ card.name }}</text>
				</view>
				<view class="row">
					<text class="label">{{ copy.sex }}</text>
					<text class="value">{{ card.sex }}</text>
				</view>
				<view class="row">
					<text class="label">{{ copy.pid }}</text>
					<text class="value">{{ card.pid }}</text>
				</view>
				<view class="row">
					<text class="label">{{ copy.tel }}</text>
					<text class="value">{{ card.tel }}</text>
				</view>
				<view class="row">
					<text class="label">{{ copy.birthday }}</text>
					<text class="value">{{ card.birthday }}</text>
				</view>
				<view class="row column">
					<text class="label">{{ copy.medicalHistory }}</text>
					<view v-if="card.medicalHistory.length > 0" class="tags">
						<text class="tag" v-for="item in card.medicalHistory" :key="item">{{ item }}</text>
					</view>
					<text v-else class="value">{{ copy.none }}</text>
				</view>
				<view class="row">
					<text class="label">{{ copy.insuranceType }}</text>
					<text class="value">{{ card.insuranceType }}</text>
				</view>
			</view>
		</view>
		<view v-else-if="loaded" class="empty-wrap">
			<u-empty mode="data" :text="copy.empty" icon="http://cdn.uviewui.com/uview/empty/data.png"></u-empty>
			<view class="empty-action" @tap="goEditCard">{{ copy.create }}</view>
		</view>
	</view>
</template>

<script>
const copy = {
	title: '\u5c31\u8bca\u5361\u4fe1\u606f',
	subtitle: '\u53ef\u67e5\u770b\uff0c\u4e5f\u53ef\u7ee7\u7eed\u4fee\u6539',
	edit: '\u4fee\u6539\u8d44\u6599',
	create: '\u53bb\u5b8c\u5584\u5c31\u8bca\u5361',
	name: '\u59d3\u540d',
	sex: '\u6027\u522b',
	pid: '\u8eab\u4efd\u8bc1\u53f7',
	tel: '\u624b\u673a\u53f7',
	birthday: '\u51fa\u751f\u65e5\u671f',
	medicalHistory: '\u75c5\u53f2',
	insuranceType: '\u533b\u4fdd\u7c7b\u578b',
	none: '\u65e0',
	empty: '\u6682\u65e0\u5c31\u8bca\u5361\u4fe1\u606f'
};

export default {
	data() {
		return {
			copy,
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
		goEditCard() {
			uni.navigateTo({
				url: '/user/fill_user_info/fill_user_info'
			});
		},
		loadUserInfoCard() {
			this.ajax(
				this.api.searchUserInfoCard,
				'GET',
				{},
				resp => {
					let data = resp.data || {};
					this.loaded = true;
					if (data.msg && `${data.msg}`.indexOf('\u6ca1\u6709\u67e5\u8be2\u5230\u6570\u636e') > -1) {
						this.hasCard = false;
						return;
					}
					this.hasCard = true;
					this.card.name = data.name || '';
					this.card.sex = data.sex || '';
					this.card.pid = data.pid || '';
					this.card.tel = data.tel || '';
					this.card.birthday = data.birthday || '';
					this.card.medicalHistory = Array.isArray(data.medicalHistory) ? data.medicalHistory : [];
					this.card.insuranceType = data.insuranceType || '';
				},
				false
			);
		}
	},
	onShow() {
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
	display: flex;
	justify-content: space-between;
	align-items: center;
	gap: 20rpx;
	margin-bottom: 24rpx;
}

.header-main {
	flex: 1;
	min-width: 0;
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

.edit-btn,
.empty-action {
	display: inline-flex;
	align-items: center;
	justify-content: center;
	padding: 16rpx 24rpx;
	border-radius: 999rpx;
	background: linear-gradient(135deg, #1e88ff 0%, #0d5bd7 100%);
	color: #fff;
	font-size: 24rpx;
	line-height: 1;
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
	display: flex;
	flex-direction: column;
	align-items: center;
}

.empty-action {
	margin-top: 24rpx;
}
</style>
