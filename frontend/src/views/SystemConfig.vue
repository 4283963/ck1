<template>
  <div class="config-page">
    <header class="config-header">
      <div class="header-left">
        <el-button :icon="ArrowLeft" @click="goBack" circle />
        <h1>系统配置</h1>
      </div>
    </header>

    <div class="config-content">
      <el-card class="config-card">
        <template #header>
          <div class="card-header">
            <el-icon :size="20" color="#409EFF"><ChatDotRound /></el-icon>
            <span>微信企业号通知配置</span>
            <el-tag :type="configured ? 'success' : 'warning'" size="small" effect="dark">
              {{ configured ? '已配置' : '未配置' }}
            </el-tag>
          </div>
        </template>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-width="180px"
          class="config-form">
          <el-form-item label="企业号 CorpId" prop="corpId">
            <el-input
              v-model="form.corpId"
              placeholder="请输入企业号 CorpId"
              clearable />
          </el-form-item>

          <el-form-item label="应用 AgentId" prop="agentId">
            <el-input
              v-model="form.agentId"
              placeholder="请输入应用 AgentId"
              clearable />
          </el-form-item>

          <el-form-item label="应用 Secret" prop="appSecret">
            <el-input
              v-model="form.appSecret"
              type="password"
              show-password
              placeholder="请输入应用 AppSecret"
              clearable />
          </el-form-item>

          <el-divider content-position="left">接收人配置</el-divider>

          <el-alert
            title="以下三项至少填写一项，多个值用 | 分隔，@all 表示发送给所有人"
            type="info"
            :closable="false"
            show-icon
            class="hint-alert" />

          <el-form-item label="接收人 UserId">
            <el-input
              v-model="form.toUser"
              placeholder="例: user1|user2 或 @all"
              clearable />
          </el-form-item>

          <el-form-item label="接收部门 ID">
            <el-input
              v-model="form.toParty"
              placeholder="例: 1|2|3"
              clearable />
          </el-form-item>

          <el-form-item label="接收标签 ID">
            <el-input
              v-model="form.toTag"
              placeholder="例: 1|2"
              clearable />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" :loading="saving" @click="handleSave">
              保存配置
            </el-button>
            <el-button :loading="testing" @click="handleTest">
              连接测试
            </el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card class="config-card help-card">
        <template #header>
          <div class="card-header">
            <el-icon :size="20" color="#E6A23C"><QuestionFilled /></el-icon>
            <span>配置说明</span>
          </div>
        </template>
        <div class="help-content">
          <p><strong>1. 企业号 CorpId：</strong>登录企业微信管理后台，在"我的企业"页面底部可查看</p>
          <p><strong>2. 应用 AgentId 和 Secret：</strong>在"应用管理"中创建或选择自建应用，可查看 AgentId 和 Secret</p>
          <p><strong>3. 接收人配置：</strong></p>
          <ul>
            <li>UserId：企业微信成员的账号，可在"通讯录"中查看</li>
            <li>部门 ID：可在"组织机构"中查看部门编号</li>
            <li>标签 ID：可在"通讯录-标签"中管理和查看</li>
          </ul>
          <p><strong>4. 注意事项：</strong>应用需要有"发送消息"的接口权限，接收人必须在应用可见范围内</p>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, ChatDotRound, QuestionFilled } from '@element-plus/icons-vue'
import { getWechatConfig, saveWechatConfig, testWechatConfig } from '../api'

const router = useRouter()
const formRef = ref(null)
const saving = ref(false)
const testing = ref(false)
const configured = ref(false)

const form = reactive({
  corpId: '',
  agentId: '',
  appSecret: '',
  toUser: '',
  toParty: '',
  toTag: ''
})

const rules = {
  corpId: [{ required: true, message: '请输入 CorpId', trigger: 'blur' }],
  agentId: [{ required: true, message: '请输入 AgentId', trigger: 'blur' }],
  appSecret: [{ required: true, message: '请输入 AppSecret', trigger: 'blur' }]
}

function goBack() {
  router.push('/')
}

async function loadConfig() {
  try {
    const res = await getWechatConfig()
    if (res && res.success && res.data) {
      Object.assign(form, res.data)
      configured.value = !!res.configured
    }
  } catch (e) {
    console.error('加载配置失败', e)
  }
}

async function handleSave() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  const hasReceiver = form.toUser || form.toParty || form.toTag
  if (!hasReceiver) {
    ElMessage.warning('请至少填写一项接收人配置')
    return
  }

  try {
    saving.value = true
    const res = await saveWechatConfig(form)
    if (res && res.success) {
      ElMessage.success('微信配置已保存')
      configured.value = true
    } else {
      ElMessage.error(res?.message || '保存失败')
    }
  } catch (e) {
    console.error(e)
  } finally {
    saving.value = false
  }
}

async function handleTest() {
  if (!form.corpId || !form.agentId || !form.appSecret) {
    ElMessage.warning('请先填写 CorpId、AgentId 和 AppSecret')
    return
  }

  try {
    testing.value = true
    const res = await testWechatConfig(form)
    if (res && res.success) {
      ElMessage.success('连接测试成功！')
    } else {
      ElMessage.error(res?.message || '连接测试失败')
    }
  } catch (e) {
    console.error(e)
  } finally {
    testing.value = false
  }
}

async function handleReset() {
  try {
    await ElMessageBox.confirm('确定要重置所有配置吗？', '确认重置', {
      type: 'warning'
    })
    Object.assign(form, {
      corpId: '',
      agentId: '',
      appSecret: '',
      toUser: '',
      toParty: '',
      toTag: ''
    })
    configured.value = false
  } catch (e) {
    if (e !== 'cancel') {
      console.error(e)
    }
  }
}

onMounted(() => {
  loadConfig()
})
</script>

<style scoped>
.config-page {
  width: 100%;
  min-height: 100vh;
  background: #f0f2f5;
}

.config-header {
  background: linear-gradient(135deg, #1a2a42, #2a4a72);
  padding: 16px 24px;
  display: flex;
  color: #fff;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.header-left h1 {
  font-size: 20px;
  font-weight: 600;
  margin: 0;
}

.config-content {
  padding: 24px;
  max-width: 900px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.config-card {
  border-radius: 10px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 600;
  font-size: 16px;
}

.card-header .el-tag {
  margin-left: auto;
}

.config-form {
  padding: 20px 0;
}

.hint-alert {
  margin-bottom: 20px;
}

.help-card {
  background: #fffbeb;
  border: 1px solid #faecd8;
}

.help-content {
  color: #606266;
  line-height: 2;
  font-size: 14px;
}

.help-content p {
  margin: 8px 0;
}

.help-content ul {
  margin: 8px 0 8px 20px;
}

.help-content strong {
  color: #1a2a42;
}
</style>
