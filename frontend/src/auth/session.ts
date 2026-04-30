import { computed, ref } from 'vue'
import { asinMapping } from '../constants/asinMapping'

export type AuthRole = 'admin' | 'operator'

export type AuthUser = {
  username: string
  displayName: string
  ownerName: string
  role: AuthRole
  unionId?: string
}

type MockAuthAccount = AuthUser & {
  password: string
}

const DEFAULT_UNION_ID = 'aXcM7iiyEGBkqvsE8FyLkOQiEiE'

const AUTH_STORAGE_KEY = 'ecommerce-ops-auth-user'

const ownerNames = [...new Set(asinMapping.map((item) => item.ownerName).filter(Boolean))]

export const mockAuthAccounts: MockAuthAccount[] = [
  {
    username: 'admin',
    displayName: '管理员',
    ownerName: '全部',
    role: 'admin',
    password: '123456',
    unionId: DEFAULT_UNION_ID
  },
  ...ownerNames.map((name) => ({
    username: name,
    displayName: name,
    ownerName: name,
    role: 'operator' as const,
    password: '123456',
    unionId: DEFAULT_UNION_ID
  }))
]

const readStoredUser = (): AuthUser | null => {
  if (typeof window === 'undefined') return null
  const raw = window.localStorage.getItem(AUTH_STORAGE_KEY)
  if (!raw) return null

  try {
    return JSON.parse(raw) as AuthUser
  } catch {
    window.localStorage.removeItem(AUTH_STORAGE_KEY)
    return null
  }
}

export const currentUser = ref<AuthUser | null>(readStoredUser())

const persistUser = (user: AuthUser | null) => {
  if (typeof window === 'undefined') return

  if (!user) {
    window.localStorage.removeItem(AUTH_STORAGE_KEY)
    return
  }

  window.localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(user))
}

export const isAuthenticated = computed(() => Boolean(currentUser.value))

export const login = (username: string, password: string) => {
  const normalizedUsername = username.trim()
  const matched = mockAuthAccounts.find((account) => account.username === normalizedUsername && account.password === password)

  if (!matched) {
    return {
      success: false as const,
      message: '账号或密码错误'
    }
  }

  const user: AuthUser = {
    username: matched.username,
    displayName: matched.displayName,
    ownerName: matched.ownerName,
    role: matched.role,
    unionId: matched.unionId ?? DEFAULT_UNION_ID
  }

  currentUser.value = user
  persistUser(user)

  return {
    success: true as const,
    user
  }
}

export const logout = () => {
  currentUser.value = null
  persistUser(null)
}

export const getCurrentUser = () => currentUser.value
