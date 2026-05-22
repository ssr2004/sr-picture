<template>
  <div id="userPasswordPage">
    <a-card title="修改密码" :bordered="false" class="password-card">
      <a-form :model="formState" layout="vertical" @finish="handleSubmit">
        <a-form-item
          label="原密码"
          name="oldPassword"
          :rules="[
            { required: true, message: '请输入原密码' },
            { min: 8, message: '密码长度不能小于 8 位' },
          ]"
        >
          <a-input-password v-model:value="formState.oldPassword" placeholder="请输入原密码" />
        </a-form-item>

        <a-form-item
          label="新密码"
          name="newPassword"
          :rules="[
            { required: true, message: '请输入新密码' },
            { min: 8, message: '密码长度不能小于 8 位' },
          ]"
        >
          <a-input-password v-model:value="formState.newPassword" placeholder="请输入新密码" />
        </a-form-item>

        <a-form-item
          label="确认新密码"
          name="checkPassword"
          :rules="[
            { required: true, message: '请输入确认新密码' },
            { min: 8, message: '确认密码长度不能小于 8 位' },
          ]"
        >
          <a-input-password
            v-model:value="formState.checkPassword"
            placeholder="请再次输入新密码"
          />
        </a-form-item>

        <a-space>
          <a-button type="primary" html-type="submit" :loading="submitting">确认修改</a-button>
          <a-button :disabled="submitting" @click="resetForm">重置</a-button>
        </a-space>
      </a-form>
    </a-card>
  </div>
</template>

<script lang="ts" setup>
import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { updateUserPasswordUsingPost } from '@/api/userController'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore'

const submitting = ref(false)
const router = useRouter()
const loginUserStore = useLoginUserStore()

const formState = reactive<API.UserPasswordUpdateRequest>({
  oldPassword: '',
  newPassword: '',
  checkPassword: '',
})

const resetForm = () => {
  formState.oldPassword = ''
  formState.newPassword = ''
  formState.checkPassword = ''
}

const handleSubmit = async () => {
  const oldPassword = formState.oldPassword?.trim() ?? ''
  const newPassword = formState.newPassword?.trim() ?? ''
  const checkPassword = formState.checkPassword?.trim() ?? ''

  if (newPassword !== checkPassword) {
    message.error('两次输入的新密码不一致')
    return
  }

  if (oldPassword === newPassword) {
    message.warning('新密码不能与原密码相同')
    return
  }

  submitting.value = true
  try {
    const res = await updateUserPasswordUsingPost({
      oldPassword,
      newPassword,
      checkPassword,
    })
    if (res.data.code === 0 && res.data.data) {
      loginUserStore.setLoginUser({
        userName: '未登录',
      })
      message.success('密码修改成功，请重新登录')
      resetForm()
      await router.replace('/user/login')
      return
    }
    message.error('修改失败，' + res.data.message)
  } catch {
    message.error('修改失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
#userPasswordPage {
  max-width: 520px;
  margin: 0 auto;
}

.password-card {
  border-radius: 12px;
}
</style>
