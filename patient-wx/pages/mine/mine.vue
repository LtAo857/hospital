<template>
	<view>
		<view class="top-container">
			<u-avatar :src="user.photo" shape="square" size="52" @tap.stop="openUserInfoCard"></u-avatar>
			<view class="info">
				<view v-if="flag == 'logout'" open-type="getUserInfo" @tap="loginOrRegister">
					<text class="operate">注册 / 登陆</text>
					<text class="remark">请登陆医疗挂号挂号小程序</text>
				</view>
				<view v-if="flag == 'login'">
					<view class="left">
						<text class="username">{{ user.username }}</text>
						<text class="tel">{{ user.tel != null ? user.tel : '未绑定手机（需实名登记）' }}</text>
					</view>
					<view class="right">
						<text
							class="logout"
							style="display: block; font-size: 38rpx; font-weight: bold; color: #fff; margin-bottom: 5rpx; margin-left: 250rpx;"
							@tap="logout"
						>退出登录</text>
					</view>
				</view>
			</view>
		</view>

		<view class="navigator-container">
			<u-grid :border="false" col="4" @click="navigatorHandle">
				<u-grid-item name="实名登记">
					<view class="navigator-icon"><view class="navigator-icon-1" /></view>
					<text class="title">登记</text>
				</u-grid-item>
				<u-grid-item name="挂号就诊">
					<view class="navigator-icon"><view class="navigator-icon-4" /></view>
					<text class="title">挂号</text>
				</u-grid-item>
				<u-grid-item name="AI挂号助手">
					<view class="navigator-icon"><view class="navigator-icon-4" /></view>
					<text class="title">AI助手</text>
				</u-grid-item>
				<u-grid-item name="医生列表">
					<view class="navigator-icon"><view class="navigator-icon-2" /></view>
					<text class="title">医生</text>
				</u-grid-item>
				<u-grid-item name="疾病百科">
					<view class="navigator-icon"><view class="navigator-icon-4" /></view>
					<text class="title">百科</text>
				</u-grid-item>
				<u-grid-item name="视频问诊">
					<view class="navigator-icon"><view class="navigator-icon-4" /></view>
					<text class="title">视频问诊</text>
				</u-grid-item>
				<u-grid-item name="我的评价">
					<view class="navigator-icon"><view class="navigator-icon-4" /></view>
					<text class="title">我的评价</text>
				</u-grid-item>
				<u-grid-item :name="text.favoriteNavName">
					<view class="navigator-icon"><view class="navigator-icon-4" /></view>
					<text class="title">{{ text.favoriteNavTitle }}</text>
				</u-grid-item>
			</u-grid>
		</view>

		<view class="publicity-container">
			<image :src="publicityBannerUrl" mode="widthFix" class="banner" />
		</view>

		<view class="mine-container">
			<view class="title-row">
				<text class="title">我的问诊</text>
				<u-icon label="更多" labelPos="left" size="15" name="arrow-right"></u-icon>
			</view>
			<view class="content">
				<u-grid :border="false" @click="navigatorHandle" col="5">
					<u-grid-item name="待支付">
						<image :src="img['mine-icon-1']" mode="widthFix" class="navigator-icon"></image>
						<text class="title">待支付</text>
					</u-grid-item>
					<u-grid-item name="问诊记录">
						<image :src="img['mine-icon-2']" mode="widthFix" class="navigator-icon"></image>
						<text class="title">问诊记录</text>
					</u-grid-item>
					<u-grid-item name="药品清单">
						<image :src="img['mine-icon-3']" mode="widthFix" class="navigator-icon"></image>
						<text class="title">药品清单</text>
					</u-grid-item>
					<u-grid-item name="医保报销">
						<image :src="img['mine-icon-4']" mode="widthFix" class="navigator-icon"></image>
						<text class="title">医保报销</text>
					</u-grid-item>
					<u-grid-item name="在线续方">
						<image :src="img['mine-icon-5']" mode="widthFix" class="navigator-icon"></image>
						<text class="title">在线续方</text>
					</u-grid-item>
				</u-grid>
				<image :src="otherBannerUrl" mode="widthFix" class="banner"></image>
			</view>
		</view>

		<view class="ad-container">
			<image :src="adBannerUrl[0]" mode="widthFix" class="banner" />
		</view>

		<u-toast ref="uToast" />

		<view class="service-container">
			<view class="title-row">
				<text class="title">医疗挂号挂号服务</text>
				<u-icon label="更多" labelPos="left" size="15" name="arrow-right"></u-icon>
			</view>
			<view class="content">
				<u-grid :border="false" @click="navigatorHandle" col="5">
					<u-grid-item name="挂号就诊">
						<image :src="img['service-icon-1']" mode="widthFix" class="navigator-icon"></image>
						<text class="title">挂号就诊</text>
					</u-grid-item>
					<u-grid-item name="预约陪诊">
						<image :src="img['service-icon-2']" mode="widthFix" class="navigator-icon"></image>
						<text class="title">预约陪诊</text>
					</u-grid-item>
					<u-grid-item name="核酸检测">
						<image :src="img['service-icon-3']" mode="widthFix" class="navigator-icon"></image>
						<text class="title">核酸检测</text>
					</u-grid-item>
					<u-grid-item name="疫苗预约">
						<image :src="img['service-icon-4']" mode="widthFix" class="navigator-icon"></image>
						<text class="title">疫苗预约</text>
					</u-grid-item>
					<u-grid-item name="护工服务">
						<image :src="img['service-icon-5']" mode="widthFix" class="navigator-icon"></image>
						<text class="title">护工服务</text>
					</u-grid-item>
					<u-grid-item name="视频问诊">
						<image :src="img['service-icon-6']" mode="widthFix" class="navigator-icon"></image>
						<text class="title">视频问诊</text>
					</u-grid-item>
					<u-grid-item name="电子发票">
						<image :src="img['service-icon-7']" mode="widthFix" class="navigator-icon"></image>
						<text class="title">电子发票</text>
					</u-grid-item>
					<u-grid-item name="打印服务">
						<image :src="img['service-icon-8']" mode="widthFix" class="navigator-icon"></image>
						<text class="title">打印服务</text>
					</u-grid-item>
					<u-grid-item name="住院押金">
						<image :src="img['service-icon-9']" mode="widthFix" class="navigator-icon"></image>
						<text class="title">住院押金</text>
					</u-grid-item>
					<u-grid-item name="住院清单">
						<image :src="img['service-icon-10']" mode="widthFix" class="navigator-icon"></image>
						<text class="title">住院清单</text>
					</u-grid-item>
					<u-grid-item name="线上导诊">
						<image :src="img['service-icon-11']" mode="widthFix" class="navigator-icon"></image>
						<text class="title">线上导诊</text>
					</u-grid-item>
					<u-grid-item name="中药代煎">
						<image :src="img['service-icon-12']" mode="widthFix" class="navigator-icon"></image>
						<text class="title">中药代煎</text>
					</u-grid-item>
					<u-grid-item name="体检预约">
						<image :src="img['service-icon-13']" mode="widthFix" class="navigator-icon"></image>
						<text class="title">体检预约</text>
					</u-grid-item>
					<u-grid-item name="停车缴费">
						<image :src="img['service-icon-14']" mode="widthFix" class="navigator-icon"></image>
						<text class="title">停车缴费</text>
					</u-grid-item>
					<u-grid-item name="疾病百科">
						<image :src="img['service-icon-15']" mode="widthFix" class="navigator-icon"></image>
						<text class="title">疾病百科</text>
					</u-grid-item>
				</u-grid>
			</view>
		</view>

		<view class="ad-container">
			<image :src="adBannerUrl[1]" mode="widthFix" class="banner" />
		</view>
	</view>
