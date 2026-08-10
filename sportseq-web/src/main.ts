import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import store from './store'
import '@/styles/index.scss'

const app = createApp(App)

app.use(store)
app.use(router)
app.use(ElementPlus, { locale: zhCn })

app.mount('#app')
