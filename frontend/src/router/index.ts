import { createRouter, createWebHistory } from 'vue-router'
import { getCurrentUser } from '../auth/session'
import LoginView from '../views/LoginView.vue'
import ProjectProgressView from '../views/ProjectProgressView.vue'
import ProductTaskDetailView from '../views/ProductTaskDetailView.vue'
import RussiaWbView from '../views/RussiaWbView.vue'
import WorkbenchView from '../views/WorkbenchView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { guestOnly: true }
    },
    {
      path: '/',
      name: 'amazon-workbench',
      component: WorkbenchView,
      meta: { requiresAuth: true }
    },
    {
      path: '/wb',
      name: 'russia-wb',
      component: RussiaWbView,
      meta: { requiresAuth: true }
    },
    {
      path: '/project-progress',
      name: 'project-progress',
      component: ProjectProgressView,
      meta: { requiresAuth: true }
    },
    {
      path: '/product-task/:id',
      name: 'product-task-detail',
      component: ProductTaskDetailView,
      meta: { requiresAuth: true }
    }
  ]
})

router.beforeEach((to) => {
  const user = getCurrentUser()

  if (to.meta.requiresAuth && !user) {
    return {
      path: '/login',
      query: { redirect: to.fullPath }
    }
  }

  if (to.meta.guestOnly && user) {
    return '/'
  }

  return true
})

export default router
