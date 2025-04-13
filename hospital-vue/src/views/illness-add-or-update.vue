<template>
    <el-dialog
        :title="!dataForm.id ? '新增' : '修改'"
        v-if="isAuth(['ROOT', 'DOCTOR:INSERT', 'DOCTOR:UPDATE'])"
        :close-on-click-modal="false"
        v-model="visible"
        width="480px"
    >
        <el-scrollbar height="500px">
            <el-form :model="dataForm" ref="dataForm" :rules="dataRule" label-width="80px">
                <el-form-item label="疾病名称" prop="name"><el-input v-model="dataForm.name" clearable /></el-form-item>

                <el-form-item label="病因" prop="cause">
                    <el-input
                        v-model="dataForm.cause"
                        type="textarea"
                        :rows="6"
                        style="width:100%"
                        maxlength="1000"
                        show-word-limit
                        clearable
                    />
                </el-form-item>
                <el-form-item label="症状" prop="symptom">
                    <el-input
                        v-model="dataForm.symptom"
                        type="textarea"
                        :rows="6"
                        style="width:100%"
                        maxlength="1000"
                        show-word-limit
                        clearable
                    />
                </el-form-item>
                <el-form-item label="治疗方法" prop="method">
                    <el-input
                        v-model="dataForm.method"
                        type="textarea"
                        :rows="6"
                        style="width:100%"
                        maxlength="1000"
                        show-word-limit
                        clearable
                    />
                </el-form-item>
                <el-form-item label="描述" prop="description">
                    <el-input
                        v-model="dataForm.description"
                        type="textarea"
                        :rows="6"
                        style="width:100%"
                        maxlength="1000"
                        show-word-limit
                        clearable
                    />
                </el-form-item>

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
                cause: null,
                symptom: null,
                method: null,
                description: null,
 
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
                that.loadDeptAndSub();
                
                
               //加载医生详情信息 
              if (that.dataForm.id) {
                          that.$http('/illness/searchById', 'POST', { id: id }, true, function(resp) {
                   
                              that.dataForm.name = resp.name;
                              that.dataForm.cause = resp.cause;
                              that.dataForm.symptom = resp.symptom;
                              that.dataForm.method = resp.method;
                              that.dataForm.description = resp.description;
             
                          });
                      }
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
                        cause: that.dataForm.cause,
                        symptom: that.dataForm.symptom,
                        method: that.dataForm.method,
                        description: that.dataForm.description,

                    };
                    that.$http(`/illness/${!that.dataForm.id ? 'insert' : 'update'}`, 'POST', data, true, function(
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
