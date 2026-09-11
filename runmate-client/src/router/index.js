import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import SignupView from '../views/SignupView.vue'
import LoginView from '../views/LoginView.vue'
import KakaoSignupView from '../views/KakaoSignupView.vue'
import NaverSignupView from '../views/NaverSignupView.vue'
import TermsView from '../views/TermsView.vue'

export default createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: HomeView },
    { path: '/signup', component: SignupView },
    { path: '/login', component: LoginView },
    { path: '/kakao-signup', component: KakaoSignupView },
    { path: '/naver-signup', component: NaverSignupView },
    { path: '/terms/:type/:version', component: TermsView },
  ],
  scrollBehavior: () => ({ top: 0 }),
})
