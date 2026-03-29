<template>
  <div v-if="canViewEvaluation()">
    <el-form :inline="true" :model="dataForm" ref="dataForm">
      <el-form-item v-if="!isDoctorUser()">
        <el-input v-model="dataForm.doctorName" placeholder="医生姓名" clearable class="input" />
      </el-form-item>
      <el-form-item>
        <el-input v-model="dataForm.patientName" placeholder="患者姓名" clearable class="input" />
      </el-form-item>
      <el-form-item>
        <el-select v-model="dataForm.score" placeholder="评分" clearable class="input">
          <el-option v-for="score in [1, 2, 3, 4, 5]" :key="score" :label="`${score}分`" :value="score" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-select v-model="dataForm.source" placeholder="来源" clearable class="input">
          <el-option label="门诊" value="门诊" />
          <el-option label="视频问诊" value="视频问诊" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="searchHandle">查询</el-button>
        <el-button @click="resetHandle">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="dataList" border v-loading="dataListLoading" :cell-style="{ padding: '3px 0' }" style="width: 100%">
      <el-table-column type="index" header-align="center" align="center" width="100" label="序号">
        <template #default="scope">
          <span>{{ (pageIndex - 1) * pageSize + scope.$index + 1 }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="doctorName" header-align="center" align="center" label="医生" min-width="120" show-overflow-tooltip />
      <el-table-column prop="patientName" header-align="center" align="center" label="患者" min-width="120" show-overflow-tooltip />
      <el-table-column prop="score" header-align="center" align="center" label="评分" min-width="80">
        <template #default="scope">
          <el-tag type="warning">{{ scope.row.score }}分</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="source" header-align="center" align="center" label="来源" min-width="100" />
      <el-table-column prop="comment" header-align="center" align="center" label="评价内容" min-width="320" show-overflow-tooltip />
      <el-table-column prop="createTime" header-align="center" align="center" label="评价时间" min-width="160" />
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
      dataForm: {
        doctorName: null,
        patientName: null,
        score: null,
        source: null,
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
    canViewEvaluation() {
      return this.isDoctorUser() || this.isAuth(['ROOT', 'EVALUATION:SELECT']);
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
      if (!this.canViewEvaluation()) {
        return;
      }
      this.dataListLoading = true;
      const data = {
        doctorName: this.dataForm.doctorName || null,
        patientName: this.dataForm.patientName || null,
        score: this.dataForm.score,
        source: this.dataForm.source || null,
        startDate: this.dataForm.startDate || null,
        endDate: this.dataForm.endDate || null,
        page: this.pageIndex,
        length: this.pageSize
      };
      this.$http('/evaluation/searchByPage', 'POST', data, true, resp => {
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
        score: null,
        source: null,
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
