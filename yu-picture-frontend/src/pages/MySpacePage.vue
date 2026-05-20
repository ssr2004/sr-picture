<template>
  <div id="mySpace">
    <p>正在加载中，请稍候......</p>
  </div>
</template>

<script setup lang="ts">
import { listSpaceVoByPageUsingPost } from '@/api/SpaceController'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { message } from 'ant-design-vue'
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const loginUserStore = useLoginUserStore()

//检查用户是否有个人空间
const checkUserSpace = async () => {
  const loginUser = loginUserStore.loginUser
  if (!loginUser?.id) {
    router.replace('/user/login')
    return
  }
  //获取用户空间信息
  const res = await listSpaceVoByPageUsingPost({
    current: 1,
    pageSize: 1,
    userId: loginUser.id,
    spaceType: 0,
  })
  if (res.data.code === 0) {
    if (res.data.data?.records?.length > 0) {
      const space = res.data.data.records[0]
      router.replace(`/space/${space.id}`)
    } else {
      router.replace('/add_space')
      message.warn('请先创建空间')
    }
  } else {
    message.error('加载我的空间失败：' + res.data.message)
  }
}
onMounted(() => {
  checkUserSpace()
})
</script>

<style scoped></style>
