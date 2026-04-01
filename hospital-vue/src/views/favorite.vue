<template>
  <div v-if="canViewFavorite()">
    <el-form :inline="true" :model="dataForm" ref="dataForm">
      <el-form-item v-if="!isDoctorUser()">
        <el-input v-model="dataForm.doctorName" :placeholder="text.doctorName" clearable class="input" />
      </el-form-item>
      <el-form-item>
        <el-input v-model="dataForm.patientName" :placeholder="text.patientName" clearable class="input" />
      </el-form-item>
      <el-form-item>
        <el-input v-model="dataForm.patientTel" :placeholder="text.patientTel" clearable class="input" />
      </el-form-item>
      <el-form-item>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          :range-separator="text.rangeSeparator"
          :start-placeholder="text.startDate"
          :end-placeholder="text.endDate"
          value-format="YYYY-MM-DD"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="searchHandle">{{ text.search }}</el-button>
        <el-button @click="resetHandle">{{ text.reset }}</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="dataList" border v-loading="dataListLoading" :cell-style="{ padding: '3px 0' }" style="width: 100%">
      <el-table-column type="index" header-align="center" align="center" width="100" :label="text.index">
        <template #default="scope">
          <span>{{ (pageIndex - 1) * pageSize + scope.$index + 1 }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="doctorName" header-align="center" align="center" :label="text.doctor" min-width="120" show-overflow-tooltip />
      <el-table-column prop="job" header-align="center" align="center" :label="text.job" min-width="100" show-overflow-tooltip />
      <el-table-column prop="patientName" header-align="center" align="center" :label="text.patient" min-width="120" show-overflow-tooltip />
      <el-table-column prop="patientTel" header-align="center" align="center" :label="text.tel" min-width="140" />
      <el-table-column prop="createTime" header-align="center" align="center" :label="text.favoriteTime" min-width="160" />
    </el-table>

    <el-pagination
      @size-change="sizeChangeHandle"
      @current-change="currentChangeHandle"
      :current-page="pageIndex"
      :page-sizes="[10, 20, 50]"
      :page-size="pageSize"
      :total="totalCount"
      layout="total, sizes, prev, pager, next, jumper"
    />
  </div>
</template>

<script>
export default {
  data() {
    return {
      text: {
        doctorName: '\u533b\u751f\u59d3\u540d',
        patientName: '\u60a3\u8005\u59d3\u540d',
        patientTel: '\u60a3\u8005\u624b\u673a\u53f7',
        rangeSeparator: '\u81f3',
        startDate: '\u5f00\u59cb\u65e5\u671f',
        endDate: '\u7ed3\u675f\u65e5\u671f',
        search: '\u67e5\u8be2',
        reset: '\u91cd\u7f6e',
        index: '\u5e8f\u53f7',
        doctor: '\u533b\u751f',
        job: '\u804c\u79f0',
        patient: '\u60a3\u8005',
        tel: '\u8054\u7cfb\u7535\u8bdd',
        favoriteTime: '\u6536\u85cf\u65f6\u95f4'
      },
      dataForm: {
        doctorName: null,
        patientName: null,
        patientTel: null,
        startDate: null,
        endDate: null
      },
      dateRange: [],
      dataList: [],
      pageIndex: 1,
      pageSize: 10,
      totalCount: 0,
      dataListLoading: false
    };
  },
  methods: {
    canViewFavorite() {
      return this.isDoctorUser() || this.isAuth(['ROOT', 'FAVORITE:SELECT']);
    },
    isDoctorUser() {
      try {
        const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
        return userInfo.job === '医生' && !!userInfo.refId;
      } catch (e) {
        return false;
      }
    },
    loadDataList() {
      if (!this.canViewFavorite()) {
        return;
      }
      this.dataListLoading = true;
      const data = {
        doctorName: this.dataForm.doctorName || null,
        patientName: this.dataForm.patientName || null,
        patientTel: this.dataForm.patientTel || null,
        startDate: this.dataForm.startDate || null,
        endDate: this.dataForm.endDate || null,
        page: this.pageIndex,
        length: this.pageSize
      };
      this.$http('/favorite/searchByPage', 'POST', data, true, resp => {
        const result = resp.result;
        this.dataList = result.list;
        this.totalCount = result.totalCount;
        this.dataListLoading = false;
      });
    },
    searchHandle() {
      this.dataForm.startDate = this.dateRange && this.dateRange.length > 0 ? this.dateRange[0] : '';
      this.dataForm.endDate = this.dateRange && this.dateRange.length > 0 ? this.dateRange[1] : '';
      this.pageIndex = 1;
      this.loadDataList();
    },
    resetHandle() {
      this.dataForm = {
        doctorName: null,
        patientName: null,
        patientTel: null,
        startDate: null,
        endDate: null
      };
      this.dateRange = [];
      this.pageIndex = 1;
      this.loadDataList();
    },
    sizeChangeHandle(val) {
      this.pageSize = val;
      this.pageIndex = 1;
      this.loadDataList();
    },
    currentChangeHandle(val) {
      this.pageIndex = val;
      this.loadDataList();
    }
  },
  created() {
    this.loadDataList();
  }
};
</script>
