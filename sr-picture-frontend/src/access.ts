import { message } from "ant-design-vue";
import { useLoginUserStore } from "./stores/useLoginUserStore";
import router from "./router";


//是否是第一次获取登录用户信息
let firstFetchLoginUser = true;

/**
 * 全员权限校验
 */
router.beforeEach(async (to, from, next) => {
    const loginUserStore = useLoginUserStore()
    let loginUser = loginUserStore.loginUser
    //确保页面刷新，首次加载时，能够等后端返回用户信息后再校验权限
    if (firstFetchLoginUser) {
        await loginUserStore.fetchLoginUser()
        loginUser = loginUserStore.loginUser
        firstFetchLoginUser = false;
    }
    const toUrl = to.fullPath
    if (toUrl.startsWith('/user/profile') || toUrl.startsWith('/user/password')) {
        if (!loginUser || !loginUser.id) {
            message.warning('请先登录')
            next(`/user/login?redirect=${encodeURIComponent(to.fullPath)}`)
            return
        }
    }

    if (toUrl.startsWith('/admin')) {
        if (!loginUser || loginUser.userRole !== 'admin') {
            message.error('您没有访问权限')
            next(`/user/login?redirect=${to.fullPath}`)
            return
        }
    }

    next()

})