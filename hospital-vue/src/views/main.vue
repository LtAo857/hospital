<template>
  <div
    class="site-wrapper"
    :class="{ 'site-sidebar--fold': sidebarFold }"
    v-loading.fullscreen.lock="loading"
    element-loading-text="加载中"
  >
    <nav class="site-navbar" :class="'site-navbar--' + navbarLayoutType">
      <div class="site-navbar__header">
        <h1 class="site-navbar__brand">
          <a class="site-navbar__brand-lg">智慧医疗挂号系统</a>
          <a class="site-navbar__brand-mini">医院</a>
        </h1>
      </div>
      <div class="navbar-container" :class="'navbar-container--' + navbarLayoutType">
        <div class="switch" @click="handleSwitch"><SvgIcon name="zhedie" class="icon-svg" /></div>
        <div class="right-container">
          <div class="message">
            <el-badge value="0"><SvgIcon name="duanxin" class="icon-svg duanxin-svg" /></el-badge>
          </div>
          <el-dropdown>
            <span class="el-dropdown-link">{{ displayName }}</span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="updatePasswordHandle()">修改密码</el-dropdown-item>
                <el-dropdown-item @click="logout">退出</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
      <update-password v-if="updatePasswordVisible" ref="updatePassword"></update-password>
    </nav>

    <aside class="site-sidebar site-sidebar--dark">
      <div class="site-sidebar__inner">
        <el-menu
          :default-active="menuActiveName || 'Home'"
          :collapse="sidebarFold"
          :collapse-transition="false"
          class="site-sidebar__menu"
          background-color="#263238"
          active-text-color="#fff"
          text-color="#8a979e"
        >
          <el-menu-item index="Home" @click="$router.push({ name: 'Home' })">
            <SvgIcon name="home" class="icon-svg" />
            <span>首页</span>
          </el-menu-item>

          <el-sub-menu v-if="!isDoctorUser()" index="组织管理" :popper-class="'site-sidebar--' + sidebarLayoutSkin + '-popper'">
            <template #title>
              <SvgIcon name="users_fill" class="icon-svg" />
              <span>组织管理</span>
            </template>
            <el-menu-item
              v-if="isAuth(['ROOT', 'MEDICAL_DEPT:SELECT'])"
              index="医疗科室管理"
              @click="$router.push({ name: 'MedicalDept' })"
            >
              <SvgIcon name="company_fill" class="icon-svg" />
              <span>医疗科室管理</span>
            </el-menu-item>
            <el-menu-item
              v-if="isAuth(['ROOT', 'MEDICAL_DEPT_SUB:SELECT'])"
              index="医疗诊室管理"
              @click="$router.push({ name: 'MedicalDeptSub' })"
            >
              <SvgIcon name="company_fill" class="icon-svg" />
              <span>医疗诊室管理</span>
            </el-menu-item>
          </el-sub-menu>

          <el-sub-menu v-if="!isDoctorUser()" index="医护管理" :popper-class="'site-sidebar--' + sidebarLayoutSkin + '-popper'">
            <template #title>
              <SvgIcon name="trust_fill" class="icon-svg" />
              <span>医护管理</span>
            </template>
            <el-menu-item v-if="isAuth(['ROOT', 'DOCTOR:SELECT'])" index="医生管理" @click="$router.push({ name: 'Doctor' })">
              <SvgIcon name="doctor_fill" class="icon-svg" />
              <span>医生管理</span>
            </el-menu-item>
            <el-menu-item
              v-if="isAuth(['ROOT', 'DOCTOR_PRICE:SELECT'])"
              index="诊费设置"
              @click="$router.push({ name: 'DoctorPrice' })"
            >
              <SvgIcon name="money_fill" class="icon-svg" />
              <span>诊费设置</span>
            </el-menu-item>
          </el-sub-menu>

          <el-sub-menu
            v-if="isAuth(['ROOT', 'SCHEDULE:SELECT']) || isAuth(['ROOT', 'VIDEO_DIAGNOSE:DIAGNOSE']) || canViewEvaluation()"
            index="出诊管理"
            :popper-class="'site-sidebar--' + sidebarLayoutSkin + '-popper'"
          >
            <template #title>
              <SvgIcon name="night_fill" class="icon-svg" />
              <span>出诊管理</span>
            </template>
            <el-menu-item
              v-if="!isDoctorUser() && isAuth(['ROOT', 'SCHEDULE:SELECT'])"
              index="门诊日程表"
              @click="$router.push({ name: 'MedicalDeptSubWorkPlan' })"
            >
              <SvgIcon name="calendar_fill" class="icon-svg" />
              <span>门诊日程表</span>
            </el-menu-item>
            <el-menu-item
              v-if="isAuth(['ROOT', 'SCHEDULE:SELECT'])"
              index="医生出诊表"
              @click="
                $router.push({
                  name: 'DoctorSchedule',
                  params: { deptName: 'NAN', deptSubId: 'NAN', date: 'NAN' }
                })
              "
            >
              <SvgIcon name="clock_fill" class="icon-svg" />
              <span>医生出诊表</span>
            </el-menu-item>
            <el-menu-item
              v-if="isAuth(['ROOT', 'VIDEO_DIAGNOSE:DIAGNOSE'])"
              index="视频问诊"
              @click="$router.push({ name: 'VideoDiagnose' })"
            >
              <SvgIcon name="camera_fill" class="icon-svg" />
              <span>视频问诊</span>
            </el-menu-item>
            <el-menu-item v-if="canViewEvaluation()" index="评价管理" @click="$router.push({ name: 'Evaluation' })">
              <SvgIcon name="camera_fill" class="icon-svg" />
              <span>评价管理</span>
            </el-menu-item>
          </el-sub-menu>

          <el-sub-menu v-if="!isDoctorUser()" index="疾病管理" :popper-class="'site-sidebar--' + sidebarLayoutSkin + '-popper'">
            <template #title>
              <SvgIcon name="night_fill" class="icon-svg" />
              <span>疾病管理</span>
            </template>
            <el-menu-item index="疾病百科" @click="$router.push({ name: 'Illness' })">
              <SvgIcon name="camera_fill" class="icon-svg" />
              <span>疾病百科</span>
            </el-menu-item>
          </el-sub-menu>

          <el-sub-menu v-if="!isDoctorUser()" index="患者管理" :popper-class="'site-sidebar--' + sidebarLayoutSkin + '-popper'">
            <template #title>
              <SvgIcon name="night_fill" class="icon-svg" />
              <span>患者管理</span>
            </template>
            <el-menu-item index="患者信息" @click="$router.push({ name: 'Patient' })">
              <SvgIcon name="camera_fill" class="icon-svg" />
              <span>患者信息</span>
            </el-menu-item>
          </el-sub-menu>
        </el-menu>
      </div>
    </aside>

    <div class="site-content__wrapper" :style="{ minHeight: documentClientHeight + 'px' }">
      <main class="site-content" :class="{ 'site-content--tabs': $route.meta.isTab }">
        <el-tabs
          v-if="$route.meta.isTab"
          v-model="mainTabsActiveName"
          :closable="true"
          @tab-click="selectedTabHandle"
          @tab-remove="removeTabHandle"
        >
          <el-tab-pane v-for="item in mainTabs" :key="item.name" :label="item.title" :name="item.name">
            <el-card :body-style="siteContentViewHeight">
              <iframe
                v-if="item.type === 'iframe'"
                :src="item.iframeUrl"
                width="100%"
                height="100%"
                frameborder="0"
                scrolling="yes"
              ></iframe>
              <router-view v-if="item.name === mainTabsActiveName" />
            </el-card>
          </el-tab-pane>
        </el-tabs>
        <el-card v-else :body-style="siteContentViewHeight"><router-view /></el-card>
      </main>
    </div>
  </div>
