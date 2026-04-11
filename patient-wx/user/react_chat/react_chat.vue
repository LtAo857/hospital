<template>
	<view class="page">
		<view class="hero-card">
			<view class="hero-title">传统 Agent 挂号助手</view>
			<view class="hero-desc">第二套架构：基于传统 ReAct 风格执行“理解问题 -> 选工具 -> 观察结果 -> 再决策”的循环。</view>
		</view>

		<scroll-view class="message-list" scroll-y :scroll-into-view="scrollAnchor">
			<view
				v-for="(item, index) in messages"
				:key="index"
				:id="`msg-${index}`"
				:class="item.role === 'user' ? 'message-row user' : 'message-row assistant'"
			>
				<view class="bubble">
					<text class="bubble-text">{{ item.content }}</text>
				</view>
			</view>

			<view v-if="agent.steps && agent.steps.length > 0" class="section-card">
				<view class="section-title">当前步骤</view>
				<view class="step-list">
					<view class="step-item" v-for="step in agent.steps" :key="step.key">
						<text class="step-label">{{ step.label }}</text>
						<text :class="step.status === 'completed' ? 'step-status success' : step.status === 'in_progress' ? 'step-status active' : 'step-status'">{{ formatStepStatus(step.status) }}</text>
					</view>
				</view>
			</view>

			<view v-if="agent.cards && agent.cards.length > 0" class="section-card">
				<view class="section-title">可执行项</view>
				<view class="card-list">
					<view class="action-card" v-for="(card, index) in agent.cards" :key="index" @tap="handleCard(card)">
						<view class="card-head">
							<text class="card-title">{{ card.title }}</text>
							<text v-if="card.badge" class="card-badge">{{ card.badge }}</text>
						</view>
						<text class="card-desc">{{ card.description }}</text>
					</view>
				</view>
			</view>

			<view v-if="agent.toolLogs && agent.toolLogs.length > 0" class="section-card slim">
				<view class="section-title">工具日志</view>
				<view class="tool-log" v-for="(log, index) in agent.toolLogs" :key="index">
					<text class="tool-name">{{ log.name }}</text>
					<text class="tool-summary">{{ log.summary }}</text>
				</view>
			</view>

			<view :id="scrollAnchor"></view>
		</scroll-view>

		<view class="quick-actions">
			<view class="quick-btn" @tap="sendAction('welcome')">首页</view>
			<view class="quick-btn" @tap="sendAction('start_registration')">开始挂号</view>
			<view class="quick-btn" @tap="sendAction('view_user_card')">就诊卡</view>
			<view class="quick-btn" @tap="sendAction('view_messages')">消息</view>
		</view>

		<view class="composer">
			<input
				class="composer-input"
				v-model="inputText"
				confirm-type="send"
				placeholder="例如：帮我挂明天口腔科的号"
				@confirm="submitText"
			/>
			<button class="composer-btn" @tap="submitText">发送</button>
		</view>
	</view>
</template>

<script>
export default {
	data() {
		return {
			sessionId: '',
			inputText: '',
			messages: [],
			agent: {
				cards: [],
				toolLogs: [],
				steps: [],
				confirmation: null,
				state: 'idle'
			},
			streamTimer: null,
			scrollAnchor: 'anchor-bottom'
		};
	},
	onLoad() {
		this.sessionId = `${Date.now()}`;
		this.sendAction('welcome');
	},
	beforeDestroy() {
		this.clearStreamTimer();
	},
	methods: {
		formatStepStatus(status) {
			if (status === 'completed') return '已完成';
			if (status === 'in_progress') return '进行中';
			return '待处理';
		},
		appendUserMessage(text) {
			if (!text) return;
			this.messages.push({ role: 'user', content: text });
		},
		appendAssistantMessageStreaming(text) {
			if (!text) return;
			this.clearStreamTimer();
			let message = { role: 'assistant', content: '' };
			this.messages.push(message);
			let index = this.messages.length - 1;
			let chars = Array.from(text);
			let cursor = 0;
			this.scrollToBottom();
			this.streamTimer = setInterval(() => {
				if (cursor >= chars.length) {
					this.clearStreamTimer();
					return;
				}
				this.messages[index].content += chars[cursor];
				cursor += 1;
				this.scrollToBottom();
			}, 20);
		},
		clearStreamTimer() {
			if (this.streamTimer) {
				clearInterval(this.streamTimer);
				this.streamTimer = null;
			}
		},
		scrollToBottom() {
			this.$nextTick(() => {
				this.scrollAnchor = `msg-${this.messages.length - 1}`;
			});
		},
		submitText() {
			let text = (this.inputText || '').trim();
			if (!text) return;
			this.appendUserMessage(text);
			this.inputText = '';
			this.chat({
				message: text,
				currentPage: '/user/react_chat/react_chat'
			});
		},
		sendAction(action, payload) {
			this.chat({
				action,
				payload: payload || {},
				currentPage: '/user/react_chat/react_chat'
			});
		},
		handleCard(card) {
			if (!card || !card.action) {
				return;
			}
			if (card.action === 'navigate') {
				let url = card.payload && card.payload.url ? card.payload.url : '';
				if (!url) return;
				if (url.indexOf('/pages/') === 0) {
					uni.switchTab({ url });
				} else {
					uni.navigateTo({ url });
				}
				return;
			}
			if (card.action === 'create_registration') {
				uni.showModal({
					title: '确认挂号',
					content: '传统 Agent 已准备好提交挂号，是否继续？',
					success: resp => {
						if (!resp.confirm) return;
						this.chat({
							action: card.action,
							payload: {
								...(card.payload || {}),
								confirmed: true
							},
							currentPage: '/user/react_chat/react_chat'
						});
					}
				});
				return;
			}
			this.chat({
				action: card.action,
				payload: card.payload || {},
				currentPage: '/user/react_chat/react_chat'
			});
		},
		chat(data) {
			let request = {
				sessionId: this.sessionId,
				...data
			};
			this.ajax(
				this.api.agentReactChat,
				'POST',
				request,
				resp => {
					let result = resp.data.result || {};
					this.sessionId = result.sessionId || this.sessionId;
					this.agent = {
						cards: result.cards || [],
						toolLogs: result.toolLogs || [],
						steps: result.steps || [],
						confirmation: result.confirmation || null,
						state: result.state || 'idle'
					};
					this.appendAssistantMessageStreaming(result.reply || '传统 Agent 暂时没有返回内容。');
				},
				false
			);
		}
	}
};
</script>

