const SOCIAL_LOGIN_URLS = {
  kakao: '/api/auth/kakao?prompt=login',
  naver: '/api/auth/naver',
}

export const useSocialLogin = () => {
  const startSocialLogin = (provider) => {
    window.location.href = SOCIAL_LOGIN_URLS[provider]
  }

  const startKakaoLogin = () => startSocialLogin('kakao')
  const startNaverLogin = () => startSocialLogin('naver')

  return { startKakaoLogin, startNaverLogin }
}
