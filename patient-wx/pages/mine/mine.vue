<template>
	<view class="page">
		<view class="hero">
			<view class="hero-card">
				<u-avatar :src="user.photo" shape="circle" size="64" @tap.stop="openUserInfoCard"></u-avatar>
				<view class="hero-content">
					<text class="hero-name">{{ heroName }}</text>
					<text class="hero-desc">{{ heroDesc }}</text>
					<view class="hero-tags">
						<text class="hero-tag">{{ cardStatusText }}</text>
						<text v-if="isLogin" class="hero-tag">{{ unreadTagText }}</text>
					</view>
				</view>
				<button class="hero-action" @tap.stop="isLogin ? logout() : loginOrRegister()">
					{{ isLogin ? copy.logout : copy.login }}
				</button>
			</view>
			<view class="stats-row">
				<view class="stat-card" v-for="item in summaryCards" :key="item.label" @tap="handleMenu(item)">
					<text class="stat-value">{{ item.value }}</text>
					<text class="stat-label">{{ item.label }}</text>
				</view>
			</view>
		</view>

		<view class="panel">
			<view class="panel-head">
				<view>
					<text class="panel-title">{{ copy.coreTitle }}</text>
					<text class="panel-subtitle">{{ copy.coreSubtitle }}</text>
				</view>
			</view>
			<view class="menu-grid">
				<view class="menu-card" v-for="item in primaryMenus" :key="item.key" @tap="handleMenu(item)">
					<view class="menu-icon" :class="'accent-' + item.accent">
						<text class="menu-icon-text">{{ item.iconText }}</text>
						<text v-if="item.key === 'messageCenter' && unreadCount > 0" class="menu-badge">
							{{ unreadCount > 99 ? '99+' : unreadCount }}
						</text>
					</view>
					<text class="menu-title">{{ item.title }}</text>
					<text class="menu-desc">{{ item.desc }}</text>
				</view>
			</view>
		</view>

		<!-- Disabled legacy entries kept intentionally:
		pendingPayment, medicineList, medicalInsurance, onlinePrescription, escortService,
		nucleicAcid, vaccine, nurseService, eInvoice, printService, inpatientDeposit,
		inpatientList, guideService, herbalDecocting, medicalExam, parkingPayment
		-->

		<view class="panel favorite-panel">
			<view class="panel-head clickable" @tap="handleMenu({ key: 'favorites', requireLogin: true })">
				<view>
					<text class="panel-title">{{ copy.favoriteTitle }}</text>
					<text class="panel-subtitle">{{ copy.favoriteSubtitle }}</text>
				</view>
				<text class="panel-link">{{ copy.viewAll }}</text>
			</view>
			<view v-if="favoriteDoctors.length > 0">
				<view class="favorite-card" v-for="doctor in favoriteDoctors" :key="doctor.id" @tap="goDoctorDetail(doctor.id)">
					<u-avatar :src="doctor.photo" size="48"></u-avatar>
					<view class="favorite-info">
						<view class="favorite-top">
							<text class="favorite-name">{{ doctor.name }}</text>
							<text class="favorite-job">{{ doctor.job || copy.defaultDoctorJob }}</text>
						</view>
						<text class="favorite-remark">{{ doctor.remark || text.noRemark }}</text>
						<view class="favorite-meta">
							<text>{{ text.pricePrefix }}{{ doctor.price || '--' }}</text>
							<text>{{ doctor.favoriteTime || copy.favoriteDone }}</text>
						</view>
					</view>
				</view>
			</view>
			<view v-else class="favorite-empty">
				<u-empty mode="data" :text="favoriteEmptyText" icon="http://cdn.uviewui.com/uview/empty/data.png"></u-empty>
			</view>
		</view>

		<view class="panel">
			<view class="panel-head">
				<view>
					<text class="panel-title">{{ copy.serviceTitle }}</text>
					<text class="panel-subtitle">{{ copy.serviceSubtitle }}</text>
				</view>
			</view>
			<view class="menu-grid compact">
				<view class="menu-card compact" v-for="item in serviceMenus" :key="item.key" @tap="handleMenu(item)">
					<view class="menu-icon small" :class="'accent-' + item.accent">
						<text class="menu-icon-text">{{ item.iconText }}</text>
					</view>
					<text class="menu-title">{{ item.title }}</text>
				</view>
			</view>
		</view>

		<view class="panel tips-panel">
			<view class="panel-head">
				<view>
					<text class="panel-title">{{ copy.tipsTitle }}</text>
					<text class="panel-subtitle">{{ copy.tipsSubtitle }}</text>
				</view>
			</view>
			<view class="tip-list">
				<view class="tip-item" v-for="item in tips" :key="item.title">
					<text class="tip-title">{{ item.title }}</text>
					<text class="tip-desc">{{ item.desc }}</text>
				</view>
			</view>
		</view>

		<view class="page-foot">
			<text class="page-foot-title">{{ copy.pageFootTitle }}</text>
			<text class="page-foot-desc">{{ copy.pageFootDesc }}</text>
		</view>

		<u-toast ref="uToast" />
	</view>
