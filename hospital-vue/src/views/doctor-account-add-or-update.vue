<template>
<!--    :title="!dataForm.id ? '新增' : '修改'"  -->
  <el-dialog
      title="添加医生账号"
      v-if="isAuth(['ROOT', 'DOCTOR:INSERT', 'DOCTOR:UPDATE'])"
      :close-on-click-modal="false"
      v-model="visible"
      width="480px"
  >
    <el-scrollbar height="500px">
      <el-form :model="dataForm" ref="dataForm" :rules="dataRule" label-width="80px">
        <el-form-item label="姓名" prop="name"><el-input  disabled v-model="dataForm.name" clearable /></el-form-item>
        <el-form-item label="账号名称" disabled prop="username"><el-input v-model="dataForm.username" clearable /></el-form-item>
        <el-form-item label="密码" disabled prop="password"><el-input v-model="dataForm.password" clearable /></el-form-item>


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
      },
      dataRule: {
        username: [
          { required: true, message: '账号名称不能为空' },

        ],


        password: [
          {
            required: true,
            message: '密码不能为空'
          }
        ]
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
    init: function(row) {
      console.log(row)
      let that = this;
      //重置表单控件
      that.reset();
      // //如果id是undefined，就对模型层id变量赋值为0
      // that.dataForm.id = row.id || 0;
      //显示对话框
      that.visible = true;

      //数据回显
      that.dataForm=row;


    },
    inputTagHandle: function () {
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
    closeTagHandle: function (tag) {
      let i = this.dataForm.tag.indexOf(tag);
      this.dataForm.tag.splice(i, 1);
    },
    dataFormSubmit: function () {
      let that = this;
      that.$refs['dataForm'].validate(function (valid) {
        if (valid) {
          let json = { 在职: 1, 离职: 2, 退休: 3 };
          let data = {
            username: that.dataForm.username,
            password: that.dataForm.password,
            name: that.dataForm.name,
            sex: that.dataForm.sex,
            tel: that.dataForm.tel,
            dept_id:that.dataForm.deptId,
            address: that.dataForm.address,
            email: "110@qq.com",
            // job: that.dataForm.job,
            status: json[that.dataForm.status],
          };
          // that.$http(`/doctor/account/${!that.dataForm.id ? 'insert' : 'update'}`, 'POST', data, true, function (
          that.$http(`/doctor/account/insert`, 'POST', data, true, function (
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
