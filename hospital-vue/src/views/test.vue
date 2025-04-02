<template>
  <div class="container">
    <!-- 初始状态 -->
    <div v-if="!isSearching" class="init-state">
      <h1>智能分析系统</h1>
      <div class="input-group">
        <input
            v-model="query"
            type="text"
            placeholder="输入查询内容..."
            @keyup.enter="startSearch"
        />
        <div class="upload-area">
          <input type="file" @change="handleFileUpload" ref="fileInput" hidden />
          <button @click="triggerFileUpload" class="upload-btn">
            {{ uploadedFile ? uploadedFile.name : '上传附件' }}
          </button>
        </div>
        <button @click="startSearch" class="search-btn">开始分析</button>
      </div>
    </div>

    <!-- 搜索结果状态 -->
    <div v-else class="result-layout">
      <!-- 左侧搜索过程 -->
      <div class="process-panel">
        <h2>分析进度</h2>
        <div class="process-steps">
          <div
              v-for="(step, index) in processSteps"
              :key="index"
              :class="['step', { active: step.active }]"
          >
            <div class="step-icon">{{ step.completed ? '✓' : index + 1 }}</div>
            <div class="step-info">
              <h3>{{ step.title }}</h3>
              <p>{{ step.description }}</p>
              <div v-if="step.progress" class="progress-bar">
                <div :style="{ width: step.progress + '%' }"></div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧结果标签页 -->
      <div class="result-panel">
        <div class="tabs-header">
          <button
              v-for="tab in tabs"
              :key="tab.id"
              :class="['tab', { active: currentTab === tab.id }]"
              @click="currentTab = tab.id"
          >
            {{ tab.label }}
          </button>
        </div>

        <div class="tab-content">
          <div v-if="currentTab === 'result'">
            <h3>分析结果</h3>
            <div v-html="analysisResult"></div>
          </div>

          <div v-if="currentTab === 'data'">
            <h3>源数据</h3>
            <pre>{{ rawData }}</pre>
          </div>

          <div v-if="currentTab === 'chart'">
            <h3>可视化图表</h3>
            <div class="chart-placeholder">
              （图表展示区域）
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'

const isSearching = ref(false)
const query = ref('')
const uploadedFile = ref(null)
const currentTab = ref('result')
const fileInput = ref(null)

// 模拟分析结果
const analysisResult = ref('')
const rawData = ref('')

// 处理文件上传
const handleFileUpload = (e) => {
  const file = e.target.files[0]
  if (file) {
    uploadedFile.value = file
  }
}

const triggerFileUpload = () => {
  fileInput.value.click()
}

// 进度步骤数据
const processSteps = reactive([
  {
    title: '文件解析',
    description: '正在解析上传文件...',
    progress: 0,
    completed: false,
    active: true
  },
  {
    title: '数据分析',
    description: '正在进行数据预处理...',
    progress: 0,
    completed: false,
    active: false
  },
  {
    title: '模型运算',
    description: '正在运行分析模型...',
    progress: 0,
    completed: false,
    active: false
  }
])

// 标签页配置
const tabs = [
  { id: 'result', label: '分析结果' },
  { id: 'data', label: '源数据' },
  { id: 'chart', label: '可视化' }
]

// 开始搜索
const startSearch = async () => {
  if (!query.value && !uploadedFile.value) return

  isSearching.value = true

  // 模拟分析过程
  for (const [index, step] of processSteps.entries()) {
    step.active = true
    for (let progress = 0; progress <= 100; progress += 10) {
      await new Promise(resolve => setTimeout(resolve, 50))
      step.progress = progress
    }
    step.completed = true
    step.active = false
    if (index < processSteps.length - 1) {
      processSteps[index + 1].active = true
    }
  }

  // 模拟结果
  analysisResult.value = `
    <p>分析完成！主要发现：</p>
    <ul>
      <li>发现3个关键模式</li>
      <li>识别出2个异常数据点</li>
      <li>生成5项建议方案</li>
    </ul>
  `
  rawData.value = JSON.stringify({
    query: query.value,
    file: uploadedFile.value?.name,
    timestamp: new Date().toISOString()
  }, null, 2)
}
</script>

<style scoped>
.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;
  min-height: 100vh;
}

.init-state {
  text-align: center;
  padding: 4rem 0;
}

h1 {
  font-size: 2.5rem;
  margin-bottom: 2rem;
  color: #2c3e50;
}

.input-group {
  max-width: 600px;
  margin: 0 auto;
}

input[type="text"] {
  width: 100%;
  padding: 1rem;
  border: 2px solid #ddd;
  border-radius: 8px;
  margin-bottom: 1rem;
  font-size: 1rem;
}

.upload-area {
  margin: 1rem 0;
}

.upload-btn, .search-btn {
  padding: 0.8rem 1.5rem;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s;
}

.upload-btn {
  background: #f0f2f5;
  color: #666;
  border: 2px dashed #ccc;
}

.search-btn {
  background: #3498db;
  color: white;
  margin-top: 1rem;
  width: 100%;
}

.result-layout {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 2rem;
  height: 80vh;
}

.process-panel {
  border-right: 1px solid #eee;
  padding-right: 2rem;
}

.process-steps {
  margin-top: 1rem;
}

.step {
  display: flex;
  gap: 1rem;
  padding: 1rem;
  margin-bottom: 1rem;
  border-radius: 8px;
  background: #f8f9fa;
  opacity: 0.6;
}

.step.active {
  opacity: 1;
  background: white;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.step-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #3498db;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
}

.tabs-header {
  display: flex;
  gap: 1rem;
  border-bottom: 1px solid #eee;
  margin-bottom: 1rem;
}

.tab {
  padding: 0.8rem 1.5rem;
  background: none;
  border: none;
  cursor: pointer;
  color: #666;
  border-radius: 6px 6px 0 0;
}

.tab.active {
  background: #3498db;
  color: white;
}

.chart-placeholder {
  height: 400px;
  background: #f8f9fa;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
  border-radius: 8px;
}

.progress-bar {
  height: 4px;
  background: #eee;
  margin-top: 0.5rem;
  border-radius: 2px;
}

.progress-bar div {
  height: 100%;
  background: #3498db;
  border-radius: 2px;
  transition: width 0.3s ease;
}

pre {
  background: #f8f9fa;
  padding: 1rem;
  border-radius: 8px;
  overflow-x: auto;
}
</style>