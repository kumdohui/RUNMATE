import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import SignupView from '../views/SignupView.vue'
import LoginView from '../views/LoginView.vue'

export default createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: HomeView },
    { path: '/signup', component: SignupView },
    { path: '/login', component: LoginView },
  ],
  scrollBehavior: () => ({ top: 0 }),
})