</template>

<script>
const copy = {
	login: '\u767b\u5f55',
	logout: '\u9000\u51fa',
	unreadLabel: '\u672a\u8bfb\u6d88\u606f',
	coreTitle: '\u6838\u5fc3\u529f\u80fd',
	coreSubtitle: '\u4fdd\u7559\u5f53\u524d\u5df2\u63a5\u901a\u5e76\u53ef\u76f4\u63a5\u4f7f\u7528\u7684\u5165\u53e3',
	favoriteTitle: '\u6536\u85cf\u533b\u751f',
	favoriteSubtitle: '\u5e38\u770b\u7684\u533b\u751f\u4f1a\u5c55\u793a\u5728\u8fd9\u91cc\uff0c\u65b9\u4fbf\u4e0b\u6b21\u5feb\u901f\u8fdb\u5165',
	viewAll: '\u67e5\u770b\u5168\u90e8',
	defaultDoctorJob: '\u533b\u751f',
	favoriteDone: '\u5df2\u6536\u85cf',
	serviceTitle: '\u5c31\u533b\u670d\u52a1',
	serviceSubtitle: '\u56f4\u7ed5\u6302\u53f7\u548c\u95ee\u8bca\u7684\u5e38\u7528\u9875\u9762\u8865\u9f50\u5230\u4e2a\u4eba\u4e2d\u5fc3',
	tipsTitle: '\u6e29\u99a8\u63d0\u793a',
	tipsSubtitle: '\u8865\u5145\u5173\u952e\u5f15\u5bfc\uff0c\u8ba9\u9875\u9762\u4fe1\u606f\u66f4\u5b8c\u6574',
	pageFootTitle: '\u4e2a\u4eba\u4e2d\u5fc3',
	pageFootDesc: '\u5df2\u805a\u5408\u767b\u5f55\u6001\u3001\u5c31\u8bca\u5361\u72b6\u6001\u3001\u5e38\u7528\u5165\u53e3\u3001\u6d88\u606f\u63d0\u9192\u548c\u6536\u85cf\u533b\u751f\u9884\u89c8\u3002',
	loginHero: '\u767b\u5f55\u540e\u67e5\u770b\u5b8c\u6574\u4e2a\u4eba\u4e2d\u5fc3',
	defaultUser: '\u5fae\u4fe1\u7528\u6237',
	loggedOutDesc: '\u767b\u5f55\u540e\u53ef\u67e5\u770b\u6d88\u606f\u3001\u6302\u53f7\u8bb0\u5f55\u3001\u6536\u85cf\u533b\u751f\u548c\u5c31\u8bca\u5361\u72b6\u6001\u3002',
	noPhone: '\u6682\u672a\u7ed1\u5b9a\u624b\u673a\u53f7\uff0c\u5efa\u8bae\u5148\u5b8c\u6210\u5b9e\u540d\u767b\u8bb0\u3002',
	loggedOutStatus: '\u672a\u767b\u5f55',
	hasCard: '\u5df2\u521b\u5efa\u5c31\u8bca\u5361',
	noCard: '\u5f85\u521b\u5efa\u5c31\u8bca\u5361',
	summaryCard: '\u5c31\u8bca\u5361',
	summaryReady: '\u5df2\u521b\u5efa',
	summaryTodo: '\u5f85\u5b8c\u5584',
	summaryFavorite: '\u5e38\u770b\u533b\u751f',
	favoriteEmpty: '\u6682\u65e0\u6536\u85cf\u533b\u751f',
	favoriteLoginHint: '\u767b\u5f55\u540e\u67e5\u770b\u6536\u85cf\u533b\u751f',
	noRemark: '\u6682\u65e0\u64c5\u957f\u65b9\u5411\u4ecb\u7ecd',
	pricePrefix: '\u6302\u53f7\u8d39 \u00a5',
	menuUserCardTitle: '\u5c31\u8bca\u5361',
	menuUserCardDesc: '\u5b9e\u540d\u5efa\u6863\u4e0e\u5c31\u8bca\u4fe1\u606f\u7ba1\u7406',
	menuRegistrationTitle: '\u95e8\u8bca\u6302\u53f7',
	menuRegistrationDesc: '\u6309\u79d1\u5ba4\u548c\u533b\u751f\u5feb\u901f\u9884\u7ea6',
	menuAiTitle: 'AI\u52a9\u624b',
	menuAiDesc: '\u901a\u8fc7\u75c7\u72b6\u63cf\u8ff0\u8f85\u52a9\u6302\u53f7',
	menuRegistrationListTitle: '\u6211\u7684\u6302\u53f7',
	menuRegistrationListDesc: '\u67e5\u770b\u5386\u53f2\u8bb0\u5f55\u548c\u72b6\u6001',
	menuMessageTitle: '\u6d88\u606f\u4e2d\u5fc3',
	menuMessageDesc: '\u67e5\u770b\u63d0\u9192\u901a\u77e5\u548c\u7cfb\u7edf\u6d88\u606f',
	menuDoctorTitle: '\u533b\u751f\u5217\u8868',
	menuDoctorDesc: '\u5feb\u901f\u67e5\u627e\u548c\u7b5b\u9009\u533b\u751f',
	menuFavoriteTitle: '\u6211\u7684\u6536\u85cf',
	menuFavoriteDesc: '\u5e38\u770b\u533b\u751f\u96c6\u4e2d\u5c55\u793a',
	menuEvaluationTitle: '\u6211\u7684\u8bc4\u4ef7',
	menuEvaluationDesc: '\u67e5\u770b\u5386\u53f2\u8bc4\u4ef7\u4e0e\u53cd\u9988',
	serviceDoctorTitle: '\u533b\u751f\u5217\u8868',
	serviceDiseaseTitle: '\u75be\u75c5\u767e\u79d1',
	serviceFavoriteTitle: '\u6211\u7684\u6536\u85cf',
	serviceEvaluationTitle: '\u5c31\u8bca\u8bc4\u4ef7',
	tipCardTitle: '\u5148\u5b8c\u5584\u5c31\u8bca\u5361',
	tipCardDesc: '\u6302\u53f7\u524d\u5efa\u8bae\u5148\u5b8c\u6210\u5b9e\u540d\u5efa\u6863\uff0c\u540e\u7eed\u53ef\u4ee5\u76f4\u63a5\u8fdb\u5165\u9009\u53f7\u6d41\u7a0b\u3002',
	tipAiTitle: 'AI\u52a9\u624b\u652f\u6301\u75c7\u72b6\u627e\u79d1\u5ba4',
	tipAiDesc: '\u50cf\u201c\u53e3\u8154\u4e0d\u8212\u670d\u201d\u201c\u7259\u75db\u201d\u8fd9\u7c7b\u63cf\u8ff0\uff0c\u7cfb\u7edf\u4f1a\u4f18\u5148\u8865\u5168\u5230\u5408\u9002\u79d1\u5ba4\u518d\u7ee7\u7eed\u67e5\u8be2\u3002',
	tipFavoriteTitle: '\u6536\u85cf\u533b\u751f\u540e\u53ef\u5feb\u901f\u590d\u7528',
	tipFavoriteDesc: '\u5e38\u770b\u7684\u533b\u751f\u4f1a\u5728\u4e2a\u4eba\u4e2d\u5fc3\u6301\u7eed\u5c55\u793a\uff0c\u51cf\u5c11\u91cd\u590d\u641c\u7d22\u548c\u7b5b\u9009\u3002',
	profileDesc: '\u83b7\u53d6\u7528\u6237\u4fe1\u606f',
	male: '\u7537',
	female: '\u5973',
	loginSuccess: '\u767b\u5f55\u6210\u529f',
	userProfileDenied: '\u672a\u83b7\u53d6\u5230\u5fae\u4fe1\u7528\u6237\u4fe1\u606f',
	loginFailed: '\u5fae\u4fe1\u767b\u5f55\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5',
	logoutSuccess: '\u5df2\u9000\u51fa\u767b\u5f55',
	loginRequired: '\u8bf7\u5148\u767b\u5f55\u5c0f\u7a0b\u5e8f'
};

