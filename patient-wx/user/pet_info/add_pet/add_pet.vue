<template>
	<view class="container">
		<view class="ui-all">
	<!-- 		<view class="avatar" @tap="avatarChoose">
				<view  class="imgAvatar">
					<view class="iavatar" :style="'background: url('+avater+') no-repeat center/cover #eeeeee;'"></view>
				</view>
				<text v-if="avater">修改头像</text>
 				<text v-if="!avater">授权微信</text>
				<button v-if="!avater" open-type="getUserInfo" @tap="getUserInfo" class="getInfo"></button> 
			</view>
			-->
			<view class="ui-list">
				<text>宠物昵称</text>
				<input type="text" :placeholder="value" :value="petInfo.name" @input="bindName" placeholder-class="place" />
			</view>
<!-- 			<view class="ui-list">
				<text>手机号</text>
				<input v-if="mobile" type="tel" :placeholder="value" :value="mobile" @input="bindmobile" placeholder-class="place" />
				<button v-if="!mobile" open-type="getPhoneNumber" @getphonenumber="getphonenumber" class="getInfo bun">授权手机号</button>
			</view> -->
			<view class="ui-list right">
				<text>性别</text>
				<picker @change="bindSexPickerChange" mode='selector' range-key="name" :value="petInfo.sex" :range="sex">
					<view class="picker">
						{{sex[index].name}}
					</view>
				</picker>
			</view>
<!-- 			<view class="ui-list right">
				<text>常住地</text>
				<picker @change="bindRegionChange" mode='region'>
					<view class="picker">
						{{region[0]}} {{region[1]}} {{region[2]}}
					</view>
				</picker>
			</view> -->
			<view class="ui-list right">
				<text>生日</text>
				<picker mode="date" :value="petInfo.birthday" @change="bindBirthdayChange">
					<view class="picker">
						{{petInfo.birthday}}
					</view>
				</picker>
			</view>

			<view class="ui-list">
				<text>品种</text>
				<input type="text" :placeholder="value" :value="petInfo.breed" @input="bindBreed" placeholder-class="place" />
			</view>
			<view class="ui-list">
				<text>体重（单位kg）</text>
				<input type="text" :placeholder="value" :value="petInfo.weight" @input="bindWeight" placeholder-class="place" />
			</view>
			<view class="ui-list">
				<text>爱好</text>
				<textarea :placeholder="value" placeholder-class="place" :value="petInfo.hobby" @input="bindHobby"></textarea>
			</view>
			<view class="ui-list">
				<text>疾病史</text>
				<textarea :placeholder="value" placeholder-class="place" :value="petInfo.disease_history" @input="bindDisease"></textarea>
			</view>
			<view class="ui-list">
				<text>疫苗情况</text>
				<textarea :placeholder="value" placeholder-class="place" :value="petInfo.vaccine" @input="bindVaccine"></textarea>
			</view>
			<button class="save"  style="background-color: red;" v-if="currentId!=='-1'"  @tap="deleteInfo">删除</button>
			<button class="save" @tap="savaInfo">保 存 修 改</button>
		</view>

	</view>
</template>

