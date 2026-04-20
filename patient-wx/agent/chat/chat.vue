<template>
	<view class="page">
		<view class="hero-card">
			<view class="hero-title">AI挂号助手</view>
			<view class="hero-desc">一期骨架版：支持查科室、查医生、查号源，并在确认后挂号。</view>
		</view>

		<scroll-view class="message-list" scroll-y :scroll-into-view="scrollAnchor">
			<view v-for="(item,index) in messages" :key="index" :id="`msg-${index}`" :class="item.role === 'user' ? 'message-row user' : 'message-row assistant'">
				<view class="bubble">
					<text class="bubble-text">{{ item.content }}</text>
				</view>
			</view>

			<view v-if="agent.errorMessage" class="section-card error-card">
				<view class="section-title">当前状态</view>
				<view class="error-head">
					<text class="error-text">{{ agent.errorMessage }}</text>
					<text class="error-badge" v-if="agent.retryable">可重试</text>
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

			<view v-if="agent.agentFlows && agent.agentFlows.length > 0" class="section-card">
				<view class="section-title">Agent 执行流程</view>
				<view class="step-list">
					<view class="flow-item" v-for="flow in agent.agentFlows" :key="flow.key">
						<view class="flow-head">
							<text class="step-label">{{ flow.title }}</text>
							<text :class="flow.status === 'completed' ? 'step-status success' : flow.status === 'failed' ? 'step-status failed' : flow.status === 'waiting' ? 'step-status waiting' : 'step-status active'">{{ formatFlowStatus(flow.status) }}</text>
						</view>
						<text class="flow-summary">{{ flow.summary }}</text>
						<text class="flow-meta" v-if="flow.toolCount">调用工具 {{ flow.toolCount }} 次</text>
					</view>
				</view>
			</view>

			<view v-if="agent.cards && agent.cards.length > 0" class="section-card">
				<view class="section-title">可执行项</view>
				<view class="card-list">
					<view class="action-card" v-for="(card,index) in agent.cards" :key="index" @tap="handleCard(card)">
						<view class="card-head">
							<text class="card-title">{{ card.title }}</text>
							<text class="card-badge" v-if="card.badge">{{ card.badge }}</text>
						</view>
						<text class="card-desc">{{ card.description }}</text>
					</view>
				</view>
			</view>

			<view v-if="agent.toolLogs && agent.toolLogs.length > 0" class="section-card slim">
				<view class="section-title">工具执行</view>
				<view class="tool-log" v-for="(log,index) in agent.toolLogs" :key="index">
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
			<view class="quick-btn" @tap="sendAction('view_registrations')">我的挂号</view>
			<view class="quick-btn" @tap="sendAction('explain_recommendation')">推荐解释</view>
		</view>

		<view class="composer">
			<input class="composer-input" v-model="inputText" confirm-type="send" placeholder="例如：帮我挂明天内科的号" @confirm="submitText" />
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
				agentFlows: [],
				confirmation: null,
				state: 'idle',
				errorCode: '',
				retryable: null,
				errorMessage: ''
			},
			streamTimer: null,
			submitting: false,
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
		formatFlowStatus(status) {
			if (status === 'completed') return '已完成';
			if (status === 'failed') return '执行失败';
			if (status === 'waiting') return '等待补充';
			return '进行中';
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
			}, 25);
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
			if (this.submitting) {
				return;
			}
			let text = (this.inputText || '').trim();
			if (!text) {
				return;
			}
			this.appendUserMessage(text);
			this.inputText = '';
			this.chat({
				message: text,
				currentPage: '/agent/chat/chat'
			});
		},
		sendAction(action, payload) {
			if (this.submitting) {
				return;
			}
			this.chat({
				action: action,
				payload: payload || {},
				currentPage: '/agent/chat/chat'
			});
		},
		handleCard(card) {
			if (!card || !card.action || this.submitting) {
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
					content: '是否确认提交本次挂号？',
					success: (resp) => {
						if (!resp.confirm) return;
						this.submitting = true;
						this.chat({
							action: card.action,
							payload: {
								...(card.payload || {}),
								confirmed: true
							},
							currentPage: '/agent/chat/chat'
						});
					}
				});
				return;
			}
			this.chat({
				action: card.action,
				payload: card.payload || {},
				currentPage: '/agent/chat/chat'
			});
		},
		chat(data) {
			let request = {
				sessionId: this.sessionId,
				...data
			};
			this.ajax(
				this.api.agentChat,
				'POST',
				request,
				(resp) => {
					let result = resp.data.result || {};
					this.sessionId = result.sessionId || this.sessionId;
					this.agent = {
						cards: result.cards || [],
						toolLogs: result.toolLogs || [],
						steps: result.steps || [],
						agentFlows: result.agentFlows || [],
						confirmation: result.confirmation || null,
						state: result.state || 'idle',
						errorCode: result.errorCode || '',
						retryable: typeof result.retryable === 'boolean' ? result.retryable : null,
						errorMessage: result.errorMessage || ''
					};
					this.submitting = false;
					this.appendAssistantMessageStreaming(result.reply || '助手暂时没有返回内容。');
				},
				false,
				() => {
					this.submitting = false;
				}
			);
		}
	}
};
</script>