</template>

<script>
import { provide } from 'vue';
import SvgIcon from '../components/SvgIcon.vue';
import { isURL } from '../utils/validate';
import UpdatePassword from './update-password.vue';

export default {
  components: { SvgIcon, UpdatePassword },
  data() {
    return {
      loading: false,
      navbarLayoutType: '',
      sidebarFold: false,
      sidebarLayoutSkin: 'dark',
      name: '',
      job: '',
      refId: null,
      documentClientHeight: 0,
      siteContentViewHeight: {},
      height: null,
      mainTabs: [],
      mainTabsActiveName: 'dept',
      menuActiveName: '',
      updatePasswordVisible: false
    };
  },
  computed: {
    displayName() {
      if (this.isDoctorUser()) {
        return this.name ? `${this.name}(医生)` : '医生';
      }
      return this.name ? `${this.name}(超级管理员)` : '超级管理员';
    }
  },
  created() {
    this.loadCurrentUser();
    this.routeHandle(this.$route);
    this.$options.sockets.onopen = () => {
      this.$socket.sendObj({ opt: 'connected', token: localStorage.getItem('token') });
      setInterval(() => {
        this.$socket.sendObj({ opt: 'ping', token: localStorage.getItem('token') });
      }, 10 * 1000);
    };
  },
  watch: {
    $route: {
      handler(to, from) {
        if (to.path !== from.path) {
          this.routeHandle(to);
        }
      }
    }
  },
  methods: {
    loadCurrentUser() {
      try {
        const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
        this.name = userInfo.name || '';
        this.job = userInfo.job || '';
        this.refId = userInfo.refId || null;
      } catch (e) {
        this.name = '';
        this.job = '';
        this.refId = null;
      }
    },
    isDoctorUser() {
      return this.job === '医生' && !!this.refId;
    },
    canViewEvaluation() {
      return this.isDoctorUser() || this.isAuth(['ROOT', 'EVALUATION:SELECT']);
    },
    handleSwitch() {
      this.navbarLayoutType = this.sidebarFold ? '' : 'fold';
      this.sidebarFold = !this.sidebarFold;
    },
    resetDocumentClientHeight() {
      this.documentClientHeight = document.documentElement.clientHeight;
      window.onresize = () => {
        this.documentClientHeight = document.documentElement.clientHeight;
        this.loadSiteContentViewHeight();
      };
    },
    loadSiteContentViewHeight() {
      let height = this.documentClientHeight - 50 - 30 - 2;
      if (this.$route.meta.isTab) {
        height -= 40;
        this.siteContentViewHeight = isURL(this.$route.meta.iframeUrl)
          ? { height: height + 'px' }
          : { minHeight: height + 'px' };
        this.height = provide('height', { height: height - 40 + 'px' });
      }
      this.siteContentViewHeight = { minHeight: height + 'px' };
    },
    routeHandle(route) {
      this.resetDocumentClientHeight();
      this.loadSiteContentViewHeight();

      if (!route.meta.isTab) {
        return;
      }
      let tab = this.mainTabs.filter(item => item.name === route.name)[0];
      if (!tab) {
        tab = {
          menuId: route.meta.menuId || route.name,
          name: route.name,
          title: route.meta.title,
          type: isURL(route.meta.iframeUrl) ? 'iframe' : 'module',
          iframeUrl: route.meta.iframeUrl || '',
          params: route.params,
          query: route.query
        };
        this.mainTabs = this.mainTabs.concat(tab);
      }
      this.menuActiveName = tab.menuId + '';
      this.mainTabsActiveName = tab.name;
    },
    logout() {
      this.$http('/mis_user/logout', 'GET', null, true, () => {
        localStorage.removeItem('permissions');
        localStorage.removeItem('token');
        localStorage.removeItem('userInfo');
        this.$router.push({ name: 'Login' });
      });
    },
    updatePasswordHandle() {
      this.updatePasswordVisible = true;
      this.$nextTick(() => {
        this.$refs.updatePassword.init();
      });
    },
    selectedTabHandle(tab) {
      const current = this.mainTabs.filter(item => item.name === tab.paneName);
      if (current.length >= 1) {
        this.$router.push({
          name: current[0].name,
          query: current[0].query,
          params: current[0].params
        });
      }
    },
    removeTabHandle(tabName) {
      this.mainTabs = this.mainTabs.filter(item => item.name !== tabName);
      if (this.mainTabs.length < 1) {
        this.menuActiveName = '';
        this.$router.push({ name: 'Home' });
        return;
      }
      if (tabName === this.mainTabsActiveName) {
        const tab = this.mainTabs[this.mainTabs.length - 1];
        this.$router.push({ name: tab.name, query: tab.query, params: tab.params }, () => {
          this.mainTabsActiveName = this.$route.name;
        });
      }
    }
  },
  mounted() {
    this.resetDocumentClientHeight();
    this.loadSiteContentViewHeight();
  }
};
</script>

<style lang="scss">
@import '../assets/scss/index.scss';
</style>
