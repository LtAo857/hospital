<template>
	<div v-if="isAuth(['ROOT', 'DOCTOR_PRICE:SELECT'])">
		<el-form :inline="true" :model="dataForm" :rules="dataRule" ref="dataForm">
				<!--
			<el-form-item prop="name">
				<el-input v-model="dataForm.name" placeholder="姓名" class="input" clearable="clearable" />
			</el-form-item>
		 <el-form-item>
				<el-select
					v-model="dataForm.deptId"
					class="input"
					placeholder="科室"
					size="medium"
					clearable="clearable"
				>
					<el-option v-for="one in medicalDeptList" :label="one.name" :value="one.id" />
				</el-select>
			</el-form-item> 
			<el-form-item>
				<el-select v-model="dataForm.job" class="input" placeholder="职务" size="medium" clearable="clearable">
					<el-option label="主任医师" value="主任医师" />
					<el-option label="副主任医师" value="副主任医师" />
					<el-option label="主治医师" value="主治医师" />
					<el-option label="副主治医师" value="副主治医师" />
				</el-select>
			</el-form-item>
			-->
			<!-- <el-form-item><el-button type="primary" @click="searchHandle()">查询</el-button></el-form-item> -->
			<el-form-item><el-button type="primary" @click="add()">添加</el-button></el-form-item>
			<!-- <div style="float: right">
				<el-radio-group v-model="dataForm.status" @change="searchHandle()">
					<el-radio-button label="在职" />
					<el-radio-button label="离职" />
					<el-radio-button label="退休" />
				</el-radio-group>
			</div> -->
		</el-form>
		<el-table
			:data="dataList"
			border
			v-loading="dataListLoading"
			:cell-style="{ padding: '3px 0' }"
			style="width: 100%;"
			@sort-change="orderHandle"
		>
			<el-table-column type="index" header-align="center" align="center" width="100" label="序号">
				<template #default="scope">
					<span>{{ (pageIndex - 1) * pageSize + scope.$index + 1 }}</span>
				</template>
			</el-table-column>
			<el-table-column
				prop="doctorName"
				header-align="center"
				align="center"
				label="医生姓名"
				min-width="120"
				:show-overflow-tooltip="true"
			/>
			<el-table-column prop="sex" header-align="center" align="center" label="性别" min-width="70" />
			<el-table-column prop="job" header-align="center" align="center" label="职务" min-width="100" />
			<el-table-column
				prop="deptName"
				header-align="center"
				align="center"
				label="科室名称"
				:show-overflow-tooltip="true"
				min-width="120"
				sortable
			/>
			<el-table-column
				prop="deptSubName"
				header-align="center"
				align="center"
				label="诊室名称"
				:show-overflow-tooltip="true"
				min-width="120"
			/>
			<el-table-column
				prop="price_1"
				header-align="center"
				align="center"
				label="门诊挂号价格"
				min-width="100"
				sortable
			/>
			<el-table-column
				prop="price_2"
				header-align="center"
				align="center"
				label="视频问诊价格"
				min-width="100"
				sortable
			/>
			<el-table-column header-align="center" align="center" width="150" label="操作">
				<template #default="scope">
					<el-button
						type="text"
						:disabled="!isAuth(['ROOT', 'DOCTOR_PRICE:UPDATE'])"
						@click="updateHandle(scope.row,scope.row.id, scope.row.price_1, scope.row.price_2, scope.row.job)"
					>
						调整价格
					</el-button>
				</template>
			</el-table-column>
		</el-table>
		<el-pagination
			@size-change="sizeChangeHandle"
			@current-change="currentChangeHandle"
			:current-page="pageIndex"
			:page-sizes="[10, 20, 50]"
			:page-size="pageSize"
			:total="totalCount"
			layout="total, sizes, prev, pager, next, jumper"
		></el-pagination>

		<update ref="update" @refreshDataList="loadDataList"></update>
	</div>
</template>

<script>
import Update from './doctor_price-update.vue';
export default {
	components: {
		Update
	},
	data: function() {
		return {
			medicalDeptList: [],
			dataForm: {
				name: null,
				deptId: null,
				job: null,
				status: '在职',
				column: null,
				order: null
			},
			doctorList:[],
			dataList: [],
			pageIndex: 1,
			pageSize: 50,
			totalCount: 0,
			dataListLoading: false,
			dataListSelections: [],
			dataRule: {
				name: [{ required: false, pattern: '^[a-zA-Z0-9\u4e00-\u9fa5]{1,10}$', message: '部门名称格式错误' }]
			}
		};
	},
	methods: {
		add:function(){
			let that = this
			this.$nextTick(() => {
				// console.log(row);
				that.$refs.update.init(that.doctorList);
                  });
		},

		getALLDoctor:function(){
            let that = this;
            let data = {
                page: that.pageIndex,
                length: that.pageSize,
				status:1
            };
            that.$http('/doctor/searchByPage', 'POST', data, true, function(resp) {
                let result = resp.result;
				that.doctorList=result.list;
                // that.dataList = result.list;
                // that.totalCount = result.totalCount;
                // that.dataListLoading = false;
            });
		},
		      //创建封装查询分页记录的函数
        loadDataList: function() {
            let that = this;
            that.dataListLoading = true;
            let data = {
                page: that.pageIndex,
                length: that.pageSize,
            };
            that.$http('/price/searchByPage', 'POST', data, true, function(resp) {
				console.log(resp);
                let result = resp.result;
                that.totalCount = result.totalCount;
                that.dataList = result.list;
                that.dataListLoading = false;

            });
        },

		updateHandle: function(row,doctorId,price_1,price_2,job) {
			this.$nextTick(() => {
				// console.log(row);
                      this.$refs.update.init(doctorId,price_1,price_2,job);
                  });
        },
	},
	created: function() {
		this.loadDataList()
		this.getALLDoctor()
	}
};
</script>

<style></style>
