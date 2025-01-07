<template>
	<view class="doctor-container">
		<view class="title-row">
			<!-- <text class="title">科室展示</text> -->
			<!-- <u-icon label="更多" labelPos="left" size="15" name="arrow-right" @click="navigatorHandle('doctor')"></u-icon> -->
		</view>
		<u-tabs :list="tab.list" @click="clickTabHandle"></u-tabs>
		<view class="panel">
			<view class="doctor" v-for="one in dept">
				<!-- <u-avatar :src="one.photo" size="45"></u-avatar> -->
				<view class="info">
					<view class="row">
						<text class="name">{{ one.name }}</text>
						<!-- <text class="job">{{ one.job }}</text> -->
						<!-- <button class="btn">挂号</button> -->
					</view>
					<view class="row">
						<!-- <text class="remark">{{ one.remark }}</text> -->
					</view>
					<view class="row">
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
				dept:[]
			}
		},
		onShow() {
			this.getAllDeptList()
		},
		methods:{
			getAllDeptList:function(){
				let that = this;
				let data = {
				    page: that.page,
				    length: that.length
				};
				that.ajax(
				    that.api.searchMedicalDeptList,
				    'POST',
				    data,
				    function(resp) {
               let list = resp.data;
			   that.dept=list.result;
	
				    },
				    false
				);
			},
		}
	}
</script>

<style lang="less">
@import url(allDept.less);
</style>
