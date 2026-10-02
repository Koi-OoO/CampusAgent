<template>
  <div class="home-page">
    <header class="topbar">
      <a class="brand" href="#" @click.prevent="resetFilters">
        <span class="brand-mark">CA</span>
        <span>CampusAgent</span>
      </a>

      <nav class="nav-links" aria-label="主导航">
        <a class="active" href="#activities">发现活动</a>
        <a href="#my-signups">我的报名</a>
        <a href="#profile">个人资料</a>
      </nav>

      <div class="user-actions">
        <div class="avatar">{{ avatarText }}</div>
        <div class="user-copy">
          <strong>{{ displayName }}</strong>
          <span>{{ userInfo?.college || '校园成员' }}</span>
        </div>
        <el-button
          class="logout-button"
          :loading="logoutLoading"
          title="退出登录"
          @click="handleLogout"
        >
          <span class="logout-icon" aria-hidden="true">↪</span>
          <span>退出登录</span>
        </el-button>
      </div>
    </header>

    <main class="home-main">
      <section class="hero">
        <div class="hero-copy">
          <div class="eyebrow">Campus activity network / 2026</div>
          <h1>把校园生活，<em>安排得刚刚好。</em></h1>
          <p>发现讲座、社团、志愿服务与更多校园活动，今天也有新的事情等你加入。</p>
        </div>
        <div class="hero-stamp">
          <strong>{{ total }}</strong>
          <span>VISIBLE ACTIVITIES<br />RIGHT NOW</span>
        </div>
      </section>

      <section class="toolbar" aria-label="活动筛选">
        <label class="search-box">
          <span>⌕</span>
          <el-input
            v-model="keyword"
            clearable
            placeholder="搜索活动名称、地点或分类"
            @keyup.enter="loadActivities"
            @clear="loadActivities"
          />
        </label>
        <div class="filter-row">
          <button
            class="filter-button"
            :class="{ active: !selectedCategory }"
            @click="selectCategory(null)"
          >
            全部
          </button>
          <button
            v-for="category in categories"
            :key="category.id"
            class="filter-button"
            :class="{ active: selectedCategory === category.id }"
            @click="selectCategory(category.id)"
          >
            {{ category.name }}
          </button>
        </div>
      </section>

      <section id="activities" class="activity-section">
        <div class="section-heading">
          <div>
            <div class="eyebrow">01 / curated for you</div>
            <h2>正在发生的活动</h2>
          </div>
          <span>{{ total }} 个活动 · 第 {{ page }} / {{ pages || 1 }} 页</span>
        </div>

        <el-skeleton :loading="activityLoading" animated :count="4">
          <template #template>
            <div class="activity-grid">
              <div v-for="item in 4" :key="item" class="skeleton-card">
                <el-skeleton-item variant="image" />
                <div class="skeleton-content">
                  <el-skeleton-item variant="h3" />
                  <el-skeleton-item variant="text" />
                  <el-skeleton-item variant="text" />
                </div>
              </div>
            </div>
          </template>

          <div v-if="activities.length" class="activity-grid">
            <ActivityCard
              v-for="(activity, index) in activities"
              :key="activity.id"
              :activity="activity"
              :index="index"
              @detail="openDetail"
            />
          </div>

          <el-empty v-else description="暂时没有匹配的活动" />
        </el-skeleton>

        <div v-if="pages > 1" class="pagination-wrap">
          <el-pagination
            v-model:current-page="page"
            :page-size="pageSize"
            :total="total"
            background
            layout="prev, pager, next"
            @current-change="loadActivities"
          />
        </div>
      </section>

      <section id="my-signups" class="lower-grid">
        <div class="section-panel">
          <div class="panel-heading">
            <div>
              <div class="eyebrow">02 / getting involved</div>
              <h2>我的报名</h2>
            </div>
            <span>{{ mySignups.length }} 条记录</span>
          </div>
          <MySignupList :signups="mySignups" :status-map="signupStatusMap" />
        </div>

        <div id="profile" class="section-panel profile-panel">
          <div class="panel-heading">
            <div>
              <div class="eyebrow">03 / your campus identity</div>
              <h2>个人资料</h2>
            </div>
            <span>{{ roleText }}</span>
          </div>
          <div class="profile-grid">
            <div><span>姓名</span><strong>{{ userInfo?.realName || '未填写' }}</strong></div>
            <div><span>学号</span><strong>{{ userInfo?.studentId || '未填写' }}</strong></div>
            <div><span>学院</span><strong>{{ userInfo?.college || '未填写' }}</strong></div>
            <div><span>手机号</span><strong>{{ userInfo?.phone || '未填写' }}</strong></div>
          </div>
        </div>
      </section>
    </main>

    <ActivityDetailDialog
      v-model="detailVisible"
      :detail="detail"
      :signup-loading="signupLoading"
      @cancel="handleCancelSignup"
      @signup="handleSignup"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { ElMessage, ElMessageBox } from 'element-plus'

