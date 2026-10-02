import { createRouter, createWebHistory } from 'vue-router'

import Home from '@/views/Home.vue'
import Login from '@/views/Login.vue'
import Register from '@/views/Register.vue'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: Home,
      meta: { requiresAuth: true }
    },
    {
      path: '/login',
      name: 'login',
      component: Login
    },
    {
      path: '/register',
      name: 'register',
      component: Register
    }
  ]
})

router.beforeEach((to) => {
  const userStore = useUserStore()

  if (to.meta.requiresAuth && !userStore.token) {
    return '/login'
  }

  if ((to.path === '/login' || to.path === '/register') && userStore.token) {
    return '/'
  }

  return true
})

export default router
