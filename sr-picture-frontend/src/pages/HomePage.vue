<template>
  <div id="homePage">
    <div class="search-bar">
      <a-input-search
        v-model:value="searchParams.searchText"
        placeholder="关键字 搜索"
        enter-button="搜索"
        size="large"
        @search="doSearch"
        allow-clear
      />
      <div class="ai-search-bar">
        <a-input-search
          v-model:value="aiSearchQuery"
          placeholder="AI 语义搜索：试试输入「海边风景」「蓝色背景的证件照」"
          enter-button="AI 搜索"
          size="large"
          :loading="aiLoading"
          @search="doAISearch"
          allow-clear
        />
      </div>
    </div>

    <!-- 分类 + 标签 -->
    <a-tabs v-model:activeKey="selectedCategory" @change="doSearch">
      <a-tab-pane key="all" tab="全部" />
      <a-tab-pane v-for="category in categoryList" :key="category" :tab="category" />
    </a-tabs>
    <div class="tag-bar">
      <span style="margin-right: 10px">标签：</span>
      <a-space :size="[0, 8]" wrap>
        <a-checkable-tag
          v-for="(tag, index) in tagList"
          :key="tag"
          v-model:checked="selectedTagList[index]"
          @change="doSearch"
        >
          {{ tag }}
        </a-checkable-tag>
      </a-space>
    </div>
    <!-- 图片列表 -->
    <PictureList :dataList="dataList" :loading="loading" />
    <a-pagination
      style="text-align: right"
      v-model:current="searchParams.current"
      v-model:pageSize="searchParams.pageSize"
      :total="total"
      @change="onPageChange"
    />
  </div>
</template>

<script setup lang="ts">
import {
  listPictureTagCategoryUsingGet,
  listPictureVoByPageUsingPost,
} from '@/api/pictureController'
import { searchByAiUsingPost } from '@/api/pictureSearchController'
import PictureList from '@/components/PictureList.vue'
import { message } from 'ant-design-vue'
import { computed, onMounted, reactive, ref } from 'vue'

//数据
const dataList = ref([])
const total = ref(0)
const loading = ref(true)

//搜索条件
const searchParams = reactive<API.PictureQueryRequest>({
  current: 1,
  pageSize: 12,
  sortField: 'createTime',
  sortOrder: 'descend',
})

//分页参数
const onPageChange = (page: number, pageSize: number) => {
  searchParams.current = page
  searchParams.pageSize = pageSize
  fetchData()
}
//获取数据
const fetchData = async () => {
  loading.value = true
  const params = {
    ...searchParams,
    tags: [] as string[],
  }
  if (selectedCategory.value !== 'all') {
    params.category = selectedCategory.value
  }
  selectedTagList.value.forEach((userTag, index) => {
    if (userTag) {
      params.tags.push(tagList.value[index])
    }
  })
  const res = await listPictureVoByPageUsingPost(params)
  if (res.data.data) {
    dataList.value = res.data.data.records ?? []
    total.value = res.data.data.total ?? 0
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
  loading.value = false
}

//页面加载时请求一次
onMounted(() => {
  fetchData()
})

const doSearch = () => {
  //重置页码
  searchParams.current = 1
  aiSearchQuery.value = ''
  isAIMode.value = false
  fetchData()
}

// AI 语义搜索
const aiSearchQuery = ref('')
const aiLoading = ref(false)
const isAIMode = ref(false)

const doAISearch = async () => {
  if (!aiSearchQuery.value || aiSearchQuery.value.trim() === '') {
    isAIMode.value = false
    searchParams.current = 1
    fetchData()
    return
  }
  aiLoading.value = true
  isAIMode.value = true
  try {
    const res = await searchByAiUsingPost({
      query: aiSearchQuery.value,
    })
    if (res.data.code === 0 && res.data.data) {
      dataList.value = res.data.data
      total.value = res.data.data.length
    } else {
      message.error('AI 搜索失败：' + res.data.message)
    }
  } catch (e) {
    message.error('AI 搜索请求失败')
  } finally {
    aiLoading.value = false
  }
}

const categoryList = ref<string[]>([])
const tagList = ref<string[]>([])
const selectedCategory = ref<string>('all')
const selectedTagList = ref<string[]>([])

const getTagCategoryOptions = async () => {
  const res = await listPictureTagCategoryUsingGet()
  if (res.data.code === 0 && res.data.data) {
    //转换为下拉选项组件接受的格式
    tagList.value = res.data.data.tagList ?? []
    categoryList.value = res.data.data.categoryList ?? []
  } else {
    message.error('获取标签分类失败：' + res.data.message)
  }
}

/**
  warn：因为主页最后一列有空位，下面代码是新增的，  updatePageSize()
  fetchData()以及window计算！！！！！
 */
const getCurrentColumns = () => {
  const width = window.innerWidth
  if (width < 576) return 1
  if (width < 768) return 2
  if (width < 992) return 3
  if (width < 1200) return 4
  if (width < 1600) return 5
  return 6
}
const updatePageSize = () => {
  const cols = getCurrentColumns()
  searchParams.pageSize = cols * 3
}
window.addEventListener('resize', () => {
  const oldPageSize = searchParams.pageSize
  updatePageSize()
  if (searchParams.pageSize !== oldPageSize) {
    fetchData()
  }
})

onMounted(() => {
  getTagCategoryOptions()
  updatePageSize()
  fetchData()
})
</script>

<style scoped>
#homePage {
  margin-bottom: 16px;
}
#homePage .search-bar {
  max-width: 560px;
  margin: 0 auto 16px;
}
#homePage .ai-search-bar {
  margin-top: 12px;
}
#homePage .tag-bar {
  margin-bottom: 16px;
}
</style>
