<template>
  <a-modal v-model:visible="visible" title="分享图片" :footer="false" @cancel="closeModel">
    <h4>复制分享链接</h4>
    <a-typography-link copyable>
      {{ link }}
    </a-typography-link>
    <div style="margin-bottom: 16px" />
    <h4>手机扫码查看</h4>
    <a-qrcode :value="link" />
  </a-modal>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { defineExpose } from 'vue'

//定义组件属性类型
interface Props {
  title: string
  link: string
}
//给组件指定初始值
const props = withDefaults(defineProps<Props>(), {
  title: () => '分享',
  link: () => '',
})

//是否可见
const visible = ref(false)

//打开弹窗
const openModel = () => {
  visible.value = true
}

//关闭弹窗
const closeModel = () => {
  visible.value = false
}

defineExpose({
  openModel,
})
</script>

<style scoped></style>
