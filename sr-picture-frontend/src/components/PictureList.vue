<template>
  <div class="picture-list" v-if="dataList.length > 0">
    <div
      v-for="picture in dataList"
      :key="picture.id"
      class="picture-card"
      @click="doClickPicture(picture)"
    >
      <a-card hoverable>
        <template #cover>
          <img
            :alt="picture.name"
            :src="picture.thumbnailUrl ?? picture.url"
            loading="lazy"
          />
        </template>
        <div class="card-info">
          <a-card-meta :title="picture.name">
            <template #description>
              <a-flex>
                <a-tag color="green">
                  {{ picture.category ?? '默认' }}
                </a-tag>
                <a-tag v-for="tag in picture.tags" :key="tag">
                  {{ tag }}
                </a-tag>
              </a-flex>
            </template>
          </a-card-meta>
        </div>
        <template v-if="showOp" #actions>
          <share-alt-outlined @click="(e) => doShare(picture, e)" />
          <edit-outlined v-if="canEdit" @click="(e) => doEdit(picture, e)" />
          <delete-outlined v-if="canDelete" @click="(e) => doDelete(picture, e)" />
        </template>
      </a-card>
    </div>
  </div>
  <a-empty v-else-if="!loading" description="暂无图片" />
  <ShareModel ref="shareModelRef" :link="shareLink" />
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { EditOutlined, DeleteOutlined, ShareAltOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { deletePictureUsingPost } from '@/api/pictureController'
import ShareModel from './ShareModel.vue'
import { ref } from 'vue'

interface Props {
  dataList?: API.PictureVO[]
  loading?: boolean
  showOp?: boolean
  canEdit?: boolean
  canDelete?: boolean
  onReload?: () => void
}
const props = withDefaults(defineProps<Props>(), {
  dataList: () => [],
  loading: false,
  showOp: false,
  canEdit: false,
  canDelete: false,
})
//跳转至图片详情
const router = useRouter()
const doClickPicture = (picture: API.PictureVO) => {
  router.push({
    path: `/picture/${picture.id}`,
  })
}

//编辑
const doEdit = (picture: API.PictureVO, e: Event) => {
  e.stopPropagation()
  router.push({
    path: '/add_picture',
    query: {
      id: picture.id,
      spaceId: picture.spaceId,
    },
  })
}

//删除
const doDelete = async (picture: API.PictureVO, e: Event) => {
  e.stopPropagation()
  const id = picture.id
  if (!id) {
    return
  }
  const res = await deletePictureUsingPost({ id })
  if (res.data.code === 0) {
    message.success('删除成功')
    //让外层刷新
    props?.onReload()
  } else {
    message.error('删除失败：' + res.data.message)
  }
}

//分享弹窗引用
const shareModelRef = ref()
//分享链接
const shareLink = ref<string>()

//分享
const doShare = (picture: API.PictureVO, e: Event) => {
  e.stopPropagation()
  shareLink.value = `${window.location.protocol}//${window.location.host}/picture/${picture.id}`
  if (shareModelRef.value) {
    shareModelRef.value.openModel()
  }
}
</script>

<style scoped>
.picture-list {
  column-count: 5;
  column-gap: 16px;
}

@media (max-width: 1600px) {
  .picture-list {
    column-count: 4;
  }
}

@media (max-width: 1200px) {
  .picture-list {
    column-count: 3;
  }
}

@media (max-width: 992px) {
  .picture-list {
    column-count: 2;
  }
}

@media (max-width: 576px) {
  .picture-list {
    column-count: 1;
  }
}

.picture-card {
  break-inside: avoid;
  margin-bottom: 16px;
  cursor: pointer;
}

.picture-card :deep(.ant-card:hover) {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.picture-card :deep(.ant-card-cover img) {
  width: 100%;
  display: block;
}

.picture-card .card-info {
  opacity: 0;
  transition: opacity 0.3s ease;
}

.picture-card:hover .card-info {
  opacity: 1;
}
</style>
