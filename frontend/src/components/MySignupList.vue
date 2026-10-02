<template>
  <div v-if="signups.length" class="signup-list">
    <div v-for="signup in signups" :key="signup.signupId" class="signup-item">
      <div class="date-block">
        <strong>{{ formatActivityDay(signup.activityStartTime) }}</strong>
        <span>{{ formatActivityMonth(signup.activityStartTime) }}</span>
      </div>
      <div class="signup-copy">
        <strong>{{ signup.activityTitle }}</strong>
        <span>{{ signup.activityLocation || '地点待定' }} · {{ formatActivityTime(signup.activityStartTime) }}</span>
      </div>
      <span class="signup-status" :class="`status-${signup.signupStatus}`">
        {{ statusMap[signup.signupStatus] || '报名记录' }}
      </span>
    </div>
  </div>
  <el-empty v-else description="还没有报名记录" :image-size="64" />
</template>

<script setup>
import {
  formatActivityDay,
  formatActivityMonth,
  formatActivityTime
} from '@/utils/date'

defineProps({
  signups: {
    type: Array,
    default: () => []
  },
  statusMap: {
    type: Object,
    required: true
  }
})
</script>

<style scoped>
.signup-list {
  display: grid;
}

.signup-item {
  display: grid;
  grid-template-columns: 48px 1fr auto;
  align-items: center;
  gap: 14px;
  padding: 17px 22px;
  border-bottom: 1px solid #dfdfd4;
}

.signup-item:last-child {
  border-bottom: 0;
}

.date-block {
  padding: 8px 4px;
  background: #d9cdf9;
  text-align: center;
}

.date-block strong {
  display: block;
  font-size: 19px;
  line-height: 1;
}

.date-block span {
  display: block;
  margin-top: 4px;
  font-family: "DM Mono", monospace;
  font-size: 9px;
}

.signup-copy strong {
  display: block;
  font-size: 12px;
}

.signup-copy span {
  display: block;
  margin-top: 5px;
  color: #777870;
  font-size: 10px;
}

.signup-status {
  padding: 6px 8px;
  background: #a9e6c0;
  font-family: "DM Mono", monospace;
  font-size: 9px;
  white-space: nowrap;
}

.status-1 {
  background: #b9e7f5;
}

.status-2,
.status-3 {
  background: #dfdfd4;
}

@media (max-width: 520px) {
  .signup-item {
    grid-template-columns: 44px 1fr;
  }

  .signup-status {
    grid-column: 2;
    justify-self: start;
  }
}
</style>
