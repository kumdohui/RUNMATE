<template>
  <footer class="app-footer">
    <div class="app-footer-inner">
      <div>
        <RouterLink
          class="app-footer-brand"
          to="/"
        >
          <span>R</span>
          <strong>RUNMATE</strong>
        </RouterLink>
        <p>달리는 순간을 기록하고, 서로의 길을 나누는 러닝 커뮤니티</p>
      </div>
      <div class="app-footer-info">
        <nav aria-label="서비스 정책">
          <RouterLink to="/terms/service/1.0">이용약관</RouterLink>
          <RouterLink to="/terms/privacy/1.0">개인정보 처리방침</RouterLink>
        </nav>
        <a href="mailto:dohui091019@gmail.com">문의 dohui091019@gmail.com</a>
        <span>© 2026 RUNMATE</span>
      </div>
    </div>
  </footer>
</template>

<style scoped>
.app-footer {
  padding: 0 32px;
  background: #f2f7f3;
}
.app-footer-inner {
  width: min(1440px, 100%);
  margin: 0 auto;
  padding: 38px 0 46px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #15583c;
  background: transparent;
  border-top: 1px solid #d7e2da;
}
.app-footer-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #15583c;
  letter-spacing: 0.04em;
  text-decoration: none;
}
.app-footer-brand > span {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  color: #c7f36c;
  background: #176040;
  border-radius: 11px;
  font-weight: 900;
}
.app-footer-brand strong {
  font-size: 20px;
  font-weight: 900;
}
.app-footer p,
.app-footer-info a,
.app-footer-info span {
  color: #788a80;
  font-size: 12px;
}
.app-footer p {
  margin: 5px 0 0;
}
.app-footer-info {
  margin-right: 80px;
  display: grid;
  justify-items: end;
  gap: 8px;
}
.app-footer-info nav {
  display: flex;
  gap: 20px;
}
.app-footer-info a {
  text-decoration: none;
}
.app-footer-info a:hover {
  color: #15583c;
}
@media (max-width: 720px) {
  .app-footer {
    padding: 0 18px;
  }
  .app-footer-inner {
    padding: 28px 0;
    align-items: flex-start;
    gap: 24px;
    flex-direction: column;
  }
  .app-footer-info {
    margin-right: 0;
    justify-items: start;
  }
  .app-footer-inner {
    padding-bottom: 96px;
  }
}
</style>
