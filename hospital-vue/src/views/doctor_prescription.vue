<template>
  <div v-if="canViewPrescription()">
    <el-form :inline="true" :model="dataForm" ref="dataForm">
      <el-form-item v-if="!isDoctorUser()">
        <el-input v-model="dataForm.doctorName" placeholder="医生姓名" clearable class="input" />
      </el-form-item>
      <el-form-item>
        <el-input v-model="dataForm.patientName" placeholder="患者姓名" clearable class="input" />
      </el-form-item>
      <el-form-item>
        <el-input v-model="dataForm.subDeptName" placeholder="诊室名称" clearable class="input" />
      </el-form-item>
      <el-form-item>
        <el-select v-model="dataForm.hasPrescription" placeholder="处方状态" clearable class="input">
          <el-option label="未开方" :value="0" />
          <el-option label="已开方" :value="1" />
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
      <el-table-column prop="registrationId" header-align="center" align="center" label="挂号单号" min-width="120" />
      <el-table-column prop="date" header-align="center" align="center" label="就诊日期" min-width="160">
        <template #default="scope">
          <span>{{ scope.row.date }} {{ slotText(scope.row.slot) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="patientName" header-align="center" align="center" label="患者" min-width="120" show-overflow-tooltip />
      <el-table-column prop="doctorName" header-align="center" align="center" label="医生" min-width="120" show-overflow-tooltip />
      <el-table-column prop="subDeptName" header-align="center" align="center" label="诊室" min-width="140" show-overflow-tooltip />
      <el-table-column prop="paymentStatus" header-align="center" align="center" label="挂号状态" min-width="100">
        <template #default="scope">
          <el-tag :type="scope.row.paymentStatus === 2 ? 'success' : scope.row.paymentStatus === 1 ? 'warning' : 'info'">
            {{ scope.row.paymentStatus === 2 ? '已支付' : scope.row.paymentStatus === 1 ? '已挂号' : '未支付' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="hasPrescription" header-align="center" align="center" label="处方状态" min-width="100">
        <template #default="scope">
          <el-tag :type="scope.row.hasPrescription ? 'success' : 'warning'">
            {{ scope.row.hasPrescription ? '已开方' : '未开方' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column header-align="center" align="center" label="操作" width="180">
        <template #default="scope">
          <el-button type="text" size="medium" :disabled="!canEditPrescription()" @click="openDialog(scope.row)">
            {{ scope.row.hasPrescription ? '编辑处方' : '开方' }}
          </el-button>
        </template>
      </el-table-column>
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

    <add-or-update ref="addOrUpdate" @refreshDataList="loadDataList"></add-or-update>
  </div>
</template>

<script>
import AddOrUpdate from './doctor_prescription-add-or-update.vue';

export default {
  components: { AddOrUpdate },
  data() {
    return {
      dataForm: {
        doctorName: null,
        patientName: null,
        subDeptName: null,
        hasPrescription: null,
        startDate: null,
        endDate: null
      },
      dateRange: [],
      dataList: [],
      pageIndex: 1,
      pageSize: 10,
      totalCount: 0,
      dataListLoading: false,
      slotMap: {
        1: '08:00',
        2: '08:30',
        3: '09:00',
        4: '09:30',
        5: '10:00',
        6: '10:30',
        7: '11:00',
        8: '11:30',
        9: '13:00',
        10: '13:30',
        11: '14:00',
        12: '14:30',
        13: '15:00',
        14: '15:30',
        15: '16:00'
      }
    };
  },
  methods: {
    canViewPrescription() {
      return this.isDoctorUser() || this.isAuth(['ROOT', 'PRESCRIPTION:SELECT']);
    },
    canEditPrescription() {
      return this.isDoctorUser() || this.isAuth(['ROOT', 'PRESCRIPTION:INSERT', 'PRESCRIPTION:UPDATE']);
    },
    isDoctorUser() {
      try {
        const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
        return userInfo.job === '医生' && !!userInfo.refId;
      } catch (e) {
        return false;
      }
    },
    slotText(slot) {
      return this.slotMap[slot] || '';
    },
    loadDataList() {
      if (!this.canViewPrescription()) {
        return;
      }
      this.dataListLoading = true;
      const data = {
        doctorName: this.dataForm.doctorName || null,
        patientName: this.dataForm.patientName || null,
        subDeptName: this.dataForm.subDeptName || null,
        hasPrescription: this.dataForm.hasPrescription,
        startDate: this.dataForm.startDate || null,
        endDate: this.dataForm.endDate || null,
        page: this.pageIndex,
        length: this.pageSize
      };
      this.$http('/doctor_prescription/searchRegistrationByPage', 'POST', data, true, resp => {
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
        subDeptName: null,
        hasPrescription: null,
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
    },
    openDialog(row) {
      this.$nextTick(() => {
        this.$refs.addOrUpdate.init(row);
      });
    }
  },
  created() {
    this.loadDataList();
  }
};
</script>
