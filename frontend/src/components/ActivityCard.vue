<template>
  <article class="activity-card">
    <div class="activity-art" :class="`art-${index % 4}`">
      <span class="activity-tag">{{ activity.categoryName || '校园活动' }}</span>
      <img
        v-if="activity.coverImage"
        :src="activity.coverImage"
        :alt="activity.title"
        @error="handleImageError"
      />
      <div v-else class="art-placeholder">{{ artSymbols[index % 4] }}</div>
    </div>
    <div class="activity-body">
      <h3>{{ activity.title }}</h3>
      <div class="activity-meta">
        <span>◷ <strong>{{ formatActivityDate(activity.startTime) }}</strong></span>
        <span>⌖ {{ activity.location || '地点待定' }}</span>
      </div>
      <div class="activity-footer">
        <span class="capacity">
          {{ activity.maxParticipants ? `${activity.currentParticipants || 0} / ${activity.maxParticipants}` : '不限人数' }}
        </span>
        <el-button class="detail-button" @click="$emit('detail', activity.id)">
          查看详情
        </el-button>
      </div>
    </div>
  </article>
</template>

<script setup>
import { formatActivityDate } from '@/utils/date'

defineProps({
  activity: {
    type: Object,
    required: true
  },
  index: {
    type: Number,
    default: 0
  }
})

defineEmits(['detail'])

const artSymbols = ['✦', '◌', '↗', '⌁']

function handleImageError(event) {
  const image = event.currentTarget
  image.style.display = 'none'
  image.parentElement?.classList.add('has-image-error')
}
</script>

<style scoped>
.activity-card {
  display: flex;
  min-height: 386px;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid #dfdfd4;
  background: #fffef9;
  box-shadow: 0 18px 46px rgba(39, 42, 35, 0.07);
  transition: transform 180ms ease, box-shadow 180ms ease;
}

.activity-card:hover {
  box-shadow: 0 24px 60px rgba(39, 42, 35, 0.13);
  transform: translateY(-5px);
}

.activity-art {
  position: relative;
  min-height: 172px;
  overflow: hidden;
  background: #b9e7f5;
}

.activity-art.art-1 {
  background: #ffb36a;
}

.activity-art.art-2 {
  background: #a9e6c0;
}

.activity-art.art-3 {
  background: #d9cdf9;
}

.activity-art img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  mix-blend-mode: multiply;
  opacity: 0.83;
}

.activity-art.has-image-error::after {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  color: rgba(32, 33, 30, 0.68);
  content: "✦";
  font-size: 56px;
  font-weight: 800;
}

.activity-tag {
  position: absolute;
  z-index: 1;
  top: 14px;
  left: 14px;
  padding: 6px 8px;
  background: #d5f44d;
  font-family: "DM Mono", monospace;
  font-size: 9px;
}

.art-placeholder {
  display: grid;
  min-height: 172px;
  place-items: center;
  color: rgba(32, 33, 30, 0.72);
  font-size: 70px;
  font-weight: 800;
}

.activity-body {
  display: flex;
  flex: 1;
  flex-direction: column;
  padding: 18px;
}

.activity-body h3 {
  min-height: 48px;
  margin: 0;
  font-size: 17px;
  line-height: 1.35;
  letter-spacing: -0.05em;
}

.activity-meta {
  display: grid;
  gap: 8px;
  margin: 17px 0;
  color: #777870;
  font-size: 10px;
  line-height: 1.4;
}

.activity-meta strong {
  color: #20211e;
}

.activity-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: auto;
  padding-top: 14px;
  border-top: 1px solid #dfdfd4;
}

.capacity {
  color: #777870;
  font-family: "DM Mono", monospace;
  font-size: 10px;
}

.detail-button {
  height: 33px;
  padding: 0 12px;
  border: 1px solid #20211e;
  border-radius: 0;
  background: #20211e;
  color: #fffef9;
  font-size: 10px;
  font-weight: 800;
}

.detail-button:hover {
  border-color: #4d5742;
  background: #4d5742;
  color: #fffef9;
}
</style>
