import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useLoginUserStore = defineStore('loginUser', () => {

    const loginUser = ref<any>({
        userName: '未登录',
    });

    async function fetchLoginUser() {
        //todo
        // setTimeout(() => {
        //     loginUser.value = {
        //         id: 1,
        //         username: '测试用户',
        //     }
        // }, 3000);
    }

    function setLoginUser(newLoginUser: any) {
        loginUser.value = newLoginUser;
    }
    return { loginUser, fetchLoginUser, setLoginUser }
})
