<template>
  <el-dialog
    :title="mode === 'edit' ? '编辑医生账号' : '添加医生账号'"
    v-if="isAuth(['ROOT', 'DOCTOR:INSERT', 'DOCTOR:UPDATE'])"
    :close-on-click-modal="false"
    v-model="visible"
    width="480px"
  >
    <el-form :model="dataForm" ref="dataForm" :rules="dataRule" label-width="90px">
      <el-form-item label="姓名" prop="name">
        <el-input v-model="dataForm.name" disabled />
      </el-form-item>
      <el-form-item label="账号名称" prop="username">
        <el-input v-model="dataForm.username" maxlength="50" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input
          v-model="dataForm.password"
          maxlength="20"
          :placeholder="mode === 'edit' ? '不修改可留空' : '请输入初始密码'"
          show-password
        />
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
import { ElMessage } from 'element-plus';

export default {
  data() {
    const validateUsername = (rule, value, callback) => {
      if (!/^[a-zA-Z0-9]{5,50}$/.test(value || '')) {
        callback(new Error('账号名称应为5-50位字母或数字'));
        return;
      }
      callback();
    };
    const validatePassword = (rule, value, callback) => {
      if (this.mode === 'add' && !value) {
        callback(new Error('密码不能为空'));
        return;
      }
      if (value && !/^[a-zA-Z0-9]{6,20}$/.test(value)) {
        callback(new Error('密码应为6-20位字母或数字'));
        return;
      }
      callback();
    };
    return {
      visible: false,
      mode: 'add',
      dataForm: {
        accountId: null,
        id: null,
        name: '',
        username: '',
        password: '',
        sex: '',
        tel: '',
        email: '',
        deptId: null
      },
      dataRule: {
        username: [{ required: true, validator: validateUsername, trigger: 'blur' }],
        password: [{ validator: validatePassword, trigger: 'blur' }]
      }
    };
  },
  methods: {
    reset() {
      this.mode = 'add';
      this.dataForm = {
        accountId: null,
        id: null,
        name: '',
        username: '',
        password: '',
        sex: '',
        tel: '',
        email: '',
        deptId: null
      };
    },
    init(row) {
      const that = this;
      that.reset();
      const doctorId = row.id;
      that.$http('/doctor/account/searchAccountByRefId', 'POST', { id: doctorId }, true, function(resp) {
        const account = resp.result;
        that.$http('/doctor/account/searchById', 'POST', { id: doctorId }, true, function(detailResp) {
          const detail = detailResp;
          that.mode = account ? 'edit' : 'add';
          that.dataForm = {
            accountId: account ? account.id : null,
            id: detail.id,
            name: detail.name,
            username: account ? account.username : `doctor${detail.id}`,
            password: account ? '' : 'abc123456',
            sex: detail.sex,
            tel: detail.tel,
            email: detail.email || `doctor${detail.id}@hospital.local`,
            deptId: detail.deptId
          };
          if (account) {
            ElMessage({
              message: `该医生已设置账号，已切换为编辑模式：${account.username}`,
              type: 'warning',
              duration: 1500
            });
          }
          that.visible = true;
        });
      });
    },
    dataFormSubmit() {
      const that = this;
      that.$refs['dataForm'].validate(function(valid) {
        if (!valid) {
          return;
        }
        if (that.mode === 'edit') {
          that.$http(
            '/doctor/account/updateAccount',
            'POST',
            {
              id: that.dataForm.accountId,
              username: that.dataForm.username,
              password: that.dataForm.password
            },
            true,
            function() {
              ElMessage({
                message: '账号已更新',
                type: 'success',
                duration: 1200
              });
              that.visible = false;
              that.$emit('refreshDataList');
            }
          );
          return;
        }

        const data = {
          name: that.dataForm.name,
          username: that.dataForm.username,
          password: that.dataForm.password,
          sex: that.dataForm.sex,
          tel: that.dataForm.tel,
          email: that.dataForm.email,
          job: '医生',
          dept_id: that.dataForm.deptId,
          ref_id: that.dataForm.id,
          status: 1
        };
        that.$http('/doctor/account/insert', 'POST', data, true, function() {
          ElMessage({
            message: '账号已创建',
            type: 'success',
            duration: 1200
          });
          that.visible = false;
          that.$emit('refreshDataList');
        });
      });
    }
  }
};
</script>

<style lang="less" scoped="scoped"></style>
