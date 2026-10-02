<template>
  <main class="auth-page">
    <section class="auth-visual">
      <div class="auth-brand">
        <span class="brand-mark">CA</span>
        <span>CampusAgent</span>
      </div>

      <div class="auth-visual-copy">
        <span class="auth-kicker">Start your campus story</span>
        <h1>从今天开始，<em>加入正在发生的事。</em></h1>
        <p>建立你的校园身份，报名感兴趣的活动，也让更多同学遇见你的想法。</p>
      </div>

      <div class="auth-note">
        <span class="auth-note-index">02</span>
        <div>
          <strong>MAKE ROOM<br />FOR SOMETHING NEW.</strong>
          <p>每一次报名，都是一次新的连接。</p>
        </div>
      </div>

      <div class="auth-visual-footer">
        <span>CREATE / PARTICIPATE / CONNECT</span>
        <span>CONNECTED TO / CAMPUS</span>
      </div>
    </section>

    <section class="auth-panel">
      <div class="auth-panel-inner register-panel-inner">
        <header class="auth-header">
          <span class="auth-kicker">New member</span>
          <h2 class="auth-title">创建校园账号</h2>
          <p class="auth-subtitle">完善信息后，就可以开始探索活动。</p>
        </header>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="handleSubmit">
          <div class="form-section-label">账号信息</div>
          <div class="form-grid">
            <el-form-item label="用户名" prop="username">
              <el-input v-model.trim="form.username" placeholder="请输入用户名" size="large" />
            </el-form-item>

            <el-form-item label="密码" prop="password">
              <el-input
                v-model="form.password"
                placeholder="至少 6 位"
                show-password
                size="large"
                type="password"
              />
            </el-form-item>
          </div>

          <div class="form-section-label">校园身份</div>
          <div class="form-grid">
            <el-form-item label="姓名" prop="realName">
              <el-input v-model.trim="form.realName" placeholder="请输入姓名" size="large" />
            </el-form-item>

            <el-form-item label="学号" prop="studentId">
              <el-input v-model.trim="form.studentId" placeholder="请输入学号" size="large" />
            </el-form-item>
          </div>

          <el-form-item label="学院" prop="college">
            <el-input v-model.trim="form.college" placeholder="请输入所在学院" size="large" />
          </el-form-item>

          <el-button
            class="submit-button"
            :loading="loading"
            native-type="submit"
            size="large"
            type="primary"
          >
            创建我的账号
          </el-button>
        </el-form>

        <p class="auth-footer">
          已有账号？
          <router-link to="/login">返回登录</router-link>
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

// 后端注册 DTO 使用 realName，而不是 real_name。
const form = reactive({
  username: '',
  password: '',
  realName: '',
  studentId: '',
  college: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  studentId: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  college: [{ required: true, message: '请输入学院', trigger: 'blur' }]
}

async function handleSubmit() {
  if (loading.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.register({ ...form })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } finally {
    loading.value = false
  }
}
</script>
