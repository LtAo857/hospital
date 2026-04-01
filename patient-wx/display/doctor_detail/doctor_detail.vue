<template>
	<view class="page">
		<view class="hero-card">
			<view class="doctor-main">
				<u-avatar :src="photo" size="68"></u-avatar>
				<view class="doctor-meta">
					<view class="headline">
						<text class="name">{{ name }}</text>
						<text class="job">{{ job }}</text>
					</view>
					<text class="remark">{{ remark || text.emptyRemark }}</text>
					<view class="tags">
						<text class="tag">{{ text.ratingPrefix }} {{ formatScore(avgScore) }}</text>
						<text class="tag">{{ totalCount }} {{ text.reviewUnit }}</text>
						<text class="tag">{{ text.feePrefix }} {{ price || '--' }}</text>
					</view>
				</view>
			</view>
			<view class="action-row">
				<uni-fav
					:checked="isFavorite"
					bg-color="#eef2f7"
					bg-color-checked="#34ba97"
					fg-color="#5f6b7a"
					fg-color-checked="#ffffff"
					:content-text="favoriteText"
					@click="toggleFavorite"
				/>
				<u-button type="primary" shape="circle" size="mini" @click="goRegister">
					{{ registerButtonText }}
				</u-button>
			</view>
		</view>

		<view class="panel">
			<view class="panel-header">
				<text class="panel-title">{{ text.contactTitle }}</text>
			</view>
			<text class="panel-text">{{ tel || text.emptyPhone }}</text>
		</view>

		<view class="panel">
			<view class="panel-header">
				<text class="panel-title">{{ text.profileTitle }}</text>
			</view>
			<u-parse v-if="description" :content="description"></u-parse>
			<text v-else class="panel-text">{{ text.emptyProfile }}</text>
		</view>

		<view class="panel clickable" @tap="goEvaluation">
			<view class="panel-header">
				<text class="panel-title">{{ text.reviewTitle }}</text>
				<u-icon name="arrow-right" size="16" color="#a0a0a0"></u-icon>
			</view>
			<view class="evaluation-summary" v-if="Number(totalCount) > 0">
				<text class="score">{{ formatScore(avgScore) }}</text>
				<text class="summary-text">{{ text.reviewSummaryPrefix }} {{ totalCount }} {{ text.reviewSummarySuffix }}</text>
			</view>
			<text v-else class="panel-text">{{ text.emptyReview }}</text>
		</view>
	</view>
</template>

<script>
import uniFav from '@/uni_modules/uni-ui/components/uni-fav/uni-fav.vue';

export default {
	components: {
		uniFav
	},
	data() {
		return {
			doctorId: null,
			deptSubId: null,
			date: '',
			name: '',
			photo: '',
			job: '',
			remark: '',
			description: '',
			tel: '',
			price: '',
			avgScore: 0,
			totalCount: 0,
			isFavorite: false,
			text: {
				emptyRemark: '\u6682\u65e0\u64c5\u957f\u65b9\u5411\u4ecb\u7ecd',
				ratingPrefix: '\u8bc4\u5206',
				reviewUnit: '\u6761\u8bc4\u4ef7',
				feePrefix: '\u6302\u53f7\u8d39 \u00a5',
				contactTitle: '\u8054\u7cfb\u65b9\u5f0f',
				emptyPhone: '\u6682\u672a\u63d0\u4f9b\u8054\u7cfb\u7535\u8bdd',
				profileTitle: '\u533b\u751f\u7b80\u4ecb',
				emptyProfile: '\u6682\u65e0\u7b80\u4ecb',
				reviewTitle: '\u60a3\u8005\u8bc4\u4ef7',
				reviewSummaryPrefix: '\u7efc\u5408\u8bc4\u5206\uff0c\u7d2f\u8ba1',
				reviewSummarySuffix: '\u6761\u8bc4\u4ef7',
				emptyReview: '\u6682\u65e0\u8bc4\u4ef7\uff0c\u70b9\u51fb\u67e5\u770b\u8be6\u60c5',
				loginRequired: '\u8bf7\u5148\u767b\u5f55\u5c0f\u7a0b\u5e8f',
				saved: '\u6536\u85cf\u6210\u529f',
				removed: '\u5df2\u53d6\u6d88\u6536\u85cf',
				cardRequired: '\u8bf7\u5148\u5b8c\u5584\u5c31\u8bca\u5361\u4fe1\u606f',
				bookNow: '\u53bb\u6302\u53f7',
				goNotice: '\u9884\u7ea6\u6302\u53f7'
			}
		};
	},
	computed: {
		registerButtonText() {
			return this.deptSubId && this.date ? this.text.bookNow : this.text.goNotice;
		},
		favoriteText() {
			return {
				contentDefault: '\u6536\u85cf',
				contentFav: '\u5df2\u6536\u85cf'
			};
		}
	},
	methods: {
		loadDoctorDetail: function() {
			let that = this;
			that.ajax(
				that.api.searchDoctorInfoById,
				'POST',
				{ id: that.doctorId },
				function(resp) {
					let result = resp.data;
					that.name = result.name;
					that.photo = that.doctorPhotoUrl(result.photo);
					that.job = result.job;
					that.remark = result.remark;
					that.description = result.description;
					that.tel = result.tel;
					that.price = result.price;
					that.avgScore = result.avgScore || 0;
					that.totalCount = result.totalCount || 0;
					that.isFavorite = !!result.isFavorite;
				},
				false
			);
		},
		formatScore: function(score) {
			let value = Number(score || 0);
			if (!value) {
				return '--';
			}
			return value.toFixed(1);
		},
		ensureLogin: function() {
			let token = uni.getStorageSync('token');
			if (token) {
				return true;
			}
			uni.showToast({
				icon: 'none',
				title: this.text.loginRequired
			});
			setTimeout(function() {
				uni.switchTab({
					url: '/pages/mine/mine'
				});
			}, 1200);
			return false;
		},
		toggleFavorite: function() {
			let that = this;
			if (!that.ensureLogin()) {
				return;
			}
			let url = that.isFavorite ? that.api.unfavoriteDoctor : that.api.favoriteDoctor;
			that.ajax(
				url,
				'POST',
				{ doctorId: that.doctorId },
				function(resp) {
					if (resp.data.result) {
						that.isFavorite = !that.isFavorite;
						uni.showToast({
							icon: 'none',
							title: that.isFavorite ? that.text.saved : that.text.removed
						});
					} else {
						uni.showToast({
							icon: 'none',
							title: that.text.cardRequired
						});
					}
				},
				false
			);
		},
		goRegister: function() {
			if (this.deptSubId && this.date) {
				uni.navigateTo({
					url: `/registration/doctor_schedule/doctor_schedule?deptSubId=${this.deptSubId}&doctorId=${this.doctorId}&date=${this.date}`
				});
				return;
			}
			uni.navigateTo({
				url: '/registration/notice/notice'
			});
		},
		goEvaluation: function() {
			uni.navigateTo({
				url: `/display/doctor_evaluation/doctor_evaluation?doctorId=${this.doctorId}`
			});
		}
	},
	onLoad: function(options) {
		this.doctorId = Number(options.doctorId || 0);
		this.deptSubId = options.deptSubId || null;
		this.date = options.date || '';
		if (this.doctorId > 0) {
			this.loadDoctorDetail();
		}
	}
};
</script>

<style lang="less">
@import url(doctor_detail.less);
</style>
