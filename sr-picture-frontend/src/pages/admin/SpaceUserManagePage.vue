<template>
  <div id="SpaceUserManagePage">
    <a-flex justify="space-between">
      <h2>空间成员管理</h2>
      <a-space>
        <a-button type="primary" @click="router.push('/add_space')">+ 创建空间</a-button>
        <a-button type="primary" ghost @click="router.push('/space_analyze?queryPublic=1')">
          分析公共图库
        </a-button>
        <a-button type="primary" ghost @click="router.push('/space_analyze?queryAll=1')">
          分析全空间
        </a-button>
      </a-space>
    </a-flex>
    <div style="margin-bottom: 16px" />
    <a-form layout="inline" :model="formData" @finish="handleSubmit">
      <a-form-item label="搜索用户" name="userId">
        <a-auto-complete
          v-model:value="searchValue"
          :options="userOptions"
          placeholder="输入用户名或账号搜索"
          style="width: 280px"
          @search="onSearch"
          @select="onSelect"
          allow-clear
        />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit" :disabled="!formData.userId">添加用户</a-button>
      </a-form-item>
    </a-form>
    <div style="margin-bottom: 16px" />

    <a-table :columns="columns" :data-source="dataList">
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'userInfo'">
          <a-space>
            <a-avatar :src="record.user?.userAvatar" />
            {{ record.user?.userName }}
          </a-space>
        </template>
        <template v-if="column.dataIndex === 'spaceRole'">
          <a-select
            v-model:value="record.spaceRole"
            :options="SPACE_ROLE_OPTIONS"
            @change="(value) => editSpaceRole(value, record)"
          />
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space wrap>
            <a-button type="link" danger @click="doDelete(record.id)">删除</a-button>
          </a-space>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import {
  addSpaceUserUsingPost,
  deleteSpaceUserUsingPost,
  editSpaceUserUsingPost,
  listSpaceUserUsingPost,
} from '@/api/spaceUserController'
import { searchUsersUsingGet } from '@/api/userController'
import { SPACE_ROLE_OPTIONS } from '@/constants/space'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// 表格列
const columns = [
  {
    title: '用户',
    dataIndex: 'userInfo',
  },
  {
    title: '角色',
    dataIndex: 'spaceRole',
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
  },
  {
    title: '操作',
    key: 'action',
  },
]

// 定义属性
interface Props {
  id: string
}

const props = defineProps<Props>()

// 数据
const dataList = ref([])

// 获取数据
const fetchData = async () => {
  const spaceId = props.id
  if (!spaceId) {
    return
  }
  const res = await listSpaceUserUsingPost({
    spaceId,
  })
  if (res.data.data) {
    dataList.value = res.data.data ?? []
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
}

// 页面加载时请求一次
onMounted(() => {
  fetchData()
})

const editSpaceRole = async (value, record) => {
  const res = await editSpaceUserUsingPost({
    id: record.id,
    spaceRole: value,
  })
  if (res.data.code === 0) {
    message.success('修改成功')
  } else {
    message.error('修改失败，' + res.data.message)
  }
}

const doDelete = async (id: string) => {
  if (!id) {
    return
  }
  const res = await deleteSpaceUserUsingPost({ id })
  if (res.data.code === 0) {
    message.success('删除成功')
    // 刷新数据
    fetchData()
  } else {
    message.error('删除失败')
  }
}

// 添加用户
const formData = reactive<API.SpaceUserAddRequest>({})
const searchValue = ref<string>('')
const userOptions = ref<{ value: string; label: string; userId: number }[]>([])

let searchTimer: any = null
const onSearch = (keyword: string) => {
  if (searchTimer) clearTimeout(searchTimer)
  if (!keyword || keyword.length < 2) {
    userOptions.value = []
    return
  }
  searchTimer = setTimeout(async () => {
    const res = await searchUsersUsingGet({ keyword })
    if (res.data.code === 0 && res.data.data) {
      userOptions.value = res.data.data.map((user: API.UserVO) => ({
        value: user.userName + ' (' + user.userAccount + ')',
        label: user.userName + ' (' + user.userAccount + ')',
        userId: user.id,
      }))
    }
  }, 300)
}

const onSelect = (_value: string, option: any) => {
  formData.userId = option.userId
}

const handleSubmit = async () => {
  const spaceId = props.id
  if (!spaceId) {
    return
  }
  const res = await addSpaceUserUsingPost({
    spaceId,
    ...formData,
  })
  if (res.data.code === 0) {
    message.success('添加成功')
    // 清空搜索状态
    searchValue.value = ''
    formData.userId = undefined
    userOptions.value = []
    // 刷新数据
    fetchData()
  } else {
    message.error('添加失败，' + res.data.message)
  }
}
</script>

<style scoped></style>
