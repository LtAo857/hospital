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
                pid: [
                    {
                        required: true,
                        message: '身份证号不能为空'
                    },
                    {
                        pattern:
                            '^[1-9]\\d{5}(18|19|([23]\\d))\\d{2}((0[1-9])|(10|11|12))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$',
                        message: '身份证号不正确'
                    }
                ],
                birthday: [
                    {
                        required: true,
                        message: '出生日期不能为空'
                    }
                ],
                school: [
                    {
                        required: true,
                        message: '毕业学校不能为空'
                    }
                ],
                tel: [
                    { required: true, message: '电话不能为空' },
                    {
                        pattern: '^1[1-9][0-9]{9}$',
                        message: '电话格式错误'
                    }
                ],
                address: [
                    {
                        required: true,
                        message: '家庭住址不能为空'
                    }
                ],
                email: [
                    {
                        required: true,
                        message: '电子信箱不能为空'
                    },
                    {
                        pattern: '^([a-zA-Z]|[0-9])(\\w|\\-)+@[a-zA-Z0-9]+\\.([a-zA-Z]{2,4})$',
                        message: '电子信箱格式错误'
                    }
                ],
                job: [
                    {
                        required: true,
                        message: '职务不能为空'
                    }
                ],
                deptSub: [
                    {
                        required: true,
                        message: '科室部门不能为空'
                    }
                ],
                remark: [
                    {
                        required: true,
                        message: '备注信息不能为空'
                    }
                ],
                description: [
                    {
                        required: true,
                        message: '医师介绍不能为空'
                    }
                ],
                hiredate: [{ required: true, message: '入职日期不能为空' }]
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
            that.$refs['dataForm'].validate(function(valid) {
                if (valid) {
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
                }
            });
        }



    }
};
</script>

<style lang="less" scoped="scoped"></style>
