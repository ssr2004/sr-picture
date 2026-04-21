import { message } from 'ant-design-vue';
import axios from 'axios'

//创建axios实例
const myAxios = axios.create({
    baseURL: 'http://localhost:8123',
    timeout: 60000,
    withCredentials: true,
})

//全局请求拦截器
myAxios.interceptors.request.use(
    function (config) {
        // 在发送请求之前做些什么
        return config;
    },
    function (error) {
        // 对请求错误做些什么
        return Promise.reject(error);
    }
);

//全局响应拦截器
myAxios.interceptors.response.use(
    function (response) {
        const { data } = response;
        // 未登录
        if (data.code === 40100) {
            //不是获取用户信息的强求，并且用户目前不是已经在用户登录界面，则跳转到登录页面
            if (
                !response.request.repsponseURL.includes('/user/get/login') &&
                !window.location.pathname.includes('/user/login')
            ) {
                message.warning('请先登录')
                window.location.href = '/user/login?redirect=${window.location.href}';
            }
        }
        return response;
    },
    function (error) {
        // 对响应错误做点什么
        return Promise.reject(error);
    }
);

export default myAxios;