<template>
	<el-dialog
		title="修改"
		v-if="isAuth(['ROOT', 'DOCTOR_PRICE:UPDATE'])"
		:close-on-click-modal="false"
		v-model="visible"
		width="450px"
	>
		<el-form :model="dataForm" ref="dataForm" :rules="dataRule" label-width="100px">
            <el-form-item v-if="typeof(doctorList)==='object'" label="医生名称">
                <el-select
                    v-model="dataForm.doctorId"
                    class="input"
                    placeholder="医生"
                    size="medium"
                    clearable="clearable"
                >
                    <el-option v-for="one in doctorList" :label="one.name" :value="one.id" />
                </el-select>
			</el-form-item>

			<el-form-item label="门诊挂号费" prop="price_1">
				<el-input v-model="dataForm.price_1" style="width:100%" clearable />
			</el-form-item>
			<el-form-item label="视频问诊费" prop="price_2">
				<el-input v-model="dataForm.price_2" style="width:100%" clearable />
			</el-form-item>
		</el-form>
		<template #footer>
			<span class="dialog-footer">
				<el-button @click="visible = false">取消</el-button>
				<el-button type="primary" @click="dataFormSubmit">确定</el-button>
			</span>
		</template>
	</el-dialog>
</template>

<script>
import { number } from 'echarts';
export default {
	data: function() {
		return {
            doctorList:[],
			visible: false,
			dataForm: {
				doctorId: null,
				price_1: null,
				price_2: null,
				level: null
			},
			dataRule: {
				price_1: [
					{ required: true, message: '必须填写金额' },
					// { pattern: '^[1-9]\\d*\\.\\d{1,2}$|^0\\.\\d{1,2}$|^[1-9]\\d*$', message: '金额格式错误' }
				],
				price_2: [
					{ required: true, message: '必须填写金额' },
					// { pattern: '^[1-9]\\d*\\.\\d{1,2}$|^0\\.\\d{1,2}$|^[1-9]\\d*$', message: '金额格式错误' }
				]
			}
		};
	},

	methods: {
		reset: function() {
            let dataForm = {
                id: null,
                name: null,
                pid: null,
                sex: '男',
                photo: null,
                birthday: null,
                school: null,
                degree: '博士',
                tel: null,
                address: null,
                email: null,
                job: null,
                deptSub: null,
                deptSubId: null,
                remark: null,
                description: null,
                hiredate: null,
                tag: [],
                recommended: '普通',
                status: '在职'
            };   
            this.dataForm = dataForm;
            this.newTag = null;
        },

        init: function(doctorId,price_1,price_2,job) {
            let that = this;
            //重置表单控件
            that.reset();
            if(typeof(that.dataForm.id )==='object'){
                that.doctorList=doctorId
            }
            //如果id是undefined，就对模型层id变量赋值为0
            that.dataForm.id = doctorId || 0;
            //显示对话框
            that.visible = true;
            //DOM渲染操作要放在$nextTick函数中执行，例如加载数据
            that.$nextTick(() => {
                //清理前端验证结果
                that.$refs['dataForm'].resetFields();
                //加载二级列表数据
                // that.loadDeptAndSub();
                
                
               //加载医生详情信息 
              if (typeof(that.dataForm.id) === 'number') {
                          that.$http('/price/searchDoctorPriceById', 'POST', { doctorId: doctorId }, true, function(resp) {
                   
                              that.dataForm.price_1 = resp.price_1;
                              that.dataForm.price_2 = resp.price_2;
             
                          });
                      }
                  });
              },
			  dataFormSubmit: function() {
            let that = this;
            that.$refs['dataForm'].validate(function(valid) {
                console.log(that.dataForm);
                if (valid) {
                    let data = {
                        doctorId: typeof(that.dataForm.id)=='object'?that.dataForm.doctorId:that.dataForm. id,
                        price_1: that.dataForm.price_1*1.0,
                        price_2: that.dataForm.price_2*1.0,
                    };
                    that.$http(`/price/${typeof(that.dataForm.id)=='object'? 'insert' : 'update'}`, 'POST', data, true, function(
                        resp
                    ) {
                        ElMessage({
                            message: '操作成功',
                            type: 'success'
                        });
                        that.visible = false;
                        that.$emit('refreshDataList');
                    });
                }
            });
        }
	}
};
</script>

<style lang="less" scoped="scoped"></style>
