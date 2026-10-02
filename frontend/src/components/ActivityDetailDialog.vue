<template>
  <el-dialog
    v-model="visible"
    class="activity-dialog"
    align-center
    :show-close="false"
    width="min(760px, calc(100% - 28px))"
  >
    <template #header>
      <div class="dialog-topline">
        <span class="eyebrow">ACTIVITY DETAIL</span>
        <button class="dialog-close" aria-label="关闭详情" @click="visible = false">×</button>
      </div>
    </template>

    <div v-if="detail" class="detail-layout">
      <div class="detail-art" :class="`art-${(detail.id || 0) % 4}`">
        <img
          v-if="detail.coverImage"
          :src="detail.coverImage"
          :alt="detail.title"
          @error="handleImageError"
        />
        <div v-else class="art-placeholder">✦</div>
      </div>
      <div class="detail-content">
        <div class="eyebrow">{{ detail.categoryName || '校园活动' }} / DETAIL</div>
        <h2>{{ detail.title }}</h2>
        <p class="detail-description">{{ detail.description || '活动发布者暂未填写详细介绍。' }}</p>
        <div class="detail-grid">
          <div><span>时间</span><strong>{{ formatActivityDateRange(detail.startTime, detail.endTime) }}</strong></div>
          <div><span>地点</span><strong>{{ detail.location || '地点待定' }}</strong></div>
          <div><span>发布者</span><strong>{{ detail.publisherName || 'CampusAgent' }}</strong></div>
          <div><span>报名进度</span><strong>{{ participantText(detail) }}</strong></div>
        </div>

        <div v-if="detail.currentUserSignupStatus === 0" class="signup-notice">
          你已报名，活动开始前记得来签到。
        </div>
        <div v-else-if="detail.currentUserSignupStatus === 1" class="signup-notice checked">
          你已经完成签到，感谢参与。
        </div>
        <div v-else-if="detail.currentUserSignupStatus === 2" class="signup-notice muted">
          你已取消过报名，现在可以重新报名。
        </div>
        <div v-else-if="detail.currentUserSignupStatus === 3" class="signup-notice muted">
          本次报名记录显示为已缺席。
        </div>

        <div class="detail-actions">
          <el-button class="dialog-secondary" @click="visible = false">稍后再看</el-button>
          <el-button
            v-if="detail.currentUserSignupStatus === 0"
            class="dialog-primary"
            :loading="signupLoading"
            @click="$emit('cancel')"
          >
            取消报名
          </el-button>
          <el-button
            v-else-if="canSignup"
            class="dialog-primary"
            :loading="signupLoading"
            @click="$emit('signup')"
          >
            {{ detail.currentUserSignupStatus === 2 ? '重新报名' : '立即报名' }}
          </el-button>
          <el-button v-else class="dialog-primary" disabled>
            {{ detail.status === 4 ? '活动进行中' : '暂不可报名' }}
          </el-button>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { computed } from 'vue'
import { formatActivityDateRange } from '@/utils/date'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  detail: {
    type: Object,
    default: null
  },
  signupLoading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'signup', 'cancel'])

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const canSignup = computed(() => {
  const status = props.detail?.currentUserSignupStatus
  return props.detail?.status === 3 && (status == null || status === 2)
})

function participantText(activity) {
  if (!activity.maxParticipants) return `${activity.currentParticipants || 0} 人已报名`
  return `${activity.currentParticipants || 0} / ${activity.maxParticipants} 人`
}

function handleImageError(event) {
  const image = event.currentTarget
  image.style.display = 'none'
  image.parentElement?.classList.add('has-image-error')
}
</script>

<style scoped>
.activity-dialog :deep(.el-dialog) {
  overflow: hidden;
  border: 1px solid #dfdfd4;
  border-radius: 0;
  background: #f6f5ef;
}

.activity-dialog :deep(.el-dialog__header) {
  margin: 0;
  padding: 18px 22px;
  border-bottom: 1px solid #dfdfd4;
}

.activity-dialog :deep(.el-dialog__body) {
  padding: 0;
}

.dialog-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #777870;
  font-family: "DM Mono", monospace;
  font-size: 10px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.eyebrow::before {
  width: 22px;
  height: 1px;
  background: currentColor;
  content: "";
}

.dialog-close {
  width: 32px;
  height: 32px;
  border: 1px solid #dfdfd4;
  background: transparent;
  color: #20211e;
  font-size: 18px;
}

.detail-layout {
  display: grid;
  grid-template-columns: minmax(220px, 0.75fr) minmax(0, 1.25fr);
}

.detail-art {
  position: relative;
  min-height: 470px;
  overflow: hidden;
  background: #b9e7f5;
}

.detail-art.art-1 {
  background: #ffb36a;
}

.detail-art.art-2 {
  background: #a9e6c0;
}

.detail-art.art-3 {
  background: #d9cdf9;
}

.detail-art img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  mix-blend-mode: multiply;
  opacity: 0.83;
}

.detail-art.has-image-error::after {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  color: rgba(32, 33, 30, 0.68);
  content: "✦";
  font-size: 56px;
  font-weight: 800;
}

.art-placeholder {
  display: grid;
  height: 100%;
  min-height: 190px;
  place-items: center;
  color: rgba(32, 33, 30, 0.72);
  font-size: 70px;
  font-weight: 800;
}

.detail-content {
  padding: 34px 30px;
}

.detail-content h2 {
  margin: 18px 0;
  font-size: 38px;
  line-height: 1;
  letter-spacing: -0.08em;
}

.detail-description {
  color: #777870;
  font-size: 13px;
  line-height: 1.8;
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin: 25px 0;
}

.detail-grid > div {
  padding: 12px;
  border: 1px solid #dfdfd4;
  background: #fffef9;
}

.detail-grid span {
  display: block;
  color: #777870;
  font-family: "DM Mono", monospace;
  font-size: 9px;
}

.detail-grid strong {
  display: block;
  margin-top: 7px;
  font-size: 11px;
  line-height: 1.45;
}

.signup-notice {
  padding: 11px 12px;
  border-left: 3px solid #87971c;
  background: #eaf0d2;
  color: #5e681e;
  font-size: 11px;
}

.signup-notice.checked {
  border-left-color: #4c9b69;
  background: #dff2e6;
  color: #39714c;
}

.signup-notice.muted {
  border-left-color: #9d9d94;
  background: #e9e9e2;
  color: #777870;
}

.detail-actions {
  display: flex;
  gap: 10px;
  margin-top: 28px;
}

.dialog-secondary,
.dialog-primary {
  height: 44px;
  border-radius: 0;
  font-size: 11px;
  font-weight: 800;
}

.dialog-secondary {
  border-color: #dfdfd4;
  background: transparent;
  color: #20211e;
}

.dialog-primary {
  flex: 1;
  border-color: #20211e;
  background: #20211e;
  color: #fffef9;
}

.dialog-primary:hover {
  border-color: #4d5742;
  background: #4d5742;
  color: #fffef9;
}

@media (max-width: 760px) {
  .detail-layout {
    grid-template-columns: 1fr;
  }

  .detail-art {
    min-height: 190px;
  }
}

@media (max-width: 520px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .detail-content {
    padding: 26px 20px 24px;
  }

  .detail-content h2 {
    font-size: 32px;
  }
}
</style>