import { getActivityDetail, getActivityList, getCategories } from '@/api/activity'
import { cancelSignup, getMySignups, signup } from '@/api/signup'
import ActivityCard from '@/components/ActivityCard.vue'
import ActivityDetailDialog from '@/components/ActivityDetailDialog.vue'
import MySignupList from '@/components/MySignupList.vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const { userInfo } = storeToRefs(userStore)
const logoutLoading = ref(false)
const activityLoading = ref(false)
const signupLoading = ref(false)
const detailVisible = ref(false)
const detail = ref(null)
const activities = ref([])
const categories = ref([])
const mySignups = ref([])
const keyword = ref('')
const selectedCategory = ref(null)
const page = ref(1)
const pageSize = ref(8)
const total = ref(0)
const pages = ref(1)

const roleMap = {
  USER: '普通用户',
  ADMIN: '管理员',
  SUPER_ADMIN: '超级管理员'
}
const signupStatusMap = {
  0: '待签到',
  1: '已签到',
  2: '已取消',
  3: '已缺席'
}

const displayName = computed(() => userInfo.value?.username || '同学')
const avatarText = computed(() => (userInfo.value?.realName || displayName.value).slice(0, 1))
const roleText = computed(() => roleMap[userInfo.value?.role] || userInfo.value?.role || '-')
onMounted(async () => {
  try {
    await Promise.all([loadProfile(), loadCategories(), loadActivities(), loadMySignups()])
  } catch {
    // 请求拦截器负责统一提示接口错误。
  }
})

async function loadProfile() {
  if (!userInfo.value) await userStore.fetchCurrentUser()
}

async function loadCategories() {
  categories.value = await getCategories()
}

async function loadActivities() {
  activityLoading.value = true
  try {
    const result = await getActivityList({
      categoryId: selectedCategory.value || undefined,
      keyword: keyword.value.trim() || undefined,
      status: 3,
      page: page.value,
      size: pageSize.value
    })
    activities.value = result?.records || []
    total.value = result?.total || 0
    pages.value = result?.pages || 1
  } finally {
    activityLoading.value = false
  }
}

async function loadMySignups() {
  const result = await getMySignups({ page: 1, size: 4 })
  mySignups.value = result?.records || []
}

function selectCategory(categoryId) {
  selectedCategory.value = categoryId
  page.value = 1
  loadActivities()
}

function resetFilters() {
  keyword.value = ''
  selectedCategory.value = null
  page.value = 1
  loadActivities()
}

async function openDetail(activityId) {
  detailVisible.value = true
  detail.value = null
  try {
    detail.value = await getActivityDetail(activityId)
  } catch {
    detailVisible.value = false
  }
}

async function handleSignup() {
  if (!detail.value || signupLoading.value) return
  signupLoading.value = true
  try {
    await signup({ activityId: detail.value.id })
    ElMessage.success('报名成功，活动已加入我的报名')
    await refreshDetail()
    await loadActivities()
    await loadMySignups()
  } finally {
    signupLoading.value = false
  }
}

async function handleCancelSignup() {
  if (!detail.value || signupLoading.value) return
  try {
    await ElMessageBox.confirm('确认取消这次活动报名吗？', '取消报名', {
      confirmButtonText: '确认取消',
      cancelButtonText: '暂不取消',
      type: 'warning'
    })
  } catch {
    return
  }

  signupLoading.value = true
  try {
    await cancelSignup(detail.value.id)
    ElMessage.success('已取消报名，可以重新报名')
    await refreshDetail()
    await loadActivities()
    await loadMySignups()
  } finally {
    signupLoading.value = false
  }
}

