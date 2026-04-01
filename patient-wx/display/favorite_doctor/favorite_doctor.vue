<template>
	<view class="page">
		<view v-if="list.length > 0">
			<view class="favorite-card" v-for="one in list" :key="one.id" @tap="goDoctorDetail(one.id)">
				<u-avatar :src="one.photo" size="52"></u-avatar>
				<view class="favorite-info">
					<view class="favorite-head">
						<text class="favorite-name">{{ one.name }}</text>
						<text class="favorite-job">{{ one.job }}</text>
					</view>
					<text class="favorite-remark">{{ one.remark || text.noRemark }}</text>
					<view class="favorite-meta">
						<text class="favorite-price">{{ text.pricePrefix }}{{ one.price || '--' }}</text>
						<text class="favorite-time">{{ one.favoriteTime }}</text>
					</view>
				</view>
			</view>
		</view>
		<view v-else class="empty-wrap">
			<u-empty mode="data" :text="text.empty" icon="http://cdn.uviewui.com/uview/empty/data.png"></u-empty>
		</view>
	</view>
</template>

<script>
export default {
	data() {
		return {
			page: 1,
			length: 10,
			list: [],
			isLastPage: false,
			text: {
				empty: '\u6682\u65e0\u6536\u85cf\u533b\u751f',
				noRemark: '\u6682\u65e0\u64c5\u957f\u65b9\u5411\u4ecb\u7ecd',
				pricePrefix: '\u6302\u53f7\u8d39 \u00a5'
			}
		};
	},
	methods: {
		loadDataList: function(ref) {
			ref.ajax(
				ref.api.searchFavoriteDoctorByPage,
				'POST',
				{
					page: ref.page,
					length: ref.length
				},
				function(resp) {
					let result = resp.data.result;
					let rows = result && result.list ? result.list : [];
					if (rows.length == 0) {
						ref.isLastPage = true;
						if (ref.page > 1) {
							ref.page = ref.page - 1;
							uni.showToast({ icon: 'none', title: '\u5df2\u7ecf\u5230\u5e95\u4e86' });
						}
						return;
					}
					for (let one of rows) {
						one.photo = ref.doctorPhotoUrl(one.photo);
						ref.list.push(one);
					}
				}
			);
		},
		goDoctorDetail: function(doctorId) {
			uni.navigateTo({
				url: `/display/doctor_detail/doctor_detail?doctorId=${doctorId}`
			});
		}
	},
	onLoad: function() {
		this.loadDataList(this);
	},
	onReachBottom: function() {
		if (this.isLastPage) {
			return;
		}
		this.page = this.page + 1;
		this.loadDataList(this);
	}
};
</script>

<style lang="less">
.page {
	min-height: 100vh;
	background: #f7f8fa;
	padding: 24rpx;
	box-sizing: border-box;
}

.favorite-card {
	display: flex;
	align-items: flex-start;
	padding: 24rpx;
	background: #fff;
	border-radius: 20rpx;
	margin-bottom: 24rpx;
	box-shadow: 0 8rpx 24rpx rgba(15, 23, 42, 0.06);
}

.favorite-info {
	flex: 1;
	margin-left: 20rpx;
}

.favorite-head {
	display: flex;
	align-items: center;
	flex-wrap: wrap;
	margin-bottom: 12rpx;
}

.favorite-name {
	font-size: 32rpx;
	font-weight: 600;
	color: #1f2937;
	margin-right: 14rpx;
}

.favorite-job {
	font-size: 24rpx;
	color: #3b82f6;
	background: rgba(59, 130, 246, 0.12);
	padding: 4rpx 12rpx;
	border-radius: 999rpx;
}

.favorite-remark {
	display: block;
	font-size: 26rpx;
	color: #6b7280;
	line-height: 1.7;
	margin-bottom: 16rpx;
}

.favorite-meta {
	display: flex;
	justify-content: space-between;
	align-items: center;
	font-size: 24rpx;
	color: #94a3b8;
	gap: 16rpx;
}

.favorite-price {
	color: #ef4444;
	font-weight: 600;
}

.empty-wrap {
	padding-top: 120rpx;
}
</style>
