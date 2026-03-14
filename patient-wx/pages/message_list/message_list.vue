<template>
	<view>
		<view class="header" v-if="unreadCount > 0">
			<text class="unread-tip">您有 {{unreadCount}} 条未读消息</text>
			<text class="read-all" @tap="readAllHandle">全部已读</text>
		</view>
		<view class="message-item" v-for="one of list" :key="one.id" @tap="readHandle(one)">
			<view class="icon-wrap">
				<view class="msg-icon" :class="'type-' + one.type"></view>
			</view>
			<view class="content-wrap">
				<view class="title-row">
					<text class="title" :class="{'unread': !one.isRead}">{{one.title}}</text>
					<text class="time">{{one.createTime}}</text>
				</view>
				<text class="content">{{one.content}}</text>
			</view>
			<view class="dot" v-if="!one.isRead"></view>
		</view>
		<view v-if="list.length == 0">
			<u-empty mode="message" text="暂无消息" icon="http://cdn.uviewui.com/uview/empty/message.png"></u-empty>
		</view>
	</view>
</template>

<script>
export default {
	data() {
		return {
			page: 1,
			length: 20,
			list: [],
			isLastPage: false,
			unreadCount: 0
		};
	},
	methods: {
		loadDataList: function(ref) {
			let data = {
				page: ref.page,
				length: ref.length
			};
			ref.ajax(ref.api.searchMessageByPage, 'POST', data, function(resp) {
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
		},
		loadUnreadCount: function() {
			let that = this;
			that.ajax(that.api.searchUnreadCount, 'GET', {}, function(resp) {
				that.unreadCount = resp.data.result;
			}, false);
		},
		readHandle: function(item) {
			let that = this;
			if (!item.isRead) {
				that.ajax(that.api.readMessage, 'POST', { id: item.id }, function(resp) {
					item.isRead = true;
					that.unreadCount = Math.max(0, that.unreadCount - 1);
				}, false);
			}
		},
		readAllHandle: function() {
			let that = this;
			that.ajax(that.api.readAllMessage, 'GET', {}, function(resp) {
				for (let one of that.list) {
					one.isRead = true;
				}
				that.unreadCount = 0;
				uni.showToast({ icon: 'success', title: '已全部标记已读' });
			});
		}
	},
	onShow: function() {
		let that = this;
		that.page = 1;
		that.list = [];
		that.isLastPage = false;
		that.loadDataList(that);
		that.loadUnreadCount();
	},
	onReachBottom: function() {
		let that = this;
		if (that.isLastPage) return;
		that.page = that.page + 1;
		that.loadDataList(that);
	}
};
</script>

<style lang="less">
.header {
	display: flex;
	justify-content: space-between;
	align-items: center;
	padding: 20rpx 30rpx;
	background-color: #fff3e0;
	.unread-tip {
		font-size: 26rpx;
		color: #e65100;
	}
	.read-all {
		font-size: 26rpx;
		color: #1296db;
	}
}
.message-item {
	display: flex;
	align-items: center;
	padding: 24rpx 30rpx;
	background-color: #fff;
	border-bottom: 1rpx solid #f0f0f0;
	position: relative;
	.icon-wrap {
		margin-right: 20rpx;
		.msg-icon {
			width: 70rpx;
			height: 70rpx;
			border-radius: 50%;
			background-color: #1296db;
			&.type-1 { background-color: #4caf50; }
			&.type-2 { background-color: #ff9800; }
			&.type-3 { background-color: #2196f3; }
			&.type-4 { background-color: #9c27b0; }
		}
	}
	.content-wrap {
		flex: 1;
		overflow: hidden;
		.title-row {
			display: flex;
			justify-content: space-between;
			align-items: center;
			margin-bottom: 8rpx;
			.title {
				font-size: 28rpx;
				color: #333;
				&.unread { font-weight: bold; }
			}
			.time {
				font-size: 22rpx;
				color: #999;
			}
		}
		.content {
			font-size: 24rpx;
			color: #666;
			overflow: hidden;
			text-overflow: ellipsis;
			white-space: nowrap;
		}
	}
	.dot {
		position: absolute;
		top: 28rpx;
		left: 16rpx;
		width: 16rpx;
		height: 16rpx;
		border-radius: 50%;
		background-color: #f44336;
	}
}
</style>
