<template>
  <el-card>
    <div class="toolbar">
      <el-input
        v-model="query.keyword"
        placeholder="文档标题 / 文件名"
        clearable
        style="width: 220px"
        @keyup.enter="loadData"
      />
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 140px">
        <el-option label="处理中" :value="0" />
        <el-option label="已就绪" :value="1" />
        <el-option label="失败" :value="2" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
      <div class="toolbar__spacer" />
      <el-button v-permission="['knowledge:manage']" type="success" @click="openUploadDialog">
        <el-icon><Upload /></el-icon>
        <span>上传文档</span>
      </el-button>
      <el-button v-permission="['knowledge:manage']" :loading="seeding" @click="handleSeed">
        <el-icon><Collection /></el-icon>
        <span>导入内置知识库</span>
      </el-button>
    </div>

    <el-table v-loading="loading" :data="records" border>
      <el-table-column prop="title" label="文档标题" min-width="180" show-overflow-tooltip />
      <el-table-column prop="fileName" label="文件名" min-width="180" show-overflow-tooltip />
      <el-table-column prop="fileType" label="类型" width="80" />
      <el-table-column label="大小" width="100">
        <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="chunkCount" label="分块数" width="80" />
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row.id)">查看</el-button>
          <el-button
            v-permission="['knowledge:manage']"
            link
            type="warning"
            :loading="rebuildingId === row.id"
            @click="handleRebuild(row)"
          >
            重建向量
          </el-button>
          <el-button
            v-permission="['knowledge:manage']"
            link
            type="danger"
            @click="handleDelete(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
        v-model:current-page="query.current"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="loadData"
        @size-change="loadData"
      />
    </div>

    <el-dialog v-model="uploadVisible" title="上传知识文档" width="520px" @closed="resetUpload">
      <el-form label-width="80px">
        <el-form-item label="文档标题">
          <el-input v-model="uploadForm.title" maxlength="200" placeholder="留空则使用文件名" />
        </el-form-item>
        <el-form-item label="文件">
          <el-upload
            drag
            :auto-upload="false"
            :limit="1"
            accept=".txt,.md,.docx,.pdf"
            :on-change="onFileChange"
            :on-remove="onFileRemove"
            :file-list="fileList"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽文件到此处，或<em>点击选择</em></div>
            <template #tip>
              <div class="el-upload__tip">支持 txt / md / docx / pdf，单个不超过 10MB</div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="uploadForm.remark" maxlength="500" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" :disabled="!selectedFile" @click="handleUpload">
          上传并构建向量
        </el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="文档详情" size="45%">
      <template v-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="标题">{{ detail.title }}</el-descriptions-item>
          <el-descriptions-item label="文件名">{{ detail.fileName }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ detail.fileType }}</el-descriptions-item>
          <el-descriptions-item label="大小">{{ formatSize(detail.fileSize) }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusType(detail.status)">{{ statusText(detail.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="分块数">{{ detail.chunkCount }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detail.remark || '—' }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.errorMsg" label="失败原因" :span="2">
            {{ detail.errorMsg }}
          </el-descriptions-item>
        </el-descriptions>
        <h4 class="chunk-title">知识分块（{{ detail.chunks.length }}）</h4>
        <div v-for="chunk in detail.chunks" :key="chunk.id" class="chunk-item">
          <div class="chunk-item__head">片段 #{{ chunk.chunkIndex }}</div>
          <div class="chunk-item__content">{{ chunk.content }}</div>
        </div>
      </template>
    </el-drawer>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type UploadFile, type UploadFiles } from 'element-plus'
import { Collection, Upload, UploadFilled } from '@element-plus/icons-vue'
import {
  deleteKnowledgeDocument,
  getKnowledgeDocument,
  pageKnowledgeDocuments,
  rebuildKnowledgeDocument,
  seedKnowledge,
  uploadKnowledgeDocument,
  type KnowledgeDocumentDetailVO,
  type KnowledgeDocumentVO,
} from '@/api/knowledge'