<style lang="less">
.page {
	min-height: 100vh;
	background: linear-gradient(180deg, #f4f7fb 0%, #eef3f8 100%);
	padding: 24rpx;
	box-sizing: border-box;
}
.hero-card,
.section-card {
	background: #fff;
	border-radius: 20rpx;
	padding: 24rpx;
	margin-bottom: 20rpx;
	box-shadow: 0 10rpx 28rpx rgba(27, 82, 132, 0.08);
}
.hero-title {
	font-size: 34rpx;
	font-weight: 700;
	color: #1f2d3d;
}
.hero-desc {
	margin-top: 12rpx;
	font-size: 24rpx;
	color: #5d6b7a;
	line-height: 1.6;
}
.message-list {
	height: 760rpx;
}
.message-row {
	display: flex;
	margin-bottom: 18rpx;
}
.message-row.user {
	justify-content: flex-end;
}
.message-row.assistant {
	justify-content: flex-start;
}
.bubble {
	max-width: 82%;
	padding: 20rpx 24rpx;
	border-radius: 18rpx;
	background: #ffffff;
}
.message-row.user .bubble {
	background: #2d6a9f;
}
.bubble-text {
	font-size: 26rpx;
	line-height: 1.6;
	color: #2c3a4b;
}
.message-row.user .bubble-text {
	color: #ffffff;
}
.section-title {
	font-size: 28rpx;
	font-weight: 600;
	color: #213247;
	margin-bottom: 18rpx;
}
.step-list,
.card-list {
	display: flex;
	flex-direction: column;
	gap: 14rpx;
}
.step-item,
.tool-log {
	display: flex;
	justify-content: space-between;
	align-items: flex-start;
	gap: 16rpx;
}
.step-label,
.tool-name {
	font-size: 24rpx;
	color: #34495e;
}
.step-status,
.tool-summary {
	font-size: 22rpx;
	color: #7d8b99;
}
.step-status.active {
	color: #2d6a9f;
}
.step-status.success {
	color: #1f8f5f;
}
.action-card {
	padding: 20rpx;
	border-radius: 16rpx;
	background: #f6f9fc;
	border: 1rpx solid #dde8f2;
}
.card-head {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 10rpx;
}
.card-title {
	font-size: 28rpx;
	font-weight: 600;
	color: #223246;
}
.card-badge {
	font-size: 22rpx;
	color: #2d6a9f;
	background: rgba(45, 106, 159, 0.12);
	padding: 6rpx 12rpx;
	border-radius: 999rpx;
}
.card-desc {
	font-size: 24rpx;
	color: #627487;
	line-height: 1.5;
}
.quick-actions {
	display: grid;
	grid-template-columns: repeat(4, 1fr);
	gap: 12rpx;
	margin-bottom: 18rpx;
}
.quick-btn {
	background: #ffffff;
	border-radius: 14rpx;
	padding: 18rpx 0;
	text-align: center;
	font-size: 24rpx;
	color: #2d6a9f;
	border: 1rpx solid #d8e5f1;
}
.composer {
	display: flex;
	align-items: center;
	gap: 16rpx;
	background: #ffffff;
	padding: 16rpx;
	border-radius: 18rpx;
	box-shadow: 0 8rpx 24rpx rgba(20, 56, 92, 0.06);
}
.composer-input {
	flex: 1;
	height: 76rpx;
	background: #f5f8fb;
	border-radius: 14rpx;
	padding: 0 20rpx;
	font-size: 26rpx;
}
.composer-btn {
	height: 76rpx;
	line-height: 76rpx;
	padding: 0 28rpx;
	border-radius: 14rpx;
	background: #2d6a9f;
	color: #fff;
	font-size: 26rpx;
}
.slim {
	padding-bottom: 18rpx;
}
</style>
