import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import LoginView from '../views/LoginView.vue'
import ProjectsView from '../views/ProjectsView.vue'
import PartnersView from '../views/PartnersView.vue'
import ContractsView from '../views/ContractsView.vue'
import ContractFormView from '../views/ContractFormView.vue'
import ContractDetailView from '../views/ContractDetailView.vue'
import ApprovalsView from '../views/ApprovalsView.vue'
import ReportsAndAuditView from '../views/ReportsAndAuditView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { public: true }
    },
    {
      path: '/',
      redirect: '/projects'
    },
    {
      path: '/projects',
      name: 'projects',
      component: ProjectsView
    },
    {
      path: '/partners',
      name: 'partners',
      component: PartnersView
    },
    {
      path: '/contracts',
      name: 'contracts',
      component: ContractsView
    },
    {
      path: '/contracts/new',
      name: 'contract-create',
      component: ContractFormView
    },
    {
      path: '/contracts/:id',
      name: 'contract-detail',
      component: ContractDetailView
    },
    {
      path: '/contracts/:id/edit',
      name: 'contract-edit',
      component: ContractFormView
    },
    {
      path: '/approvals',
      name: 'approvals',
      component: ApprovalsView
    },
    {
      path: '/reports',
      name: 'reports',
      component: ReportsAndAuditView
    }
  ]
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!to.meta.public && !auth.isAuthenticated) {
    return { name: 'login' }
  }
  if (to.name === 'login' && auth.isAuthenticated) {
    return { name: 'projects' }
  }
  return true
})

export default router
