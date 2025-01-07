<template>
	<view class="doctor-container">
		<view class="title-row">
		<!-- 	<text class="title">疾病百科</text> -->
			<!-- <u-icon label="更多" labelPos="left" size="15" name="arrow-right" @click="navigatorHandle('doctor')"></u-icon> -->
		</view>
		<view class="panel">
			<view class="doctor" v-for="one in illness">
				<!-- <u-avatar :src="one.photo" size="45"></u-avatar> -->
				<view class="info">
					<view class="row">
						<text class="name">{{ one.name }}</text>
						<!-- <text class="job">{{ one.job }}</text> -->
						<!-- <button class="btn">挂号</button> -->
					</view>
					<view class="row">
						<view class="title">
							<text class="title-text">原因:</text>
						</view>
                         
						<rich-text class="desc">{{ one.cause }}</rich-text>
					</view>
					<view class="row">
						
						<view class="title">
							<text class="title-text">症状:</text>
						</view>
						<rich-text class="desc">{{ one.symptom }}</rich-text>
					</view>
					<view class="row">
						<view class="title">
							<text class="title-text">治疗:</text>
						</view>
						
						<rich-text class="desc">{{ one.method }}</rich-text>
					</view>

					<view class="row">
						<view class="title">
							<text class="title-text">描述:</text>
						</view>
					
						<rich-text class="desc">{{ one.description }}</rich-text>
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
	export default{
		data(){
			return{
				page: 1,
				length: 50,
				illness:[]
			}
		},
		onShow() {
			this.getAllIllnessList()
		},
		methods:{
			getAllIllnessList:function(){
				let that = this;
				let data = {
				    page: that.page,
				    length: that.length
				};
				that.ajax(
				    that.api.searchIllnessList,
				    'POST',
				    data,
				    function(resp) {
		         let result = resp.data.result;
				that.illness=result.list
	
				    },
				    false
				);
			},
		}
	}
</script>

<style lang="less">
@import url(allInfo.less);
</style>
