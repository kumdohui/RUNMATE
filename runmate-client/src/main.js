import { createApp } from 'vue'
import './style.css'
import './home.css'
import './login.css'
import App from './App.vue'
import router from './router'

createApp(App).use(router).mount('#app')
