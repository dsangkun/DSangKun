import { createRouter, createWebHistory } from 'vue-router'
import ProjectProgressView from '../views/ProjectProgressView.vue'
import RussiaWbView from '../views/RussiaWbView.vue'
import WorkbenchView from '../views/WorkbenchView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'amazon-workbench',
      component: WorkbenchView
    },
    {
      path: '/wb',
      name: 'russia-wb',
      component: RussiaWbView
    },
    {
      path: '/project-progress',
      name: 'project-progress',
      component: ProjectProgressView
    }
  ]
})

export default router
