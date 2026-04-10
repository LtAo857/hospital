<template>
  <el-dialog
    v-if="canEditPrescription()"
    :title="!dataForm.prescriptionId ? '开立电子处方' : '编辑电子处方'"
    :close-on-click-modal="false"
    v-model="visible"
    width="860px"
  >
    <el-scrollbar height="560px">
      <el-descriptions :column="2" border class="mb-4" v-if="registrationInfo.registrationId">
        <el-descriptions-item label="挂号单号">{{ registrationInfo.registrationId }}</el-descriptions-item>
        <el-descriptions-item label="就诊日期">{{ registrationInfo.date }} {{ slotText(registrationInfo.slot) }}</el-descriptions-item>
        <el-descriptions-item label="患者">{{ registrationInfo.patientName }} {{ registrationInfo.patientSex }} {{ registrationInfo.patientAge || '' }}岁</el-descriptions-item>
        <el-descriptions-item label="医生">{{ registrationInfo.doctorName }}</el-descriptions-item>
        <el-descriptions-item label="诊室">{{ registrationInfo.subDeptName }}</el-descriptions-item>
        <el-descriptions-item label="挂号状态">
          <el-tag :type="registrationInfo.paymentStatus === 2 ? 'success' : registrationInfo.paymentStatus === 1 ? 'warning' : 'info'">
            {{ registrationInfo.paymentStatus === 2 ? '已支付' : registrationInfo.paymentStatus === 1 ? '已挂号' : '未支付' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <el-form :model="dataForm" ref="dataFormRef" :rules="dataRule" label-width="90px">
        <el-form-item label="临床诊断" prop="diagnosis">
          <el-input
            v-model="dataForm.diagnosis"
            type="textarea"
            :rows="4"
            maxlength="2000"
            show-word-limit
            clearable
          />
        </el-form-item>
      </el-form>

      <div class="rp-header">
        <span>处方明细</span>
        <el-button type="primary" link @click="addRpItem">新增药品</el-button>
      </div>
      <div v-for="(item,index) in dataForm.rpList" :key="index" class="rp-item">
        <div class="rp-item__head">
          <span>药品 {{ index + 1 }}</span>
          <el-button type="danger" link @click="removeRpItem(index)">删除</el-button>
        </div>
        <el-form :model="item" label-width="70px" class="rp-form">
          <el-form-item label="名称">
            <el-input v-model="item.name" clearable maxlength="100" />
          </el-form-item>
          <el-form-item label="规格">
            <el-input v-model="item.spec" clearable maxlength="100" />
          </el-form-item>
          <el-form-item label="数量">
            <el-input-number v-model="item.num" :min="1" :max="999" />
          </el-form-item>
          <el-form-item label="用法">
            <el-input v-model="item.method" clearable maxlength="200" />
          </el-form-item>
        </el-form>
      </div>
    </el-scrollbar>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="dataFormSubmit">保存</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script>
export default {
  data() {
    return {
      visible: false,
      registrationInfo: {
        registrationId: null,
        date: null,
        slot: null,
        patientName: null,
        patientSex: null,
        patientAge: null,
        doctorName: null,
        subDeptName: null,
        paymentStatus: null
      },
      dataForm: {
        prescriptionId: null,
        registrationId: null,
        diagnosis: '',
        rpList: []
      },
      dataRule: {
        diagnosis: [{ required: true, message: '临床诊断不能为空', trigger: 'blur' }]
      },
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
    defaultRpItem() {
      return {
        name: '',
        spec: '',
        num: 1,
        method: ''
      };
    },
    reset() {
      this.registrationInfo = {
        registrationId: null,
        date: null,
        slot: null,
        patientName: null,
        patientSex: null,
        patientAge: null,
        doctorName: null,
        subDeptName: null,
        paymentStatus: null
      };
      this.dataForm = {
        prescriptionId: null,
        registrationId: null,
        diagnosis: '',
        rpList: [this.defaultRpItem()]
      };
    },
    init(row) {
      this.reset();
      this.visible = true;
      this.dataForm.registrationId = row.registrationId;
      this.$nextTick(() => {
        this.$refs.dataFormRef.resetFields();
        this.loadPrescription(row.registrationId);
      });
    },
    loadPrescription(registrationId) {
      this.$http('/doctor_prescription/searchByRegistrationId', 'POST', { registrationId }, true, resp => {
        const result = resp.result || {};
        this.registrationInfo = {
          registrationId: result.registrationId || registrationId,
          date: result.date || '',
          slot: result.slot || null,
          patientName: result.patientName || '',
          patientSex: result.patientSex || '',
          patientAge: result.patientAge || '',
          doctorName: result.doctorName || '',
          subDeptName: result.subDeptName || '',
          paymentStatus: result.paymentStatus || null
        };
        this.dataForm.prescriptionId = result.prescriptionId || null;
        this.dataForm.diagnosis = result.diagnosis || '';
        this.dataForm.rpList = result.rpList && result.rpList.length > 0 ? result.rpList : [this.defaultRpItem()];
      });
    },
    addRpItem() {
      this.dataForm.rpList.push(this.defaultRpItem());
    },
    removeRpItem(index) {
      this.dataForm.rpList.splice(index, 1);
      if (this.dataForm.rpList.length === 0) {
        this.dataForm.rpList.push(this.defaultRpItem());
      }
    },
    validateRpList() {
      const validItems = this.dataForm.rpList.filter(item => item.name && item.name.trim() && item.method && item.method.trim());
      if (validItems.length === 0) {
        ElMessage({ message: '至少填写一条完整药品信息', type: 'warning', duration: 1200 });
        return null;
      }
      return validItems.map(item => ({
        name: item.name.trim(),
        spec: item.spec || '',
        num: item.num || 1,
        method: item.method.trim()
      }));
    },
    dataFormSubmit() {
      this.$refs.dataFormRef.validate(valid => {
        if (!valid) {
          return;
        }
        const rpList = this.validateRpList();
        if (!rpList) {
          return;
        }
        const data = {
          registrationId: this.dataForm.registrationId,
          diagnosis: this.dataForm.diagnosis,
          rp: JSON.stringify(rpList)
        };
        this.$http('/doctor_prescription/save', 'POST', data, true, () => {
          ElMessage({ message: '保存成功', type: 'success' });
          this.visible = false;
          this.$emit('refreshDataList');
        });
      });
    }
  }
};
</script>

<style lang="less" scoped>
.mb-4 {
  margin-bottom: 16px;
}
.rp-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-weight: 600;
}
.rp-item {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 12px 16px;
  margin-bottom: 12px;
}
.rp-item__head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}
.rp-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 16px;
}
</style>
