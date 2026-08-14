<template>
  <div class="ai-chat">
    <el-card class="chat-card" shadow="never">
      <template #header>
        <div class="chat-header">
          <div class="chat-header__left">
            <el-icon class="chat-header__icon"><ChatDotRound /></el-icon>
            <span class="chat-header__title">AI 智能客服</span>
            <el-tag size="small" type="success">已接入知识库 · RAG</el-tag>
          </div>
          <el-button :disabled="loading" @click="clearChat">
            <el-icon><Delete /></el-icon>
            <span>清空对话</span>
          </el-button>
        </div>
      </template>

      <div ref="messageListEl" class="message-list">
        <div
          v-for="msg in messages"
          :key="msg.id"
          class="message-row"
          :class="{ 'message-row--user': msg.role === 'user' }"
        >
          <el-avatar
            :size="36"
            class="message-avatar"
            :class="msg.role === 'user' ? 'avatar-user' : 'avatar-ai'"
          >
            <el-icon v-if="msg.role === 'assistant'"><Service /></el-icon>
            <span v-else>我</span>
          </el-avatar>
          <div class="message-content">
            <div class="message-bubble" :class="msg.role === 'user' ? 'bubble-user' : 'bubble-ai'">
              <span v-if="msg.loading" class="typing"><i /><i /><i /></span>
              <span v-else class="message-bubble__text">{{ msg.content }}</span>
            </div>
            <el-collapse v-if="msg.role === 'assistant' && !msg.loading && msg.debug" class="rag-debug">
              <el-collapse-item name="debug">
                <template #title>
                  <span class="rag-debug__title">RAG 检索详情（开发环境）</span>
                </template>
                <div class="rag-debug__query">问题：{{ msg.debug.query }}</div>
                <div class="rag-debug__meta">
                  知识库命中：{{ msg.debug.knowledgeUsed ? '是' : '否' }} · topK={{ msg.debug.topK }} ·
                  阈值={{ msg.debug.similarityThreshold }}
                </div>
                <div v-if="msg.debug.hits.length === 0" class="rag-debug__empty">
                  未检索到满足阈值的知识片段
                </div>
                <div v-for="(hit, index) in msg.debug.hits" :key="index" class="rag-debug__hit">
                  <div class="rag-debug__hit-head">
                    #{{ index + 1 }} 来源：{{ hit.title }}（片段 #{{ hit.chunkIndex }}）
                    · 相似度 {{ formatSimilarity(hit.similarity) }}
                  </div>
                  <div class="rag-debug__hit-content">{{ hit.content }}</div>
                </div>
              </el-collapse-item>
            </el-collapse>
          </div>
        </div>
      </div>

      <div class="chat-input">
        <el-input
          v-model="input"
          type="textarea"
          :rows="3"
          resize="none"
          maxlength="2000"
          show-word-limit
          placeholder="例如：篮球怎么借？"
          :disabled="loading"
          @keydown.enter.exact.prevent="send"
        />
        <div class="chat-input__actions">
          <span class="chat-input__tip">Enter 发送，Shift + Enter 换行</span>
          <el-button type="primary" :loading="loading" :disabled="!canSend" @click="send">
            <el-icon v-if="!loading"><Promotion /></el-icon>
            <span>{{ loading ? '回复中…' : '发送' }}</span>
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import { ChatDotRound, Delete, Promotion, Service } from '@element-plus/icons-vue'
import { sendChatMessage, type RagDebugVO } from '@/api/ai'

interface ChatMessage {
  id: number
  role: 'user' | 'assistant'
  content: string
  loading?: boolean
  debug?: RagDebugVO | null
}

const WELCOME_TEXT = '你好，我是体育器材智能客服。你可以问我器材借用、归还、库存等问题，比如“篮球怎么借？”。'

let nextId = 1
const messages = ref<ChatMessage[]>([
  { id: nextId++, role: 'assistant', content: WELCOME_TEXT },
])
const input = ref('')
const loading = ref(false)
const messageListEl = ref<HTMLElement>()

const canSend = computed(() => input.value.trim().length > 0 && !loading.value)

