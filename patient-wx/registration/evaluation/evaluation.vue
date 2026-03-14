<template>
	<view>
		<!-- 评价提交模式 -->
		<view v-if="mode == 'submit'" class="eval-container">
			<view class="section">
				<text class="label">评分</text>
				<view class="stars">
					<u-icon v-for="i in 5" :key="i" :name="i <= score ? 'star-fill' : 'star'"
						color="#ff9900" size="36" @click="score = i"></u-icon>
				</view>
			</view>
			<view class="section">
				<text class="label">评价内容</text>
				<textarea class="comment-input" v-model="comment" placeholder="请输入您的评价（选填）" maxlength="500" />
			</view>
			<u-button type="primary" @click="submitEvaluation" :disabled="score == 0">提交评价</u-button>
		</view>

		<!-- 我的评价列表模式 -->
		<view v-if="mode == 'list'">
			<view class="eval-item" v-for="one of list" :key="one.id">
				<view class="eval-header">
					<text class="doctor-name">{{one.doctorName}}</text>
					<text class="job">{{one.job}}</text>
				</view>
				<view class="eval-stars">
					<u-icon v-for="i in 5" :key="i" :name="i <= one.score ? 'star-fill' : 'star'"
						color="#ff9900" size="20"></u-icon>
				</view>
				<text class="eval-comment" v-if="one.comment">{{one.comment}}</text>
				<text class="eval-time">{{one.createTime}}</text>
			</view>
			<view v-if="list.length == 0">
				<u-empty mode="data" text="暂无评价记录" icon="http://cdn.uviewui.com/uview/empty/data.png"></u-empty>
			</view>
		</view>
	</view>
</template>

<script>
export default {
	data() {
		return {
			mode: 'submit',
			score: 0,
			comment: '',
			doctorId: null,
			registrationId: null,
			videoDiagnoseId: null,
			page: 1,
			length: 20,
			list: [],
			isLastPage: false
		};
	},
	methods: {
		submitEvaluation: function() {
			let that = this;
			if (that.score == 0) {
				uni.showToast({ icon: 'none', title: '请选择评分' });
				return;
			}
			let data = {
				doctorId: that.doctorId,
				score: that.score,
				comment: that.comment
			};
			if (that.registrationId) data.registrationId = that.registrationId;
			if (that.videoDiagnoseId) data.videoDiagnoseId = that.videoDiagnoseId;

			that.ajax(that.api.insertEvaluation, 'POST', data, function(resp) {
				uni.showToast({ icon: 'success', title: '评价成功' });
				setTimeout(function() {
					uni.navigateBack();
				}, 1500);
			});
		},
		loadDataList: function(ref) {
			let data = {
				page: ref.page,
				length: ref.length
			};
			ref.ajax(ref.api.searchEvaluationByPage, 'POST', data, function(resp) {
				let result = resp.data.result;
				if (result.list == null || result.list.length == 0) {
					ref.isLastPage = true;
					if (ref.page > 1) {
						ref.page = ref.page - 1;
						uni.showToast({ icon: 'none', title: '已经到底了' });
					}
				} else {
					for (let one of result.list) {
						ref.list.push(one);
					}
				}
			});
		}
	},
	onLoad: function(options) {
		if (options.mode == 'list') {
			this.mode = 'list';
			this.loadDataList(this);
		} else {
			this.mode = 'submit';
			if (options.doctorId) this.doctorId = parseInt(options.doctorId);
			if (options.registrationId) this.registrationId = parseInt(options.registrationId);
			if (options.videoDiagnoseId) this.videoDiagnoseId = parseInt(options.videoDiagnoseId);
		}
	},
	onReachBottom: function() {
		if (this.mode != 'list' || this.isLastPage) return;
		this.page = this.page + 1;
		this.loadDataList(this);
	}
};
</script>

<style lang="less">
.eval-container {
	padding: 30rpx;
	.section {
		margin-bottom: 40rpx;
		.label {
			display: block;
			font-size: 30rpx;
			color: #333;
			margin-bottom: 16rpx;
		}
		.stars {
			display: flex;
			gap: 16rpx;
		}
		.comment-input {
			width: 100%;
			height: 200rpx;
			border: 1rpx solid #ddd;
			border-radius: 8rpx;
			padding: 16rpx;
			font-size: 28rpx;
			box-sizing: border-box;
		}
	}
}
.eval-item {
	padding: 24rpx 30rpx;
	background-color: #fff;
	border-bottom: 1rpx solid #f0f0f0;
	.eval-header {
		display: flex;
		align-items: center;
		margin-bottom: 10rpx;
		.doctor-name {
			font-size: 30rpx;
			color: #333;
			margin-right: 12rpx;
		}
		.job {
			font-size: 24rpx;
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
		margin-bottom: 10rpx;
	}
	.eval-time {
		font-size: 22rpx;
		color: #999;
	}
}
</style>