<script>
	export default {
	
		data() {
			return {
				currentId:'',
				value: '请填写',
				sex: [
					{
						id: 0,
						name: '母'
					},{
					id: 1,
					name: '公'
				}],
				index: 1,
				region: ['请填写'],
				date: '请填写',
				avater: '',
				description: '',
				url: '',
				name: '',
				mobile: '',
				petInfo:{
					name: '',
					sex:'',
					image: 'http://43.143.231.180:9010/hospital/animal.png',
					birthday: '2024-01-01',
					breed: '',
					weight: '',
					hobby: '',
					disease_history: '',
					vaccine: ''
				}

			}

		},
		methods: {
			bindBirthdayChange(e) {
				this.petInfo.birthday = e.detail.value;
				
			},
			bindSexPickerChange(e) {
				this.index=e.detail.value
				this.petInfo.sex = e.detail.value=='1'?'公':'母';
				
			},
			bindName(e) {
				this.petInfo.name =e.detail.value ;
				
			},
			bindWeight(e) {
				this.petInfo.weight = e.detail.value;
				
			},
			bindVaccine(e) {
				this.petInfo.vaccine = e.detail.value;
				
			},
			bindBreed(e) {
				this.petInfo.breed = e.detail.value;
				
			},
			bindDisease(e) {
				this.petInfo.disease_history = e.detail.value;
				
			},
			bindHobby(e) {
				this.petInfo.hobby = e.detail.value;
				
			},

			avatarChoose() {
				let that = this;
				uni.chooseImage({
					count: 1,
					sizeType: ['original', 'compressed'],
					sourceType: ['album', 'camera'],
					success(res) {
						// tempFilePath可以作为img标签的src属性显示图片
						that.imgUpload(res.tempFilePaths);
						const tempFilePaths = res.tempFilePaths;
					}
				});
			},
			 getUserInfo () {
				  uni.getUserProfile({
			      desc: '用于完善会员资料', // 声明获取用户个人信息后的用途，后续会展示在弹窗中，请谨慎填写
			      success: (res) => {
			       console.log(res);
				   uni.showToast({
							   title: '已授权',
							   icon: 'none',
							   duration: 2000
							   }) 
			      }
			    })
			    } ,
				 getphonenumber(e){
					if(e.detail.iv){
					  console.log(e.detail.iv) //传后台解密换取手机号
						  uni.showToast({
							   title: '已授权',
							   icon: 'none',
							   duration: 2000
							   }) 
					}
								  },
								  deleteInfo(){
									  let that = this;
									  
									  that.ajax(
									  that.api.deletePetInfo,
									  	    'post',
									      {ids:[that.currentId]},
									      function(resp) {
									  		console.log(resp);
									  		if(resp.data.code==200){
									  			uni.showToast({
									  				title: '删除成功',
									  				icon: 'none',
									  				duration: 2000
									  			});
												setTimeout(function() {
													uni.navigateBack({
													delta: 1
													});
												}, 2000)

									  		}
									      },
									      false
									  );
								  },
			savaInfo() {
				let that = this;
		
				if (!that.petInfo.name) {
					uni.showToast({
						title: '请填写昵称',
						icon: 'none',
						duration: 2000
					});
					return;
				}
				if (that.petInfo.birthday == "0000-00-00") {
					uni.showToast({
						title: '请选择生日',
						icon: 'none',
						duration: 2000
					});
					return;
				}
				

				if (!that.petInfo.breed) {
					uni.showToast({
						title: '请填写品种',
						icon: 'none',
						duration: 2000
					});
					return;
				}
				if (!that.petInfo.weight) {
					uni.showToast({
						title: '请填写体重',
						icon: 'none',
						duration: 2000
					});
			
					return;
				}
				that.petInfo.weight=that.petInfo.weight+'kg'
		that.ajax(
		that.petInfo.id?that.api.updatePetInfo:that.api.addPetInfo,
		    'post',
		    that.petInfo,
		    function(resp) {
				console.log(resp);
				if(resp.data.code==200){
					uni.showToast({
						title: that.petInfo.id?"修改成功":"添加成功",
						icon: 'none',
						duration: 2000
					});
					setTimeout(function() {
					uni.navigateTo({
					    url: '/user/pet_info/pet_info'
					});
					}, 2000)

				}
		    },
		    false
		);
			},
			isPoneAvailable(poneInput) {
				var myreg = /^[1][3,4,5,7,8][0-9]{9}$/;
				if (!myreg.test(poneInput)) {
					return false;
				} else {
					return true;
				}
			},
			async updata(datas) {
				//传后台
				
			},
			imgUpload(file) {
				let that = this;
				uni.uploadFile({
					header: {
						Authorization: uni.getStorageSync('token')
					},
					url:'/api/upload/image', //需传后台图片上传接口
					filePath: file[0],
					name: 'file',
					formData: {
						type: 'user_headimg'
					},
					success: function(res) {
						var data = JSON.parse(res.data);
						data = data.data;
						that.avater = that.url + data.img;

						that.headimg = that.url + data.img;

						
					},
					fail: function(error) {
						console.log(error);
					}
				});
			},
	
		},
		onLoad(e) {	
			console.log(e);
			let that = this;
			that.currentId=e.id
			if(e.id!=="-1"){
				//编辑
					that.petInfo.id=e.id
				that.ajax(
				    that.api.searchById,
				    'post',
				    {id:e.id},
				    function(resp) {
						console.log(resp);
						if(resp.statusCode==200){
							that.petInfo=resp.data;
							if(resp.data.sex=='母'){
								that.index=0
							}else{
								that.index=1
							}
						}
				    },
				    false
				);
			}else{
				that.petInfo.id=null
			}
		}

	}
