<template>
	<view>
		<view class="summary" v-if="avgScore > 0">
			<view class="avg-score">
				<text class="score-num">{{avgScore}}</text>
				<text class="score-label">平均评分</text>
			</view>
			<view class="total">
				<text>共 {{totalCount}} 条评价</text>
			</view>
		</view>
		<view class="eval-item" v-for="one of list" :key="one.id">
			<view class="eval-header">
				<text class="patient-name">{{one.patientName}}</text>
				<text class="eval-time">{{one.createTime}}</text>
			</view>
			<view class="eval-stars">
				<u-icon v-for="i in 5" :key="i" :name="i <= one.score ? 'star-fill' : 'star'"
					color="#ff9900" size="18"></u-icon>
			</view>
			<text class="eval-comment" v-if="one.comment">{{one.comment}}</text>
		</view>
		<view v-if="list.length == 0">
			<u-empty mode="data" text="暂无评价" icon="http://cdn.uviewui.com/uview/empty/data.png"></u-empty>
		</view>
	</view>
</template>

<script>
export default {
	data() {
		return {
			doctorId: null,
			avgScore: 0,
			totalCount: 0,
			page: 1,
			length: 20,
			list: [],
			isLastPage: false
		};
	},
	methods: {
		loadDataList: function(ref) {
			let data = {
				doctorId: ref.doctorId,
				page: ref.page,
				length: ref.length
			};
			ref.ajax(ref.api.searchDoctorEvaluation, 'POST', data, function(resp) {
				let result = resp.data.result;
				ref.avgScore = result.avgScore;
				ref.totalCount = result.totalCount;
				let page = result.page;
				if (page.list == null || page.list.length == 0) {
					ref.isLastPage = true;
					if (ref.page > 1) {
						ref.page = ref.page - 1;
						uni.showToast({ icon: 'none', title: '已经到底了' });
					}
				} else {
					for (let one of page.list) {
						ref.list.push(one);
					}
				}
			});
		}
	},
	onLoad: function(options) {
		if (options.doctorId) {
			this.doctorId = parseInt(options.doctorId);
			this.loadDataList(this);
		}
	},
	onReachBottom: function() {
		if (this.isLastPage) return;
		this.page = this.page + 1;
		this.loadDataList(this);
	}
};
</script>

<style lang="less">
.summary {
	display: flex;
	align-items: center;
	padding: 30rpx;
	background-color: #fff;
	border-bottom: 1rpx solid #f0f0f0;
	.avg-score {
		display: flex;
		flex-direction: column;
		align-items: center;
		margin-right: 40rpx;
		.score-num {
			font-size: 56rpx;
			color: #ff9900;
			font-weight: bold;
		}
		.score-label {
			font-size: 22rpx;
			color: #999;
		}
	}
	.total {
		font-size: 26rpx;
		color: #666;
	}
}
.eval-item {
	padding: 24rpx 30rpx;
	background-color: #fff;
	border-bottom: 1rpx solid #f0f0f0;
	.eval-header {
		display: flex;
		justify-content: space-between;
		align-items: center;
		margin-bottom: 10rpx;
		.patient-name {
			font-size: 28rpx;
			color: #333;
		}
		.eval-time {
			font-size: 22rpx;
			color: #999;
		}
	}
	.eval-stars {
		display: flex;
		gap: 4rpx;
		margin-bottom: 10rpx;
	}
	.eval-comment {
		display: block;
		font-size: 26rpx;
		color: #666;
	}
}
</style>
