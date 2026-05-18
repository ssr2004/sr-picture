<template>
  <div class="picture-search-form">
    <!-- 搜索表单 -->
    <a-form layout="inline" :model="searchParams" @finish="doSearch">
      <a-form-item label="关键词" name="searchText">
        <a-input
          v-model:value="searchParams.searchText"
          placeholder="从名称和简介搜索"
          allow-clear
        />
      </a-form-item>
      <a-form-item name="category" label="分类">
        <a-auto-complete
          v-model:value="searchParams.category"
          style="min-width: 180px"
          placeholder="请输入分类"
          :options="categoryOptions"
          allow-clear
        />
      </a-form-item>
      <a-form-item name="tags" label="标签">
        <a-select
          v-model:value="searchParams.tags"
          style="min-width: 180px"
          mode="tags"
          placeholder="请输入标签"
          :options="tagOptions"
          allow-clear
        />
      </a-form-item>
      <a-form-item label="日期" name="">
        <a-range-picker
          v-model:value="dateRange"
          style="width: 400px"
          show-time
          :placeholder="['编辑开始日期', '编辑结束日期']"
          format="YYYY/MM/DD HH:mm:ss"
          :presets="rangePresets"
          @change="onRangeChange"
        />
      </a-form-item>
      <a-form-item label="名称" name="name">
        <a-input v-model:value="searchParams.name" placeholder="请输入名称" allow-clear />
      </a-form-item>
      <a-form-item label="简介" name="introduction">
        <a-input v-model:value="searchParams.introduction" placeholder="请输入简介" allow-clear />
      </a-form-item>
      <a-form-item label="宽度" name="picWidth">
        <a-input-number v-model:value="searchParams.picWidth" />
      </a-form-item>
      <a-form-item label="高度" name="picHeight">
        <a-input-number v-model:value="searchParams.picHeight" />
      </a-form-item>
      <a-form-item label="格式" name="picFormat">
        <a-input v-model:value="searchParams.picFormat" placeholder="请输入格式" allow-clear />
      </a-form-item>
      <a-form-item>
        <a-space>
          <a-button type="primary" html-type="submit" style="width: 96px">搜索</a-button>
          <a-button html-type="reset" @click="doClear">重置</a-button>
        </a-space>
      </a-form-item>
    </a-form>

    <div style="margin-bottom: 16px" />
  </div>
</template>

<script setup lang="ts">
import { listPictureTagCategoryUsingGet } from '@/api/pictureController'
import dayjs from 'dayjs'
import { onMounted, ref } from 'vue'

interface Props {
  onSearch: (searchParams: API.PictureQueryRequest) => void
}
const props = defineProps<Props>()
const loading = ref(true)
const dataList = ref([])
const total = ref(0)
//搜索条件
const searchParams = ref<API.PictureQueryRequest>({
  current: 1,
  pageSize: 12,
  sortField: 'createTime',
  sortOrder: 'descend',
})

//搜索
const doSearch = () => {
  props.onSearch?.(searchParams.value)
}

const dateRange = ref<[]>([])
//日期范围变更触发
const onRangeChange = (dates: any[], dateStrings: string[]) => {
  if (dates.length < 2) {
    searchParams.value.startEditTime = undefined
    searchParams.value.endEditTime = undefined
  } else {
    searchParams.value.startEditTime = dates[0].toDate()
    searchParams.value.endEditTime = dates[1].toDate()
  }
}
const rangePresets = ref([
  { label: '过去 7 天', value: [dayjs().add(-7, 'd'), dayjs()] },
  { label: '过去 14 天', value: [dayjs().add(-14, 'd'), dayjs()] },
  { label: '过去 30 天', value: [dayjs().add(-30, 'd'), dayjs()] },
  { label: '过去 90 天', value: [dayjs().add(-90, 'd'), dayjs()] },
])

const categoryOptions = ref<string[]>([])
const tagOptions = ref<string[]>([])

//获取标签和分类选项
const getTagCategoryOptions = async () => {
  const res = await listPictureTagCategoryUsingGet()
  if (res.data.code === 0 && res.data.data) {
    //转换成下拉选项组件接受的格式
    tagOptions.value = (res.data.data.tagList ?? []).map((data: string) => {
      return {
        value: data,
        label: data,
      }
    })
    categoryOptions.value = (res.data.data.categoryList ?? []).map((data: string) => {
      return {
        value: data,
        label: data,
      }
    })
  } else {
    console.error('加载选项失败，' + res.data.message)
  }
}
onMounted(() => {
  getTagCategoryOptions()
})

//清理
const doClear = () => {
  //取消所有对象的值
  Object.keys(searchParams.value).forEach((key) => {
    searchParams.value[key as keyof API.PictureQueryRequest] = undefined
  })
  dateRange.value = []
  props.onSearch?.(searchParams.value)
}
</script>
<style scoped>
.picture-search-form .ant-form-item {
  margin-top: 16px;
}
</style>
