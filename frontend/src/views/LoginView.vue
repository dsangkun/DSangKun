<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { login, mockAuthAccounts } from '../auth/session'

const router = useRouter()
const route = useRoute()

const username = ref('')
const password = ref('123456')
const errorText = ref('')

const operatorAccounts = computed(() => mockAuthAccounts.filter((item) => item.role === 'operator'))

const fillAccount = (nextUsername: string) => {
  username.value = nextUsername
  errorText.value = ''
}

const handleLogin = () => {
  const result = login(username.value, password.value)

  if (!result.success) {
    errorText.value = result.message
    return
  }

  const redirect = typeof route.query.redirect === 'string' && route.query.redirect ? route.query.redirect : '/'
  void router.replace(redirect)
}
</script>

<template>
  <div class="login-page">
    <div class="login-card login-card-enhanced">
      <section class="login-brand-panel">
        <div class="login-badge">系统登录</div>
        <h1 class="login-title">Amazon运营推进器</h1>
        <p class="login-desc">
          登录后按当前运营自动过滤可见父ASIN、异常任务和数据页入口。当前先使用前端模拟登录，后续可直接切到真实后端认证。
        </p>

        <div class="login-feature-list">
          <div class="login-feature-item">
            <strong>任务页</strong>
            <span>按登录运营自动展示自己负责的父ASIN任务</span>
          </div>
          <div class="login-feature-item">
            <strong>数据页</strong>
            <span>进入父ASIN后直达对应日报 Sheet</span>
          </div>
          <div class="login-feature-item">
            <strong>权限边界</strong>
            <span>管理员看全部，运营只看自己负责产品</span>
          </div>
        </div>
      </section>

      <section class="login-form-panel">
        <div class="login-form-head">
          <div class="login-form-title">账号登录</div>
          <div class="login-form-subtitle">请输入账号与密码进入系统</div>
        </div>

        <div class="login-form-grid single">
          <label class="login-field">
            <span>账号</span>
            <input v-model="username" class="login-input" placeholder="请输入账号" @keyup.enter="handleLogin" />
          </label>

          <label class="login-field">
            <span>密码</span>
            <input v-model="password" type="password" class="login-input" placeholder="请输入密码" @keyup.enter="handleLogin" />
          </label>
        </div>

        <div class="login-hint">当前为前端模拟登录，默认密码统一为 <strong>123456</strong></div>
        <div v-if="errorText" class="login-error">{{ errorText }}</div>

        <button class="login-submit-btn" @click="handleLogin">登录进入系统</button>

        <div class="login-account-panel">
          <div class="login-account-head">快捷测试账号</div>
          <div class="login-account-list">
            <button class="login-account-chip admin" @click="fillAccount('admin')">管理员</button>
            <button
              v-for="account in operatorAccounts"
              :key="account.username"
              class="login-account-chip"
              @click="fillAccount(account.username)"
            >
              {{ account.displayName }}
            </button>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>
