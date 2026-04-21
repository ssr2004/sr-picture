<template>
  <div id="globalHeader">

    <a-row :wrap="false">
      <a-col flex="200px">
        <router-link to="/">
          <div class="title_bar">
            <img class="logo" src="../assets/logo.png" alt="logo" />
            <div class="title">鱼皮云图库</div>
          </div>
        </router-link>
      </a-col>
      <a-col flex="auto">
        <a-menu v-model:selectedKeys="current" mode="horizontal" :items="items" @click="doMenuClick"/>
      </a-col>
      <a-col flex="120px">
        <div class="user-login-status">
            <!-- <div v-if="loginUserStore.loginUser.id">
              {{ loginUserStore.loginUser.username ?? '无名' }}
            </div> -->
            <!-- <div v-else> -->
                 <a-button type="primary" href="/user/login">登录</a-button>
            <!-- </div> -->
     
        </div>
      </a-col>
    </a-row>
  </div>
  
</template>

<script lang="ts" setup>
import { h, ref } from 'vue';
import { HomeOutlined } from '@ant-design/icons-vue';
import type { MenuProps } from 'ant-design-vue';
import { RouterLink } from 'vue-router';
// const loginUserStore = useLoginUserStore();
// loginUserStore.fetchLoginUser();
// import { useLoginUserStore } from '@/stores/useLoginUserStore';

const items = ref<MenuProps['items']>([
  {
    key: '/',
    icon: () => h(HomeOutlined),
    label: '主页',
    title: '主页',
  },
  {
    key: '/about',
    label: '关于·',
    title: '关于',
  },
  {
    key: '/others',
    label: h('a', { href: 'https:www.codefather.cn', target: '_blank' }, '编程导航'),
    title: '编程导航',
  },
]);

import { useRouter } from "vue-router";

const router = useRouter();

//路由跳转事件
const doMenuClick = ({ key } :{ key : string}) => {
  router.push({
    path: key,
  });
};

//当前选中菜单
const current = ref<string[]>([]);
//监听路由变化，更新当前选中菜单
router.afterEach((to, from, next) => {
  current.value = [to.path];
});

</script>

<style scoped>

.title_bar {
  display: flex;
  align-items: center;
}

.title {
  font-size: 18px;
  color: black;
  margin-left: 16px;
}

.logo {

  height: 48px;
}
</style>

