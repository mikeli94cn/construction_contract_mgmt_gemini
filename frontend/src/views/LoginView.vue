<template>
  <div class="login-wrapper">
    <el-card class="login-card" shadow="always">
      <template #header>
        <div class="login-header">
          <h2>Construction Contract Management</h2>
          <p>Enterprise Governance & Cost Control Sign In</p>
        </div>
      </template>

      <el-alert
        v-if="errorMsg"
        :title="errorMsg"
        type="error"
        show-icon
        :closable="false"
        style="margin-bottom: 16px"
      />

      <el-form :model="form" label-position="top" @submit.prevent="handleLogin">
        <el-form-item label="Username" required>
          <el-input
            v-model="form.username"
            placeholder="e.g. pm.alex, cm.elena, admin"
            autocomplete="username"
          />
        </el-form-item>

        <el-form-item label="Password" required>
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="Enter your password"
            autocomplete="current-password"
          />
        </el-form-item>

        <el-button
          type="primary"
          native-type="submit"
          :loading="loading"
          style="width: 100%; margin-top: 8px"
        >
          Sign In
        </el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { extractErrorMessage } from '../api/http'

const auth = useAuthStore()
const router = useRouter()

const form = reactive({
  username: '',
  password: ''
})
const loading = ref(false)
const errorMsg = ref('')

async function handleLogin() {
  if (!form.username || !form.password) {
    errorMsg.value = 'Username and password are required'
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    await auth.login(form.username, form.password)
    router.push('/projects')
  } catch (err) {
    errorMsg.value = extractErrorMessage(err)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrapper {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
}
.login-card {
  width: 420px;
}
.login-header h2 {
  margin: 0 0 6px 0;
  font-size: 20px;
}
.login-header p {
  margin: 0;
  color: #64748b;
  font-size: 13px;
}
</style>