async function refreshDetail() {
  if (detail.value?.id) detail.value = await getActivityDetail(detail.value.id)
}

async function handleLogout() {
  logoutLoading.value = true
  try {
    await userStore.logout()
  } finally {
    logoutLoading.value = false
  }
}
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  color: #20211e;
  background:
    radial-gradient(circle at 8% 0%, rgba(213, 244, 77, 0.18), transparent 24%),
    linear-gradient(135deg, rgba(185, 231, 245, 0.16), transparent 34%),
    #f6f5ef;
}

.topbar {
  display: flex;
  align-items: center;
  gap: 24px;
  min-height: 72px;
  padding: 0 max(24px, calc((100% - 1360px) / 2));
  border-bottom: 1px solid #dfdfd4;
  background: rgba(255, 254, 249, 0.84);
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #20211e;
  font-size: 17px;
  font-weight: 800;
  letter-spacing: -0.04em;
  text-decoration: none;
}

.brand-mark {
  display: grid;
  width: 35px;
  height: 35px;
  place-items: center;
  border: 2px solid #20211e;
  border-radius: 50%;
  background: #d5f44d;
  font-family: "DM Mono", monospace;
  font-size: 12px;
  transform: rotate(-8deg);
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 28px;
  margin-left: auto;
  color: #777870;
  font-size: 12px;
  font-weight: 800;
}

.nav-links a {
  color: inherit;
  text-decoration: none;
}

.nav-links a.active,
.nav-links a:hover {
  color: #20211e;
}

.user-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-left: 18px;
  border-left: 1px solid #dfdfd4;
}

.avatar {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border-radius: 50%;
  background: #ffb36a;
  font-size: 12px;
  font-weight: 800;
}

.user-copy {
  display: grid;
  gap: 2px;
  min-width: 90px;
  font-size: 11px;
}

