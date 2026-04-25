<template>
  <div id="userProfilePage">
    <a-card title="个人中心" :bordered="false" class="profile-card">
      <a-skeleton v-if="loading" active :paragraph="{ rows: 6 }" />
      <a-form v-else :model="formState" layout="vertical" @finish="handleSubmit">
        <a-form-item label="账号">
          <a-input v-model:value="formState.userAccount" disabled />
        </a-form-item>

        <a-form-item
          label="昵称"
          name="userName"
          :rules="[{ required: true, whitespace: true, message: '请输入昵称' }]"
        >
          <a-input v-model:value="formState.userName" placeholder="请输入昵称" allow-clear />
        </a-form-item>

        <a-form-item label="头像地址" name="userAvatar">
          <a-input v-model:value="formState.userAvatar" placeholder="请输入头像 URL" allow-clear />
        </a-form-item>

        <a-form-item label="个人简介" name="userProfile">
          <a-textarea
            v-model:value="formState.userProfile"
            :rows="4"
            :maxlength="200"
            show-count
            placeholder="介绍一下你自己"
          />
        </a-form-item>

        <a-space>
          <a-button type="primary" html-type="submit" :loading="submitting">保存修改</a-button>
          <a-button :disabled="submitting" @click="resetForm">重置</a-button>
        </a-space>
      </a-form>
    </a-card>
  </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'
import { updateMyUserUsingPost } from '@/api/userController'
import { useLoginUserStore } from '@/stores/useLoginUserStore'

type ProfileFormState = {
  userAccount: string
  userName: string
  userAvatar: string
  userProfile: string
}

const loginUserStore = useLoginUserStore()
const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submitting = ref(false)

const originalProfile = ref({
  userName: '',
  userAvatar: '',
  userProfile: '',
})

const formState = reactive<ProfileFormState>({
  userAccount: '',
  userName: '',
  userAvatar: '',
  userProfile: '',
})

const fillFormFromUser = (user: API.LoginUserVO) => {
  formState.userAccount = user.userAccount ?? ''
  formState.userName = user.userName ?? ''
  formState.userAvatar = user.userAvatar ?? ''
  formState.userProfile = user.userProfile ?? ''

  originalProfile.value = {
    userName: formState.userName,
    userAvatar: formState.userAvatar,
    userProfile: formState.userProfile,
  }
}

const fetchProfile = async () => {
  loading.value = true
  try {
    await loginUserStore.fetchLoginUser()
    const loginUser = loginUserStore.loginUser
    if (!loginUser?.id) {
      message.warning('请先登录')
      await router.push(`/user/login?redirect=${encodeURIComponent(route.fullPath)}`)
      return
    }
    fillFormFromUser(loginUser)
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  const payload: API.UserUpdateSelfRequest = {}

  const userName = formState.userName.trim()
  const userAvatar = formState.userAvatar.trim()
  const userProfile = formState.userProfile.trim()

  if (userName !== originalProfile.value.userName && !userName) {
    message.warning('昵称不能为空')
    return
  }

  if (userName !== originalProfile.value.userName) {
    payload.userName = userName
  }
  if (userAvatar !== originalProfile.value.userAvatar) {
    payload.userAvatar = userAvatar
  }
  if (userProfile !== originalProfile.value.userProfile) {
    payload.userProfile = userProfile
  }

  if (Object.keys(payload).length === 0) {
    message.info('未检测到变更')
    return
  }

  submitting.value = true
  try {
    const res = await updateMyUserUsingPost(payload)
    if (res.data.code === 0) {
      message.success('个人信息已更新')
      await loginUserStore.fetchLoginUser()
      fillFormFromUser(loginUserStore.loginUser)
      return
    }
    message.error('保存失败，' + res.data.message)
  } catch {
    message.error('保存失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

const resetForm = () => {
  formState.userName = originalProfile.value.userName
  formState.userAvatar = originalProfile.value.userAvatar
  formState.userProfile = originalProfile.value.userProfile
}

onMounted(() => {
  fetchProfile()
})
</script>

<style scoped>
#userProfilePage {
  max-width: 680px;
  margin: 0 auto;
}

.profile-card {
  border-radius: 12px;
}
</style>