export default {
	data() {
		return {
			flag: 'logout',
			unreadCount: 0,
			hasUserCard: false,
			favoriteDoctors: [],
			copy,
			text: {
				noRemark: copy.noRemark,
				pricePrefix: copy.pricePrefix
			},
			user: {
				username: '',
				photo: '',
				tel: ''
			},
			primaryMenus: [
				{ key: 'userCard', title: copy.menuUserCardTitle, desc: copy.menuUserCardDesc, iconText: '\u5361', accent: 'blue', requireLogin: true },
				{ key: 'registration', title: copy.menuRegistrationTitle, desc: copy.menuRegistrationDesc, iconText: '\u6302', accent: 'orange', requireLogin: false },
				{ key: 'aiAgent', title: copy.menuAiTitle, desc: copy.menuAiDesc, iconText: 'AI', accent: 'green', requireLogin: false },
				{ key: 'registrationList', title: copy.menuRegistrationListTitle, desc: copy.menuRegistrationListDesc, iconText: '\u5355', accent: 'navy', requireLogin: true },
				{ key: 'messageCenter', title: copy.menuMessageTitle, desc: copy.menuMessageDesc, iconText: '\u4fe1', accent: 'red', requireLogin: true },
				{ key: 'doctorList', title: copy.menuDoctorTitle, desc: copy.menuDoctorDesc, iconText: '\u533b', accent: 'cyan', requireLogin: false },
				{ key: 'favorites', title: copy.menuFavoriteTitle, desc: copy.menuFavoriteDesc, iconText: '\u85cf', accent: 'purple', requireLogin: true },
				{ key: 'evaluation', title: copy.menuEvaluationTitle, desc: copy.menuEvaluationDesc, iconText: '\u8bc4', accent: 'amber', requireLogin: true }
			],
			serviceMenus: [
				{ key: 'doctorList', title: copy.serviceDoctorTitle, iconText: '\u533b', accent: 'cyan', requireLogin: false },
				{ key: 'disease', title: copy.serviceDiseaseTitle, iconText: '\u79d1', accent: 'green', requireLogin: false },
				{ key: 'favorites', title: copy.serviceFavoriteTitle, iconText: '\u85cf', accent: 'purple', requireLogin: true },
				{ key: 'evaluation', title: copy.serviceEvaluationTitle, iconText: '\u8bc4', accent: 'amber', requireLogin: true }
			],
			tips: [
				{ title: copy.tipCardTitle, desc: copy.tipCardDesc },
				{ title: copy.tipAiTitle, desc: copy.tipAiDesc },
				{ title: copy.tipFavoriteTitle, desc: copy.tipFavoriteDesc }
			]
		};
	},
	computed: {
		isLogin() {
			return this.flag === 'login';
		},
		heroName() {
			if (!this.isLogin) {
				return this.copy.loginHero;
			}
			return this.user.username || this.copy.defaultUser;
		},
		heroDesc() {
			if (!this.isLogin) {
				return this.copy.loggedOutDesc;
			}
			return this.user.tel || this.copy.noPhone;
		},
		unreadTagText() {
			return `${this.copy.unreadLabel} ${this.unreadCount}`;
		},
		cardStatusText() {
			if (!this.isLogin) {
				return this.copy.loggedOutStatus;
			}
			return this.hasUserCard ? this.copy.hasCard : this.copy.noCard;
		},
		summaryCards() {
			return [
				{
					label: this.copy.summaryCard,
					value: !this.isLogin ? '--' : this.hasUserCard ? this.copy.summaryReady : this.copy.summaryTodo,
					key: 'userCard',
					requireLogin: true
				},
				{
					label: this.copy.unreadLabel,
					value: this.isLogin ? `${this.unreadCount}` : '--',
					key: 'messageCenter',
					requireLogin: true
				},
				{
					label: this.copy.summaryFavorite,
					value: this.isLogin ? `${this.favoriteDoctors.length}` : '--',
					key: 'favorites',
					requireLogin: true
				}
			];
		},
		favoriteEmptyText() {
			return this.isLogin ? this.copy.favoriteEmpty : this.copy.favoriteLoginHint;
		}
	},
	methods: {
		showMessage(message, type = 'none') {
			if (this.$refs.uToast) {
				this.$refs.uToast.show({
					message,
					type,
					duration: 1500
				});
				return;
			}
			uni.showToast({
				icon: type === 'success' ? 'success' : 'none',
				title: message
			});
		},
		normalizeUserPhoto(path) {
			if (!path) {
				return '';
			}
			if (/^(https?:)?\/\//.test(path) || path.indexOf('data:image') === 0 || path.indexOf('wxfile://') === 0) {
				return path;
			}
			return this.fileUrl(path);
		},
		loginOrRegister() {
			uni.getUserProfile({
				desc: this.copy.profileDesc,
				success: profileResp => {
					let info = profileResp.userInfo || {};
					uni.login({
						provider: 'weixin',
						success: loginResp => {
							let data = {
								code: loginResp.code,
								nickname: info.nickName,
								photo: info.avatarUrl,
								sex: info.gender == 0 ? this.copy.male : this.copy.female
							};
							this.ajax(this.api.loginOrRegister, 'POST', data, resp => {
								let token = resp.data.token;
								uni.setStorageSync('token', token);
								this.flag = 'login';
								this.user.username = info.nickName || '';
								this.user.photo = info.avatarUrl || '';
								this.user.tel = resp.data.tel || '';
								this.showMessage(resp.data.msg || this.copy.loginSuccess, 'success');
								this.loadMineData();
							});
						},
						fail: () => {
							this.showMessage(this.copy.loginFailed);
						}
					});
				},
				fail: () => {
					this.showMessage(this.copy.userProfileDenied);
				}
			});
		},
		logout() {
			uni.removeStorageSync('token');
			this.flag = 'logout';
			this.resetMineState();
			uni.showToast({
				icon: 'success',
				title: this.copy.logoutSuccess
			});
		},
		resetMineState() {
			this.unreadCount = 0;
			this.hasUserCard = false;
			this.favoriteDoctors = [];
			this.user = {
				username: '',
				photo: '',
				tel: ''
			};
		},
		loadMineData() {
			this.loadUserProfile();
			this.loadFavoriteDoctors();
			this.loadUnreadCount();
			this.loadUserCardStatus();
		},
		loadUserProfile() {
			this.ajax(
				this.api.searchUserInfo,
				'GET',
				{},
				resp => {
					let result = resp.data && resp.data.result ? resp.data.result : null;
					if (!result) {
						return;
					}
					this.flag = 'login';
					this.user.username = result.nickname || result.name || '';
					this.user.photo = this.normalizeUserPhoto(result.photo);
					this.user.tel = result.tel || '';
				},
				false
			);
		},
		loadFavoriteDoctors() {
			this.ajax(
				this.api.searchFavoriteDoctorByPage,
				'POST',
				{
					page: 1,
					length: 10
				},
				resp => {
					let result = resp.data && resp.data.result ? resp.data.result : {};
					let rows = result.list || [];
					this.favoriteDoctors = rows.slice(0, 3).map(item => ({
						...item,
						photo: this.doctorPhotoUrl(item.photo)
					}));
				},
				false
			);
		},
		loadUnreadCount() {
			this.ajax(
				this.api.searchUnreadCount,
				'GET',
				{},
				resp => {
					this.unreadCount = resp.data.result || 0;
				},
				false
			);
		},
		loadUserCardStatus() {
			this.ajax(
				this.api.hasUserInfoCard,
				'GET',
				{},
				resp => {
					this.hasUserCard = !!resp.data.result;
				},
				false
			);
		},
		openUserInfoCard() {
			if (!this.ensureLogin()) {
				return;
			}
			uni.navigateTo({
				url: this.hasUserCard ? '/user/user_info_card_detail' : '/user/fill_user_info/fill_user_info'
			});
		},
		handleMenu(item) {
			if (!item) {
				return;
			}
			if (item.requireLogin && !this.ensureLogin()) {
				return;
			}
			switch (item.key) {
				case 'userCard':
					this.openUserInfoCard();
					break;
				case 'registration':
					uni.navigateTo({ url: '/registration/medical_dept_list/medical_dept_list' });
					break;
				case 'aiAgent':
					uni.navigateTo({ url: '/agent/chat/chat' });
					break;
				case 'registrationList':
					uni.switchTab({ url: '/pages/registration_list/registration_list' });
					break;
				case 'messageCenter':
					uni.switchTab({ url: '/pages/message_list/message_list' });
					break;
				case 'doctorList':
					uni.navigateTo({ url: '/display/allDoctor/allDoctor' });
					break;
				case 'favorites':
					uni.navigateTo({ url: '/display/favorite_doctor/favorite_doctor' });
					break;
				case 'evaluation':
					uni.navigateTo({ url: '/registration/evaluation/evaluation?mode=list' });
					break;
				case 'disease':
					uni.navigateTo({ url: '/display/allInfo/allInfo' });
					break;
				default:
					break;
			}
		},
		goDoctorDetail(doctorId) {
			uni.navigateTo({
				url: `/display/doctor_detail/doctor_detail?doctorId=${doctorId}`
			});
		},
		ensureLogin() {
			let token = uni.getStorageSync('token');
			if (!token) {
				uni.showToast({
					icon: 'none',
					title: this.copy.loginRequired
				});
				return false;
			}
			return true;
		}
	},
	onShow() {
		let token = uni.getStorageSync('token');
		if (token) {
			this.flag = 'login';
			this.loadMineData();
			return;
		}
		this.flag = 'logout';
		this.resetMineState();
	}
};
</script>

<style lang="less">
@import url(mine.less);
</style>