</template>

<script>
export default {
	data() {
		return {
			img: {
				'mine-icon-1': `${this.patientUrl}/page/mine/mine-icon-1.png`,
				'mine-icon-2': `${this.patientUrl}/page/mine/mine-icon-2.png`,
				'mine-icon-3': `${this.patientUrl}/page/mine/mine-icon-3.png`,
				'mine-icon-4': `${this.patientUrl}/page/mine/mine-icon-4.png`,
				'mine-icon-5': `${this.patientUrl}/page/mine/mine-icon-5.png`,
				'service-icon-1': `${this.patientUrl}/page/mine/service-icon-1.png`,
				'service-icon-2': `${this.patientUrl}/page/mine/service-icon-2.png`,
				'service-icon-3': `${this.patientUrl}/page/mine/service-icon-3.png`,
				'service-icon-4': `${this.patientUrl}/page/mine/service-icon-4.png`,
				'service-icon-5': `${this.patientUrl}/page/mine/service-icon-5.png`,
				'service-icon-6': `${this.patientUrl}/page/mine/service-icon-6.png`,
				'service-icon-7': `${this.patientUrl}/page/mine/service-icon-7.png`,
				'service-icon-8': `${this.patientUrl}/page/mine/service-icon-8.png`,
				'service-icon-9': `${this.patientUrl}/page/mine/service-icon-9.png`,
				'service-icon-10': `${this.patientUrl}/page/mine/service-icon-10.png`,
				'service-icon-11': `${this.patientUrl}/page/mine/service-icon-11.png`,
				'service-icon-12': `${this.patientUrl}/page/mine/service-icon-12.png`,
				'service-icon-13': `${this.patientUrl}/page/mine/service-icon-13.png`,
				'service-icon-14': `${this.patientUrl}/page/mine/service-icon-14.png`,
				'service-icon-15': `${this.patientUrl}/page/mine/service-icon-15.png`
			},
			flag: 'logout',
			code: null,
			favoriteDoctors: [],
			favoritePage: 1,
			favoriteLength: 10,
			text: {
				favoriteTitle: '我的收藏医生',
				favoriteNavName: '我的收藏',
				favoriteNavTitle: '我的收藏',
				noFavorite: '暂无收藏医生',
				noRemark: '暂无擅长方向介绍',
				pricePrefix: '挂号费 ¥'
			},
			user: {
				username: '',
				photo: null,
				tel: null
			},
			bannerUrl: `${this.patientUrl}/banner/banner-5.jpg`,
			publicityBannerUrl: `${this.patientUrl}/banner/banner-2.jpg`,
			otherBannerUrl: `${this.patientUrl}/banner/banner-1.jpg`,
			adBannerUrl: [`${this.patientUrl}/banner/banner-8.jpg`, `${this.patientUrl}/banner/banner-5.jpg`]
		};
	},
	methods: {
		openUserInfoCard: function() {
			let token = uni.getStorageSync('token');
			if (token == null || token.length == 0) {
				uni.showToast({
					icon: 'error',
					title: '请先登录小程序'
				});
				return;
			}
			uni.navigateTo({
				url: '/user/user_info_card_detail'
			});
		},
		loginOrRegister: function() {
			let that = this;
			uni.login({
				provider: 'weixin',
				success: function(resp) {
					that.code = resp.code;
				}
			});

			uni.getUserProfile({
				desc: '获取用户信息',
				success: function(resp) {
					let info = resp.userInfo;
					let nickname = info.nickName;
					let photo = info.avatarUrl;
					let sex = info.gender == 0 ? '男' : '女';
					let data = {
						code: that.code,
						nickname: nickname,
						photo: photo,
						sex: sex
					};
					that.ajax(that.api.loginOrRegister, 'POST', data, function(resp) {
						let msg = resp.data.msg;
						that.$refs.uToast.show({
							message: msg,
							type: 'success',
							duration: 1200,
							complete: function() {
								let token = resp.data.token;
								uni.setStorageSync('token', token);
								that.flag = 'login';
								that.user.username = nickname;
								that.user.photo = photo;
								if (resp.data.hasOwnProperty('tel')) {
									that.user.tel = resp.data.tel;
								}
								that.loadFavoriteDoctors();
							}
						});
					});
				}
			});
		},
		logout: function() {
			uni.setStorageSync('token', null);
			this.flag = 'logout';
			this.favoriteDoctors = [];
			uni.showToast({
				icon: 'success',
				title: '退出成功'
			});
		},
		loadFavoriteDoctors: function() {
			let that = this;
			that.ajax(
				that.api.searchFavoriteDoctorByPage,
				'POST',
				{
					page: that.favoritePage,
					length: that.favoriteLength
				},
				function(resp) {
					let result = resp.data.result;
					let list = result && result.list ? result.list : [];
					for (let one of list) {
						one.photo = that.doctorPhotoUrl(one.photo);
					}
					that.favoriteDoctors = list.slice(0, 3);
				},
				false
			);
		},
		goDoctorDetail: function(doctorId) {
			uni.navigateTo({
				url: `/display/doctor_detail/doctor_detail?doctorId=${doctorId}`
			});
		},
		navigatorHandle: function(name) {
			if (name == 'AI挂号助手') {
				uni.navigateTo({
					url: '/agent/chat/chat'
				});
				return;
			}
			let token = uni.getStorageSync('token');
			if (token == null || token.length == 0) {
				uni.showToast({
					icon: 'error',
					title: '请先登录小程序'
				});
				return;
			}
			let url = null;
			if (name == '实名登记') {
				url = '/user/fill_user_info/fill_user_info';
			} else if (name == '医生列表') {
				url = '/display/allDoctor/allDoctor';
			} else if (name == '我的宠物') {
				url = '/user/pet_info/pet_info';
			} else if (name == '疾病百科') {
				url = '/display/allInfo/allInfo';
			} else if (name == '视频问诊') {
				url = '/video_diagnose/order_list/order_list';
			} else if (name == '我的评价') {
				url = '/registration/evaluation/evaluation?mode=list';
			} else if (name == this.text.favoriteNavName) {
				url = '/display/favorite_doctor/favorite_doctor';
			} else if (name == '挂号就诊') {
				url = '/registration/medical_dept_list/medical_dept_list';
			}
			uni.navigateTo({
				url: url
			});
		}
	},
	onShow: function() {
		let that = this;
		let token = uni.getStorageSync('token');
		if (token != null) {
			that.ajax(
				that.api.searchUserInfo,
				'GET',
				{},
				function(resp) {
					if (resp.data.hasOwnProperty('result')) {
						that.flag = 'login';
						let result = resp.data.result;
						that.user.username = result.nickname;
						that.user.photo = result.photo;
						that.user.tel = result.tel;
						that.loadFavoriteDoctors();
					}
				},
				false
			);
		} else {
			that.favoriteDoctors = [];
		}
	}
};
</script>

<style lang="less">
@import url(mine.less);
</style>
