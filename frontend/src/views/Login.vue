<template>
  <main class="auth-page">
    <section class="auth-visual">
      <div class="auth-brand">
        <span class="brand-mark">CA</span>
        <span>CampusAgent</span>
      </div>

      <div class="auth-visual-copy">
        <span class="auth-kicker">Campus activity network / 2026</span>
        <h1>把校园生活，<em>安排得刚刚好。</em></h1>
        <p>发现讲座、社团、志愿服务与更多校园活动，今天也有新的事情等你加入。</p>
      </div>

      <div class="auth-stamp" aria-hidden="true">
        <strong>24</strong>
        <span>ACTIVE MOMENTS<br />THIS WEEK</span>
      </div>

      <div class="auth-visual-footer">
        <span>01 / WELCOME BACK</span>
        <span>CONNECTED TO / CAMPUS</span>
      </div>
    </section>

    <section class="auth-panel">
      <div class="auth-panel-inner">
        <header class="auth-header">
          <span class="auth-kicker">Member access</span>
          <h2 class="auth-title">欢迎回来</h2>
          <p class="auth-subtitle">登录后继续探索校园里的新鲜事。</p>
        </header>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="handleSubmit">
          <el-form-item label="用户名" prop="username">
            <el-input v-model.trim="form.username" placeholder="请输入用户名" size="large" />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              placeholder="请输入密码"
              show-password
              size="large"
              type="password"
              @keyup.enter="handleSubmit"
            />
          </el-form-item>

          <el-button
            class="submit-button"
            :loading="loading"
            native-type="submit"
            size="large"
            type="primary"
          >
            登录 CampusAgent
          </el-button>
        </el-form>

        <p class="auth-footer">
          还没有账号？
          <router-link to="/register">创建校园账号</router-link>
        </p>
      </div>
    </section>
  </main>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)

// 登录表单只提交后端需要的 username 和 password。
const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleSubmit() {
  if (loading.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.login(form.username, form.password)
    ElMessage.success('登录成功')
    router.push('/')
  } finally {
    loading.value = false
  }
}
</script>