const loading = ref(false)
const records = ref<KnowledgeDocumentVO[]>([])
const total = ref(0)
const query = reactive({ current: 1, size: 10, keyword: '', status: undefined as number | undefined })

const uploadVisible = ref(false)
const uploading = ref(false)
const selectedFile = ref<File>()
const fileList = ref<UploadFiles>([])
const uploadForm = reactive({ title: '', remark: '' })

const detailVisible = ref(false)
const detail = ref<KnowledgeDocumentDetailVO>()

const seeding = ref(false)
const rebuildingId = ref<number>()

function statusText(status: number) {
  return status === 1 ? '已就绪' : status === 2 ? '失败' : '处理中'
}

function statusType(status: number): 'success' | 'danger' | 'info' {
  return status === 1 ? 'success' : status === 2 ? 'danger' : 'info'
}

function formatSize(size: number) {
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(2)} MB`
}

async function loadData() {
  loading.value = true
  try {
    const page = await pageKnowledgeDocuments({
      current: query.current,
      size: query.size,
      keyword: query.keyword || undefined,
      status: query.status,
    })
    records.value = page.records
    total.value = page.total
  } finally {
    loading.value = false
  }
}

function openUploadDialog() {
  uploadVisible.value = true
}

function onFileChange(file: UploadFile) {
  selectedFile.value = file.raw
}

function onFileRemove() {
  selectedFile.value = undefined
}

function resetUpload() {
  selectedFile.value = undefined
  fileList.value = []
  uploadForm.title = ''
  uploadForm.remark = ''
}

async function handleUpload() {
  if (!selectedFile.value) {
    ElMessage.warning('请选择文档文件')
    return
  }
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    if (uploadForm.title.trim()) formData.append('title', uploadForm.title.trim())
    if (uploadForm.remark.trim()) formData.append('remark', uploadForm.remark.trim())
    await uploadKnowledgeDocument(formData)
    ElMessage.success('上传成功，向量已构建')
    uploadVisible.value = false
    await loadData()
  } catch {
    // 错误提示由 request 拦截器统一处理
  } finally {
    uploading.value = false
  }
}

async function openDetail(id: number) {
  detail.value = await getKnowledgeDocument(id)
  detailVisible.value = true
}

async function handleRebuild(row: KnowledgeDocumentVO) {
  await ElMessageBox.confirm(
    `确认重建「${row.title}」的向量？将删除旧分块向量并重新解析、嵌入。`,
    '提示',
    { type: 'warning' },
  )
  rebuildingId.value = row.id
  try {
    await rebuildKnowledgeDocument(row.id)
    ElMessage.success('向量重建完成')
    await loadData()
  } finally {
    rebuildingId.value = undefined
  }
}

async function handleDelete(row: KnowledgeDocumentVO) {
  await ElMessageBox.confirm(`确认删除「${row.title}」？将同时删除分块与向量。`, '提示', {
    type: 'warning',
  })
  await deleteKnowledgeDocument(row.id)
  ElMessage.success('删除成功')
  await loadData()
}

async function handleSeed() {
  await ElMessageBox.confirm('将解析并向量化内置知识库（共 11 篇器材知识文档），已存在的同标题文档会跳过。', '提示', {
    type: 'info',
  })
  seeding.value = true
  try {
    const result = await seedKnowledge()
    ElMessage.success(`导入完成：新增 ${result.imported} 篇，跳过 ${result.skipped} 篇`)
    await loadData()
  } finally {
    seeding.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;

  &__spacer {
    flex: 1;
  }
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

.chunk-title {
  margin: 18px 0 10px;
}

.chunk-item {
  margin-bottom: 10px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 10px 12px;

  &__head {
    font-weight: 600;
    color: #409eff;
    margin-bottom: 6px;
  }

  &__content {
    color: #606266;
    font-size: 13px;
    line-height: 1.7;
    white-space: pre-wrap;
    word-break: break-word;
  }
}
</style>