async function send() {
  const text = input.value.trim()
  if (!text || loading.value) {
    return
  }
  input.value = ''
  messages.value.push({ id: nextId++, role: 'user', content: text })

  const pending: ChatMessage = { id: nextId++, role: 'assistant', content: '', loading: true }
  messages.value.push(pending)
  loading.value = true
  await scrollToBottom()

  try {
    const result = await sendChatMessage({ message: text })
    pending.loading = false
    pending.content = result.content || '（AI 没有返回内容，请重试）'
    pending.debug = result.debug ?? null
  } catch {
    pending.loading = false
    pending.content = '抱歉，暂时无法回答，请稍后再试。'
  } finally {
    loading.value = false
    await scrollToBottom()
  }
}

function clearChat() {
  messages.value = [{ id: nextId++, role: 'assistant', content: WELCOME_TEXT }]
  input.value = ''
}

function formatSimilarity(score: number | null) {
  return score == null ? '—' : score.toFixed(4)
}

async function scrollToBottom() {
  await nextTick()
  const el = messageListEl.value
  if (el) {
    el.scrollTop = el.scrollHeight
  }
}
</script>

<style scoped lang="scss">
.ai-chat {
  height: calc(100vh - 140px);
  min-height: 480px;

  .chat-card {
    height: 100%;
    display: flex;
    flex-direction: column;

    :deep(.el-card__header) {
      padding: 12px 16px;
    }

    :deep(.el-card__body) {
      flex: 1;
      min-height: 0;
      display: flex;
      flex-direction: column;
      padding: 16px;
    }
  }
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;

  &__left {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  &__icon {
    font-size: 20px;
    color: #409eff;
  }

  &__title {
    font-size: 16px;
    font-weight: 600;
  }
}

.message-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 4px 8px;
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 16px;

  &--user {
    flex-direction: row-reverse;
  }
}

.message-content {
  max-width: 72%;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.message-avatar {
  flex-shrink: 0;

  &.avatar-ai {
    background: #409eff;
    color: #fff;
  }

  &.avatar-user {
    background: #67c23a;
    color: #fff;
  }
}

.message-bubble {
  max-width: 100%;
  padding: 10px 14px;
  border-radius: 8px;
  line-height: 1.6;
  font-size: 14px;
  white-space: pre-wrap;
  word-break: break-word;

  &.bubble-ai {
    background: #f4f4f5;
    color: #303133;
  }

  &.bubble-user {
    background: #409eff;
    color: #fff;
  }
}

.typing {
  display: inline-flex;
  gap: 4px;
  align-items: center;
  height: 20px;

  i {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #909399;
    animation: typing-bounce 1s infinite ease-in-out;

    &:nth-child(2) {
      animation-delay: 0.15s;
    }

    &:nth-child(3) {
      animation-delay: 0.3s;
    }
  }
}

.rag-debug {
  margin-top: 8px;
  width: 100%;
  border: 1px dashed #c0c4cc;
  border-radius: 6px;
  background: #fafafa;

  :deep(.el-collapse-item__header) {
    height: 34px;
    line-height: 34px;
    padding: 0 10px;
    background: transparent;
    border: none;
  }

  :deep(.el-collapse-item__wrap) {
    border: none;
  }

  :deep(.el-collapse-item__content) {
    padding: 0 10px 10px;
    font-size: 12px;
    color: #606266;
  }

  &__title {
    font-size: 12px;
    color: #909399;
  }

  &__query {
    margin-bottom: 4px;
  }

  &__meta {
    margin-bottom: 8px;
    color: #909399;
  }

  &__empty {
    color: #909399;
  }

  &__hit {
    margin-bottom: 8px;
    padding: 6px 8px;
    background: #fff;
    border-radius: 4px;
    border: 1px solid #ebeef5;
  }

  &__hit-head {
    color: #409eff;
    margin-bottom: 4px;
  }

  &__hit-content {
    white-space: pre-wrap;
    word-break: break-word;
    line-height: 1.6;
  }
}

@keyframes typing-bounce {
  0%,
  60%,
  100% {
    transform: translateY(0);
    opacity: 0.4;
  }

  30% {
    transform: translateY(-5px);
    opacity: 1;
  }
}

.chat-input {
  margin-top: 12px;

  &__actions {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: 10px;
  }

  &__tip {
    color: #909399;
    font-size: 12px;
  }
}
</style>
