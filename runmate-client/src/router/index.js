import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import SignupView from '../views/SignupView.vue'
import LoginView from '../views/LoginView.vue'
import KakaoSignupView from '../views/KakaoSignupView.vue'
import NaverSignupView from '../views/NaverSignupView.vue'
import TermsView from '../views/TermsView.vue'
import CreatePostView from '../views/CreatePostView.vue'
import PostDetailView from '../views/PostDetailView.vue'
import AccountView from '../views/AccountView.vue'
import ProfileView from '../views/ProfileView.vue'
import ProfileEditView from '../views/ProfileEditView.vue'
import PasswordResetView from '../views/PasswordResetView.vue'
import FindLoginIdView from '../views/FindLoginIdView.vue'

export default createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: HomeView },
    { path: '/signup', component: SignupView },
    { path: '/login', component: LoginView },
    { path: '/password-reset', component: PasswordResetView },
    { path: '/find-login-id', component: FindLoginIdView },
    { path: '/kakao-signup', component: KakaoSignupView },
    { path: '/naver-signup', component: NaverSignupView },
    { path: '/terms/:type/:version', component: TermsView },
    { path: '/posts/new', component: CreatePostView },
    { path: '/posts/:postId', component: PostDetailView },
    { path: '/profile', component: ProfileView },
    { path: '/profile/edit', component: ProfileEditView },
    { path: '/settings/account', component: AccountView },
    { path: '/account', redirect: '/settings/account' },
  ],
  scrollBehavior: () => ({ top: 0 }),
})