<style lang="less">
.page {
	min-height: 100vh;
	background: #f5f7fb;
	padding: 24rpx;
	box-sizing: border-box;
}
.hero-card,
.section-card {
	background: #fff;
	border-radius: 20rpx;
	padding: 24rpx;
	margin-bottom: 20rpx;
	box-shadow: 0 8rpx 24rpx rgba(18, 150, 219, 0.08);
}
.hero-title {
	font-size: 34rpx;
	font-weight: 700;
	color: #222;
}
.hero-desc {
	margin-top: 10rpx;
	font-size: 24rpx;
	color: #666;
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
	max-width: 80%;
	padding: 20rpx 24rpx;
	border-radius: 18rpx;
	background: #ffffff;
}
.message-row.user .bubble {
	background: #1296db;
}
.bubble-text {
	font-size: 26rpx;
	line-height: 1.6;
	color: #333;
}
.message-row.user .bubble-text {
	color: #fff;
}
.section-title {
	font-size: 28rpx;
	font-weight: 600;
	color: #222;
	margin-bottom: 18rpx;
}
.error-card {
	border: 1rpx solid #ffd7d7;
	background: #fff7f7;
}
.error-head {
	display: flex;
	justify-content: space-between;
	align-items: center;
	gap: 16rpx;
}
.error-text {
	flex: 1;
	font-size: 24rpx;
	line-height: 1.6;
	color: #c62828;
}
.error-badge {
	font-size: 22rpx;
	color: #ed6c02;
	background: rgba(237, 108, 2, 0.12);
	padding: 6rpx 12rpx;
	border-radius: 999rpx;
}
.card-list {
	display: flex;
	flex-direction: column;
	gap: 16rpx;
}
.action-card {
	padding: 20rpx;
	border-radius: 16rpx;
	background: #f7fbff;
	border: 1rpx solid #dceefe;
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
	color: #222;
}
.card-badge {
	font-size: 22rpx;
	color: #1296db;
	background: rgba(18, 150, 219, 0.12);
	padding: 6rpx 12rpx;
	border-radius: 999rpx;
}
.card-desc {
	font-size: 24rpx;
	color: #666;
	line-height: 1.5;
}
.step-list {
	display: flex;
	flex-direction: column;
	gap: 12rpx;
}
.step-item,
.tool-log {
	display: flex;
	justify-content: space-between;
	gap: 20rpx;
}
.flow-item {
	padding: 18rpx 20rpx;
	border-radius: 16rpx;
	background: #f7fbff;
	border: 1rpx solid #dceefe;
}
.flow-head {
	display: flex;
	justify-content: space-between;
	align-items: center;
	gap: 20rpx;
}
.flow-summary {
	display: block;
	margin-top: 10rpx;
	font-size: 24rpx;
	line-height: 1.6;
	color: #555;
}
.flow-meta {
	display: block;
	margin-top: 8rpx;
	font-size: 22rpx;
	color: #999;
}
.step-label,
.tool-name {
	font-size: 24rpx;
	color: #333;
}
.step-status,
.tool-summary {
	font-size: 22rpx;
	color: #999;
}
.step-status.success {
	color: #2e7d32;
}
.step-status.active {
	color: #1296db;
}
.step-status.failed {
	color: #d32f2f;
}
.step-status.waiting {
	color: #ed6c02;
}
.quick-actions {
	display: grid;
	grid-template-columns: repeat(3, 1fr);
	gap: 16rpx;
	margin: 20rpx 0;
}
.quick-btn {
	background: #fff;
	border-radius: 16rpx;
	text-align: center;
	padding: 18rpx 8rpx;
	font-size: 24rpx;
	color: #1296db;
	box-shadow: 0 6rpx 16rpx rgba(18, 150, 219, 0.08);
}
.composer {
	display: flex;
	gap: 16rpx;
	align-items: center;
}
.composer-input {
	flex: 1;
	background: #fff;
	border-radius: 16rpx;
	padding: 22rpx 24rpx;
	font-size: 26rpx;
}
.composer-btn {
	width: 150rpx;
	border-radius: 16rpx;
	background: linear-gradient(135deg, #1296db, #47c2ff);
	color: #fff;
	font-size: 26rpx;
	border: none;
}
.slim {
	padding-top: 20rpx;
	padding-bottom: 20rpx;
}
</style>