.user-copy span {
  overflow: hidden;
  color: #777870;
  font-size: 9px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.logout-button {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 36px;
  margin-left: 6px;
  padding: 0 12px;
  border: 1px solid #c9c9bd;
  border-radius: 0;
  background: transparent;
  color: #5f6258;
  font-size: 11px;
  font-weight: 800;
}

.logout-button:hover,
.logout-button:focus {
  border-color: #a7593b;
  background: #fff1e9;
  color: #a7593b;
}

.logout-icon {
  display: inline-grid;
  width: 15px;
  height: 15px;
  place-items: center;
  font-size: 18px;
  line-height: 1;
}

.home-main {
  width: min(1360px, calc(100% - 48px));
  margin: 0 auto;
  padding: 58px 0 70px;
}

.hero {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 28px;
  padding-bottom: 52px;
}

.hero-copy {
  max-width: 820px;
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

.hero h1 {
  max-width: 820px;
  margin: 20px 0 24px;
  font-size: clamp(48px, 7vw, 92px);
  line-height: 0.95;
  letter-spacing: -0.08em;
}

.hero h1 em {
  color: #87971c;
  font-style: normal;
}

.hero p {
  max-width: 520px;
  color: #777870;
  font-size: 15px;
  line-height: 1.75;
}

.hero-stamp {
  display: grid;
  width: 172px;
  height: 172px;
  flex: 0 0 auto;
  place-items: center;
  border: 1px solid #20211e;
  border-radius: 50%;
  background: #b9e7f5;
  text-align: center;
  transform: rotate(8deg);
}

.hero-stamp strong {
  display: block;
  font-size: 44px;
  line-height: 0.9;
  letter-spacing: -0.08em;
}

.hero-stamp span {
  display: block;
  margin-top: 10px;
  font-family: "DM Mono", monospace;
  font-size: 8px;
  line-height: 1.5;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 0;
  border-top: 1px solid #dfdfd4;
  border-bottom: 1px solid #dfdfd4;
}

.search-box {
  display: flex;
  align-items: center;
  flex: 1;
  gap: 8px;
  min-width: 230px;
  padding: 0 13px;
  border: 1px solid #dfdfd4;
  background: #fffef9;
}

.search-box > span {
  color: #777870;
  font-size: 19px;
}

.search-box .el-input {
  flex: 1;
}

.search-box :deep(.el-input__wrapper) {
  min-height: 42px;
  border-radius: 0;
  box-shadow: none;
}

.filter-row {
  display: flex;
  gap: 8px;
  overflow-x: auto;
}

.filter-button {
  height: 42px;
  flex: 0 0 auto;
  padding: 0 15px;
  border: 1px solid #dfdfd4;
  background: transparent;
  color: #777870;
  font-size: 11px;
  font-weight: 800;
}

.filter-button.active,
.filter-button:hover {
  border-color: #20211e;
  background: #20211e;
  color: #fffef9;
}

.section-heading,
.panel-heading {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 18px;
}

.section-heading {
  padding: 48px 0 20px;
}

.section-heading h2,
.panel-heading h2 {
  margin: 12px 0 0;
  font-size: 28px;
  letter-spacing: -0.07em;
}

.section-heading > span,
.panel-heading > span {
  color: #777870;
  font-family: "DM Mono", monospace;
  font-size: 10px;
}

.activity-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.skeleton-card {
  overflow: hidden;
  border: 1px solid #dfdfd4;
  background: #fffef9;
}

.skeleton-card :deep(.el-skeleton__item) {
  border-radius: 0;
}

.skeleton-card :deep(.el-skeleton__item:first-child) {
  width: 100%;
  height: 172px;
}

.skeleton-content {
  display: grid;
  gap: 13px;
  padding: 20px;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  padding-top: 28px;
}

.pagination-wrap :deep(.el-pagination.is-background .el-pager li) {
  border-radius: 0;
  background: #fffef9;
}

.pagination-wrap :deep(.el-pagination.is-background .el-pager li.is-active) {
  background: #20211e;
}

.lower-grid {
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 16px;
  padding-top: 54px;
}

.section-panel {
  border: 1px solid #dfdfd4;
  background: rgba(255, 254, 249, 0.8);
  box-shadow: 0 18px 46px rgba(39, 42, 35, 0.06);
}

.panel-heading {
  padding: 21px 22px;
  border-bottom: 1px solid #dfdfd4;
}

.profile-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1px;
  background: #dfdfd4;
}

.profile-grid > div {
  min-height: 112px;
  padding: 18px;
  background: #fffef9;
}

.profile-grid > div:nth-child(2) {
  background: #d5f44d;
}

.profile-grid > div:nth-child(3) {
  background: #ffb36a;
}

.profile-grid > div:nth-child(4) {
  background: #b9e7f5;
}

.profile-grid span {
  display: block;
  color: rgba(32, 33, 30, 0.62);
  font-family: "DM Mono", monospace;
  font-size: 9px;
  text-transform: uppercase;
}

.profile-grid strong {
  display: block;
  margin-top: 23px;
  overflow: hidden;
  font-size: 15px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 1050px) {
  .activity-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .hero h1 {
    font-size: clamp(48px, 8vw, 76px);
  }
}

@media (max-width: 760px) {
  .topbar {
    flex-wrap: wrap;
    padding: 14px 18px;
  }

  .nav-links {
    display: none;
  }

  .user-actions {
    margin-left: auto;
    padding-left: 0;
    border-left: 0;
  }

  .logout-button {
    padding: 0 10px;
  }

  .home-main {
    width: min(100% - 28px, 620px);
    padding-top: 38px;
  }

  .hero {
    align-items: start;
    flex-direction: column;
    padding-bottom: 36px;
  }

  .hero-stamp {
    width: 128px;
    height: 128px;
  }

  .hero-stamp strong {
    font-size: 34px;
  }

  .toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .lower-grid {
    grid-template-columns: 1fr;
  }

}

@media (max-width: 520px) {
  .activity-grid {
    grid-template-columns: 1fr;
  }

  .section-heading,
  .panel-heading {
    align-items: start;
    flex-direction: column;
  }

  .profile-grid {
    grid-template-columns: 1fr;
  }
}
</style>