</script>

<style lang="less">
	.container {
		display: block;
	}

	.ui-all {
		padding: 20rpx 40rpx;

		.avatar {
			width: 100%;
			text-align: left;
			padding: 20rpx 0;
			border-bottom: solid 1px #f2f2f2;
			position: relative;

			.imgAvatar {
				width: 140rpx;
				height: 140rpx;
				border-radius: 50%;
				display: inline-block;
				vertical-align: middle;
				overflow: hidden;

				.iavatar {
					width: 100%;
					height: 100%;
					display: block;
				}
			}

			text {
				display: inline-block;
				vertical-align: middle;
				color: #8e8e93;
				font-size: 28rpx;
				margin-left: 40rpx;
			}

			&:after {
				content: ' ';
				width: 20rpx;
				height: 20rpx;
				border-top: solid 1px #030303;
				border-right: solid 1px #030303;
				transform: rotate(45deg);
				-ms-transform: rotate(45deg);
				/* IE 9 */
				-moz-transform: rotate(45deg);
				/* Firefox */
				-webkit-transform: rotate(45deg);
				/* Safari 和 Chrome */
				-o-transform: rotate(45deg);
				position: absolute;
				top: 85rpx;
				right: 0;
			}
		}

		.ui-list {
			width: 100%;
			text-align: left;
			padding: 20rpx 0;
			border-bottom: solid 1px #f2f2f2;
			position: relative;

			text {
				color: #4a4a4a;
				font-size: 28rpx;
				display: inline-block;
				vertical-align: middle;
				min-width: 150rpx;
			}

			input {
				color: #030303;
				font-size: 30rpx;
				display: inline-block;
				vertical-align: middle;
			}
			button{
				color: #030303;
				font-size: 30rpx;
				display: inline-block;
				vertical-align: middle;
				background: none;
				margin: 0;
				padding: 0;
				&::after{
					display: none;
				}
			}
			picker {
				width: 90%;
				color: #030303;
				font-size: 30rpx;
				display: inline-block;
				vertical-align: middle;
				position: absolute;
				top: 30rpx;
				left: 150rpx;
			}

			textarea {
				color: #030303;
				font-size: 30rpx;
				vertical-align: middle;
				height: 150rpx;
				width: 100%;
				margin-top: 50rpx;
			}

			.place {
				color: #999999;
				font-size: 28rpx;
			}
		}

		.right:after {
			content: ' ';
			width: 20rpx;
			height: 20rpx;
			border-top: solid 1px #030303;
			border-right: solid 1px #030303;
			transform: rotate(45deg);
			-ms-transform: rotate(45deg);
			/* IE 9 */
			-moz-transform: rotate(45deg);
			/* Firefox */
			-webkit-transform: rotate(45deg);
			/* Safari 和 Chrome */
			-o-transform: rotate(45deg);
			position: absolute;
			top: 40rpx;
			right: 0;
		}

		.save {
			background: #030303;
			border: none;
			color: #ffffff;
			margin-top: 40rpx;
			font-size: 28rpx;
		}
	}
</style>
