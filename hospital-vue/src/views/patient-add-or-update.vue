<template>
    <el-dialog
        :title="!dataForm.id ? '新增' : '修改'"
        v-if="isAuth(['ROOT', 'DOCTOR:INSERT', 'DOCTOR:UPDATE'])"
        :close-on-click-modal="false"
        v-model="visible"
        width="480px"
    >
        <el-scrollbar>
            <el-form :model="dataForm" ref="dataForm" :rules="dataRule" label-width="120px">

                <el-form-item label="就就诊卡信息" prop="name"><el-input v-model="dataForm.name" clearable /></el-form-item>
                <el-form-item label="患者性别" prop="name"><el-input v-model="dataForm.sex" clearable /></el-form-item>
                <el-form-item label="患者身份证" prop="name"><el-input v-model="dataForm.pid" clearable /></el-form-item>
                <el-form-item label="患者电话" prop="name"><el-input v-model="dataForm.tel" clearable /></el-form-item>
                <el-form-item label="患者生日" prop="name"><el-input v-model="dataForm.birthday" clearable /></el-form-item>
                <el-form-item label="就诊历史" prop="name"><el-input v-model="dataForm.medicalHistory" clearable /></el-form-item>
                <el-form-item label="患者医保类型" prop="name"><el-input v-model="dataForm.insuranceType" clearable /></el-form-item>



            </el-form>
        </el-scrollbar>
        <template #footer>
            <span class="dialog-footer">
                <el-button @click="visible = false">取消</el-button>
                <el-button type="primary" @click="dataFormSubmit">确定</el-button>
            </span>
        </template>
    </el-dialog>
</template>

<script>
import dayjs from 'dayjs';
export default {
    data: function() {
        return {
            visible: false,
            newTag: null,
            dept: [],

            dataForm: {
              id: null,
              name: null,
              sex: null,
              pid: null,
              tel: null,
              birthday: null,
              medicalHistory: null,
              insuranceType: null,
 
            },
            dataRule: {
                name: [
                    { required: true, message: '姓名不能为空' },
                    {
                        pattern: '^[\\u4e00-\\u9fa5]{2,20}$',
                        message: '姓名格式错误'
                    }
                ],

            }
        };
    },
    methods: {
        loadDeptAndSub: function() {
            let that = this;
            that.$http('/medical/dept/searchDeptAndSub', 'GET', {}, false, function(resp) {
                let result = resp.result;
                let dept = [];
                for (let one in result) {
                    let array = [];
                    for (let sub of result[one]) {
                        array.push({
                            value: sub.subId,
                            label: sub.subName
                        });
                    }
                    dept.push({
                        value: one,
                        label: one,
                        children: array
                    });
                }
                that.dept = dept;
            });
        },
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
        init: function(id) {
            let that = this;
            //重置表单控件
            that.reset();
            //如果id是undefined，就对模型层id变量赋值为0
            that.dataForm.id = id || 0;
            //显示对话框
            that.visible = true;
          
            //DOM渲染操作要放在$nextTick函数中执行，例如加载数据
            that.$nextTick(() => {
                //清理前端验证结果
                that.$refs['dataForm'].resetFields();
                //加载二级列表数据

              that.$http(`/patient/searchUserInfoCard/${id}`, 'GET', {}, true, function(resp) {
                that.dataForm.name = resp.name;
                that.dataForm.sex = resp.sex;
                that.dataForm.pid = resp.pid;
                that.dataForm.tel = resp.tel;
                that.dataForm.birthday = resp.birthday;

                that.dataForm.medicalHistory = resp.medicalHistory;
                that.dataForm.insuranceType = resp.insuranceType;

              });


                  });
              },
        inputTagHandle: function() {
            if (this.newTag != null && this.newTag != '') {
                if (this.dataForm.tag.includes(this.newTag)) {
                    ElMessage({
                        message: '不能添加重复标签',
                        type: 'warning',
                        duration: 1200
                    });
                } else {
                    this.dataForm.tag.push(this.newTag);
                    this.newTag = null;
                }
            }
        },
        closeTagHandle: function(tag) {
            let i = this.dataForm.tag.indexOf(tag);
            this.dataForm.tag.splice(i, 1);
        },
        dataFormSubmit: function() {
            let that = this;
          console.log(11)
            // that.$refs['dataForm'].validate(function(valid) {
                // if (valid) {

                    let data = {
                        id: that.dataForm.id,
                        name: that.dataForm.name,
                      pid:  that.dataForm.pid,
                      tel:  that.dataForm.tel,
                      sex:  that.dataForm.sex,
                      birthday:  that.dataForm.birthday,
                      medicalHistory:  that.dataForm.medicalHistory,
                      insuranceType:  that.dataForm.insuranceType

                    };
                    that.$http(`/patient/${!that.dataForm.id ? 'insert' : 'update'}`, 'POST', data, true, function(
                        resp
                    ) {
                        ElMessage({
                            message: '操作成功',
                            type: 'success'
                        });
                        that.visible = false;
                        that.$emit('refreshDataList');
                    });
                // }
            // });
        }



    }
};
</script>

<style lang="less" scoped="scoped"></style>
