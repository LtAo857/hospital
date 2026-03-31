<template>
	<view>
		<view class="header-container">
			<view class="dept_sub">
				<text @click="showDeptSubPickerHandler">{{ deptSub }}</text>
				<u-icon name="arrow-down-fill" color="#b5b5b5" size="14"></u-icon>
			</view>
			<view class="job">
				<text @click="showJobPickerHandler">{{ job }}</text>
				<u-icon name="arrow-down-fill" color="#b5b5b5" size="14"></u-icon>
			</view>
		</view>
		<view class="doctor-container">
			<view class="doctor" v-for="one in doctor">
				<u-avatar :src="one.photo" size="45"></u-avatar>
				<view class="info">
					<view class="row">
						<text class="name">{{ one.name }}</text>
						<text class="job">{{ one.job }}</text>
						<button class="btn" @tap="createVideoDiagnoseHandle(one.doctorId)">问诊</button>
					</view>
					<view class="row">
						<text class="remark">{{ one.remark }}</text>
					</view>
					<view class="row">
						<rich-text class="desc">{{ one.description }}</rich-text>
					</view>
					<view class="row">
						<u-icon
							name="rmb-circle-fill"
							color="#ff0000"
							size="18"
							:label="one.price"
							labelColor="#ff0000"
							labelSize="18"
							top="1"
						></u-icon>

						<u-icon
							name="clock-fill"
							color="#a0a0a0"
							size="18"
							label="问诊15分钟"
							labelColor="#a0a0a0"
							labelSize="16"
							top="1"
						></u-icon>
					</view>
				</view>
			</view>
			<view v-if="doctor.length == 0">
				<u-empty mode="search" icon="http://cdn.uviewui.com/uview/empty/search.png"></u-empty>
			</view>
		</view>
		<u-picker
			:show="showDeptSubPicker"
			ref="DeptSubPicker"
			:columns="deptSubColumns"
			@confirm="confirmDeptSub"
			@change="changeDeptSub"
			@cancel="cancelDeptSub"
		></u-picker>

		<u-picker
			:show="showJobPicker"
			ref="JobPicker"
			:columns="jobColumns"
			@confirm="confirmJob"
			@cancel="cancelJob"
		></u-picker>
	</view>
</template>

<script>
export default {
	data() {
		return {
			deptSub: '全部诊室',
			showDeptSubPicker: false,
			deptSubColumns: [['全部诊室'], ['全部诊室']],
			deptSubColumnData: [['全部诊室']],

			job: '全部医师',
			showJobPicker: false,
			jobColumns: [['全部医师', '主任医师', '副主任医师', '主治医师', '副主治医师']],

			doctor: []
		};
	},
	methods: {
	    showDeptSubPickerHandler: function() {
	        this.showDeptSubPicker = true;
	    },

	    changeDeptSub(e) {
	        const {
	            columnIndex,
	            index,
	            picker = this.$refs.DeptSubPicker
	        } = e;
	        if (columnIndex === 0) {
	            picker.setColumnValues(1, this.deptSubColumnData[index]);
	        }
	    },
	    confirmDeptSub(e) {
	        this.showDeptSubPicker = false;
	        this.deptSub = e.value[1];
	        this.loadOnlineDoctorList();
	    },
	    cancelDeptSub: function() {
	        this.showDeptSubPicker = false;
	    },
	    showJobPickerHandler: function() {
	        this.showJobPicker = true;
	    },
	    confirmJob(e) {
	        this.showJobPicker = false;
	        this.job = e.value[0];
	        this.loadOnlineDoctorList();
	    },
	    cancelJob: function() {
	        this.showJobPicker = false;
	    },
	    loadOnlineDoctorList: function() {
	        let that = this;
	        let data = {
	            subName: that.deptSub == '全部诊室' ? null : that.deptSub,
	            job: that.job == '全部医师' ? null : that.job
	        };
	        that.ajax(that.api.searchOnlineDoctorList, 'POST', data, function(resp) {
	            let result = resp.data.result;
	            for (let one of result) {
	                one.photo = that.doctorPhotoUrl(one.photo);
	                one.price = one.price + '元';
	            }
	            that.doctor = result;
	        });
	    },
	    createVideoDiagnoseHandle: function() {
	        uni.showToast({
	            icon: 'none',
	            title: '暂未开放/功能维护中'
	        });
	    }
	},
	onLoad: function() {
	    this.loadOnlineDoctorList();
	}

};
</script>

<style lang="less">
@import url('doctor_list.less');
</style>
