<template>
  <div>
    <div class="page-header">
      <h1 class="page-title">Construction Projects & Team Access</h1>
      <el-button
        v-if="canCreateProject"
        type="primary"
        @click="openCreateDialog"
      >
        + New Project
      </el-button>
    </div>

    <el-card shadow="never" style="margin-bottom: 16px">
      <el-form :inline="true" @submit.prevent="loadProjects">
        <el-form-item label="Keyword">
          <el-input
            v-model="filters.keyword"
            placeholder="Code or project name"
            clearable
            @clear="loadProjects"
          />
        </el-form-item>
        <el-form-item label="Status">
          <el-select v-model="filters.status" placeholder="All Statuses" clearable style="width: 160px">
            <el-option v-for="st in projectStatuses" :key="st" :label="st" :value="st" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadProjects">Search</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="projects" border stripe style="width: 100%">
        <el-table-column prop="projectCode" label="Project Code" width="140" />
        <el-table-column prop="name" label="Project Name" min-width="220" />
        <el-table-column prop="organizationName" label="Organization" width="180" />
        <el-table-column prop="location" label="Location" width="160" />
        <el-table-column label="Approved Budget" width="170" align="right">
          <template #default="{ row }">
            <strong>{{ formatCurrency(row.approvedBudget, row.currency) }}</strong>
          </template>
        </el-table-column>
        <el-table-column label="Schedule" width="210">
          <template #default="{ row }">
            {{ row.startDate }} &rarr; {{ row.plannedEndDate }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="Status" width="120">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openMembersDialog(row)">Team Members</el-button>
            <el-button
              v-if="canCreateProject && row.status !== 'CLOSED'"
              size="small"
              type="primary"
              plain
              @click="openEditDialog(row)"
            >
              Edit
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Create / Edit Project Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? 'Edit Project' : 'Create Construction Project'"
      width="620px"
    >
      <el-form :model="projectForm" label-width="150px">
        <el-form-item label="Project Code" required>
          <el-input v-model="projectForm.projectCode" :disabled="Boolean(editingId)" placeholder="e.g. PRJ-2026-01" />
        </el-form-item>
        <el-form-item label="Project Name" required>
          <el-input v-model="projectForm.name" placeholder="Full construction project name" />
        </el-form-item>
        <el-form-item v-if="!editingId" label="Organization" required>
          <el-select v-model="projectForm.organizationId" style="width: 100%">
            <el-option v-for="org in organizations" :key="org.id" :label="`${org.code} — ${org.name}`" :value="org.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="Location" required>
          <el-input v-model="projectForm.location" placeholder="Site location / district" />
        </el-form-item>
        <el-form-item label="Currency" required>
          <el-input v-model="projectForm.currency" :disabled="Boolean(editingId)" maxlength="3" />
        </el-form-item>
        <el-form-item label="Approved Budget" required>
          <el-input-number
            v-model="projectForm.approvedBudget"
            :min="1"
            :precision="2"
            :step="100000"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="Start Date" required>
          <el-date-picker
            v-model="projectForm.startDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="Planned End Date" required>
          <el-date-picker
            v-model="projectForm.plannedEndDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="Status" required>
          <el-select v-model="projectForm.status" style="width: 100%">
            <el-option v-for="st in projectStatuses" :key="st" :label="st" :value="st" />
          </el-select>
        </el-form-item>
        <el-form-item label="Description">
          <el-input v-model="projectForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">Cancel</el-button>
        <el-button type="primary" :loading="saving" @click="saveProject">Save Project</el-button>
      </template>
    </el-dialog>

    <!-- Project Team Members Dialog -->
    <el-dialog
      v-model="membersDialogVisible"
      :title="`Project Team Members — ${selectedProject?.projectCode || ''}`"
      width="720px"
    >
      <div v-if="canCreateProject" class="assign-member-bar">
        <el-select v-model="memberForm.userId" placeholder="Select user to assign" style="width: 280px">
          <el-option
            v-for="u in usersDirectory"
            :key="u.id"
            :label="`${u.fullName} (${u.username})`"
            :value="u.id"
          />
        </el-select>
        <el-select v-model="memberForm.projectRole" style="width: 200px">
          <el-option v-for="pr in projectRoles" :key="pr" :label="pr" :value="pr" />
        </el-select>
        <el-button type="primary" :loading="assigningMember" @click="assignMember">
          Assign / Update Role
        </el-button>
      </div>

      <el-table :data="projectMembers" border stripe style="width: 100%; margin-top: 12px">
        <el-table-column prop="username" label="Username" width="130" />
        <el-table-column prop="fullName" label="Full Name" min-width="180" />
        <el-table-column prop="projectRole" label="Project Role" width="170">
          <template #default="{ row }">
            <el-tag size="small">{{ row.projectRole }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="assignedBy" label="Assigned By" width="130" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { http, extractErrorMessage } from '../api/http'
import { useAuthStore } from '../stores/auth'
import { formatCurrency } from '../utils/money'
import type {
  Organization,
  PageResponse,
  Project,
  ProjectMember,
  ProjectRole,
  ProjectStatus,
  UserProfile
} from '../types/api'

const auth = useAuthStore()
const canCreateProject = computed(() => auth.hasAnyRole('ROLE_SYSTEM_ADMIN', 'ROLE_PROJECT_MANAGER'))

const projectStatuses: ProjectStatus[] = ['PLANNING', 'ACTIVE', 'ON_HOLD', 'COMPLETED', 'CLOSED']
const projectRoles: ProjectRole[] = [
  'PROJECT_MANAGER',
  'CONTRACT_MANAGER',
  'COST_ENGINEER',
  'FINANCE_OFFICER',
  'VIEWER'
]

const loading = ref(false)
const saving = ref(false)
const projects = ref<Project[]>([])
const organizations = ref<Organization[]>([])
const usersDirectory = ref<UserProfile[]>([])

const filters = reactive({
  keyword: '',
  status: '' as ProjectStatus | ''
})

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const projectForm = reactive({
  projectCode: '',
  name: '',
  organizationId: undefined as number | undefined,
  location: '',
  currency: 'USD',
  approvedBudget: 10000000,
  startDate: '2026-01-01',
  plannedEndDate: '2027-12-31',
  status: 'ACTIVE' as ProjectStatus,
  description: ''
})

const membersDialogVisible = ref(false)
const selectedProject = ref<Project | null>(null)
const projectMembers = ref<ProjectMember[]>([])
const assigningMember = ref(false)
const memberForm = reactive({
  userId: undefined as number | undefined,
  projectRole: 'CONTRACT_MANAGER' as ProjectRole
})

function statusTagType(status: ProjectStatus) {
  switch (status) {
    case 'ACTIVE':
      return 'success'
    case 'PLANNING':
      return 'primary'
    case 'ON_HOLD':
      return 'warning'
    case 'CLOSED':
      return 'info'
    default:
      return 'info'
  }
}

async function loadProjects() {
  loading.value = true
  try {
    const params: Record<string, unknown> = { page: 0, size: 50 }
    if (filters.keyword) params.keyword = filters.keyword
    if (filters.status) params.status = filters.status
    const { data } = await http.get<PageResponse<Project>>('/projects', { params })
    projects.value = data.content
  } catch (err) {
    ElMessage.error(extractErrorMessage(err))
  } finally {
    loading.value = false
  }
}

async function loadOrganizations() {
  try {
    const { data } = await http.get<Organization[]>('/organizations')
    organizations.value = data
    if (data.length > 0 && !projectForm.organizationId) {
      projectForm.organizationId = data[0].id
    }
  } catch {
    // ignore
  }
}

function openCreateDialog() {
  editingId.value = null
  projectForm.projectCode = ''
  projectForm.name = ''
  projectForm.organizationId = organizations.value[0]?.id ?? auth.user?.organizationId
  projectForm.location = ''
  projectForm.currency = 'USD'
  projectForm.approvedBudget = 10000000
  projectForm.startDate = '2026-01-01'
  projectForm.plannedEndDate = '2027-12-31'
  projectForm.status = 'ACTIVE'
  projectForm.description = ''
  dialogVisible.value = true
}

function openEditDialog(row: Project) {
  editingId.value = row.id
  projectForm.projectCode = row.projectCode
  projectForm.name = row.name
  projectForm.organizationId = row.organizationId
  projectForm.location = row.location
  projectForm.currency = row.currency
  projectForm.approvedBudget = Number(row.approvedBudget)
  projectForm.startDate = row.startDate
  projectForm.plannedEndDate = row.plannedEndDate
  projectForm.status = row.status
  projectForm.description = row.description || ''
  dialogVisible.value = true
}

async function saveProject() {
  saving.value = true
  try {
    if (editingId.value) {
      await http.put(`/projects/${editingId.value}`, {
        name: projectForm.name,
        location: projectForm.location,
        approvedBudget: projectForm.approvedBudget,
        startDate: projectForm.startDate,
        plannedEndDate: projectForm.plannedEndDate,
        status: projectForm.status,
        description: projectForm.description
      })
      ElMessage.success('Project updated')
    } else {
      await http.post('/projects', projectForm)
      ElMessage.success('Project created')
    }
    dialogVisible.value = false
    await loadProjects()
  } catch (err) {
    ElMessage.error(extractErrorMessage(err))
  } finally {
    saving.value = false
  }
}

async function openMembersDialog(project: Project) {
  selectedProject.value = project
  membersDialogVisible.value = true
  try {
    const { data } = await http.get<ProjectMember[]>(`/projects/${project.id}/members`)
    projectMembers.value = data
    if (canCreateProject.value && usersDirectory.value.length === 0) {
      const usersRes = await http.get<UserProfile[]>('/users')
      usersDirectory.value = usersRes.data
    }
  } catch (err) {
    ElMessage.error(extractErrorMessage(err))
  }
}

async function assignMember() {
  if (!selectedProject.value || !memberForm.userId) {
    ElMessage.warning('Please select a user')
    return
  }
  assigningMember.value = true
  try {
    await http.post(`/projects/${selectedProject.value.id}/members`, {
      userId: memberForm.userId,
      projectRole: memberForm.projectRole
    })
    ElMessage.success('Project member role assigned')
    const { data } = await http.get<ProjectMember[]>(`/projects/${selectedProject.value.id}/members`)
    projectMembers.value = data
  } catch (err) {
    ElMessage.error(extractErrorMessage(err))
  } finally {
    assigningMember.value = false
  }
}

onMounted(() => {
  loadProjects()
  loadOrganizations()
})
</script>

<style scoped>
.assign-member-bar {
  display: flex;
  gap: 10px;
  align-items: center;
}
</style>
