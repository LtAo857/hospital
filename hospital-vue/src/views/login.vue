<template>
  <div class="page">
    <el-row type="flex" justify="center" align="middle" class="container">
      <el-col :lg="14" :xl="10" class="hidden-md-and-down">
        <el-row class="panel">
          <el-col :span="12">
            <div class="left">
              <img src="../assets/login/logo.png" class="logo" />
              <img src="../assets/login/big-1.png" class="big" />
            </div>
          </el-col>
          <el-col :span="12">
            <div class="right">
              <div class="title-container">
                <h2>智慧医疗挂号系统</h2>
                <span>V1.0</span>
              </div>
              <div>
                <div class="row">
                  <el-input
                    v-model="username"
                    placeholder="用户名"
                    prefix-icon="user"
                    size="large"
                    clearable
                  />
                </div>
                <div class="row">
                  <el-input
                    type="password"
                    v-model="password"
                    placeholder="密码"
                    prefix-icon="Lock"
                    size="large"
                    clearable
                    @keyup.enter="login"
                  />
                </div>
                <div class="row">
                  <el-button type="primary" class="btn" size="large" @click="login">
                    登录系统
                  </el-button>
                </div>
              </div>
            </div>
          </el-col>
        </el-row>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus';
import router from '../router/index.js';

export default {
  data() {
    return {
      username: 'admin',
      password: 'abc123456'
    };
  },
  methods: {
    login() {
      if (!this.username || !this.password) {
        ElMessage({
          message: '请输入用户名和密码',
          type: 'warning',
          duration: 1200
        });
        return;
      }
      const data = {
        username: this.username,
        password: this.password
      };
      this.$http('/mis_user/login', 'POST', data, true, function(resp) {
        if (!resp.result) {
          ElMessage({
            message: '登录失败',
            type: 'error',
            duration: 1200
          });
          return;
        }
        localStorage.setItem('permissions', JSON.stringify(resp.permissions || []));
        localStorage.setItem('token', resp.token);
        localStorage.setItem('userInfo', JSON.stringify(resp.userInfo || {}));
        router.push({ name: 'Home' });
      });
    }
  }
};
</script>

<style lang="less" scoped>
@import url('login.less');
</style>
