<template>
	<view class="doctor-container">
		<view class="title-row">
			<button size="mini" type="primary"@click="addPet(-1)">+添加宠物</button>
		<!-- 	<text class="title">疾病百科</text> -->
			<!-- <u-icon label="更多" labelPos="left" size="15" name="arrow-right" @click="navigatorHandle('doctor')"></u-icon> -->
		</view>
		
		<u-tabs :list="tab.list" @click="clickTabHandle"></u-tabs>
		<view class="panel">
			<view class="doctor" v-for="one in illness">
				<u-avatar :src="one.image" size="45"></u-avatar>
				<view class="info">
					<view class="row">
						<text class="name">{{ one.name }}</text>
						<!-- <text class="job">{{ one.job }}</text> -->
						<button class="btn" @click="addPet(one.id)">编辑</button>
					</view>
					<view class="row">
						<view class="title">
							<text class="title-text">性别:</text>
						</view>
                         
						<rich-text class="desc">{{ one.sex}}</rich-text>
					</view>
					<view class="row">
						<view class="title">
							<text class="title-text">爱好:</text>
						</view>
						
						<rich-text class="desc">{{ one.hobby }}</rich-text>
					</view>

					<view class="row">
						<view class="title">
							<text class="title-text">生日:</text>
						</view>
					
						<rich-text class="desc">{{ one.birthday }}</rich-text>
					</view>

					<view class="row">
						<view class="title">
							<text class="title-text">品种:</text>
						</view>
					
						<rich-text class="desc">{{ one.breed }}</rich-text>
					</view>
					<view class="row">
						<view class="title">
							<text class="title-text">体重:</text>
						</view>
					
						<rich-text class="desc">{{ one.weight }}</rich-text>
					</view>
					<view class="row">
						<view class="title">
							<text class="title-text">疾病史:</text>
						</view>
					
						<rich-text class="desc">{{ one.		disease_history }}</rich-text>
					</view>
					<view class="row">
						<view class="title">
							<text class="title-text">疫苗:</text>
						</view>
					
						<rich-text class="desc">{{ one.		vaccine }}</rich-text>
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
				illness:[],
				image:'./static/animal.png'
			}
		},
		onShow() {
			this.getPetById()
		},
		methods:{
			getPetById:function(){
				let that = this;
				let data = {
				    page: that.page,
				    length: that.length
				};
				that.ajax(
				    that.api.searchByPageAndId,
				    'POST',
				    data,
				    function(resp) {
		         let result = resp.data.result;
				that.illness=result.list
	
				    }, 
				    false
				);
			},
			addPet:function(id){
				console.log('pet_info/add_pet/add_pet');
				uni.navigateTo({
				    url: `/user/pet_info/add_pet/add_pet?id=${id}`,
				});
			}
		}
	}
</script>

<style lang="less">
@import url(pet_info.less);
</style>
