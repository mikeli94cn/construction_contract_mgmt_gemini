<template>
  <div class="ccm-root">
    <el-container v-if="auth.isAuthenticated" class="ccm-shell">
      <el-aside width="240px" class="ccm-sidebar">
        <div class="ccm-brand">
          <div class="ccm-brand-title">CCM Enterprise</div>
          <div class="ccm-brand-subtitle">Contract Governance</div>
        </div>
        <el-menu
          :default-active="activeMenu"
          router
          background-color="#0f172a"
          text-color="#cbd5e1"
          active-text-color="#38bdf8"
          class="ccm-menu"
        >
          <el-menu-item index="/projects">
            <span>Projects & Teams</span>
          </el-menu-item>
          <el-menu-item index="/partners">
            <span>Business Partners</span>
          </el-menu-item>
          <el-menu-item index="/contracts">
            <span>Contracts & BoQ</span>
          </el-menu-item>
          <el-menu-item index="/approvals">
            <span>Approvals Inbox</span>
          </el-menu-item>
          <el-menu-item index="/reports">
            <span>Reports & Audit Trail</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <el-container>
        <el-header class="ccm-header">
          <div class="ccm-header-left">
            <span class="ccm-org-badge">{{ auth.user?.organizationCode }} — {{ auth.user?.organizationName }}</span>
          </div>
          <div class="ccm-header-right">
            <el-tag
              v-for="role in auth.user?.roles"
              :key="role"
              size="small"
              type="info"
              class="ccm-role-tag"
            >
              {{ role.replace('ROLE_', '') }}
            </el-tag>
            <span class="ccm-user-name">{{ auth.user?.fullName }}</span>
            <el-button size="small" type="danger" plain @click="handleLogout">Sign Out</el-button>
          </div>
        </el-header>

        <el-main class="ccm-main">
          <router-view />
        </el-main>
      </el-container>
    </el-container>

    <router-view v-else />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from './stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const activeMenu = computed(() => {
  if (route.path.startsWith('/reports')) return '/reports'
  if (route.path.startsWith('/approvals')) return '/approvals'
  if (route.path.startsWith('/contracts')) return '/contracts'
  if (route.path.startsWith('/partners')) return '/partners'
  return '/projects'
})

function handleLogout() {
  auth.logout()
  router.push('/login')
}
</script>

<style>
body {
  margin: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
  background-color: #f8fafc;
  color: #0f172a;
}
.ccm-shell {
  min-height: 100vh;
}
.ccm-sidebar {
  background-color: #0f172a;
  color: #f8fafc;
  display: flex;
  flex-direction: column;
}
.ccm-brand {
  padding: 20px 18px;
  border-bottom: 1px solid #1e293b;
}
.ccm-brand-title {
  font-size: 18px;
  font-weight: 700;
  color: #38bdf8;
}
.ccm-brand-subtitle {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 2px;
}
.ccm-menu {
  border-right: none !important;
}
.ccm-header {
  background: #ffffff;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}
.ccm-org-badge {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}
.ccm-header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.ccm-user-name {
  font-weight: 600;
  font-size: 14px;
  color: #1e293b;
}
.ccm-main {
  padding: 24px;
}
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.page-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
}
</style>
