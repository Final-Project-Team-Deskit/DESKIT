export type AuthUser = {
  name: string
  email: string
  signupType: string
  memberCategory: string
  sellerRole?: string
  mbti?: string
  job?: string
  seller_id?: number
  sellerId?: number
  id?: number
  user_id?: number
  userId?: number
}

const apiBase = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

export const getAuthUser = (): AuthUser | null => {
  const raw = localStorage.getItem('deskit-user')
  if (!raw) return null
  try {
    const parsed = JSON.parse(raw) as Partial<AuthUser>
    if (typeof parsed?.email === 'string' && typeof parsed?.name === 'string') {
      return {
        name: parsed.name,
        email: parsed.email,
        signupType: parsed.signupType || '',
        memberCategory: parsed.memberCategory || '',
        sellerRole: (parsed as any).sellerRole || '',
        mbti: parsed.mbti || '',
        job: parsed.job || '',
        seller_id: typeof (parsed as any).seller_id === 'number' ? (parsed as any).seller_id : undefined,
        sellerId: typeof (parsed as any).sellerId === 'number' ? (parsed as any).sellerId : undefined,
        id: typeof (parsed as any).id === 'number' ? (parsed as any).id : undefined,
        user_id: typeof (parsed as any).user_id === 'number' ? (parsed as any).user_id : undefined,
        userId: typeof (parsed as any).userId === 'number' ? (parsed as any).userId : undefined,
      }
    }
  } catch {
    return null
  }
  return null
}

export const isLoggedIn = (): boolean => getAuthUser() !== null

export const isSeller = (): boolean => getAuthUser()?.memberCategory === '판매자'

export const isAdmin = (): boolean => getAuthUser()?.memberCategory === '관리자'

export const loginSeller = (): void => {
  const sellerUser = {
    name: '홍길동(판매자)',
    email: 'honggildong+seller@test.com',
    signupType: '판매자(임시)',
    memberCategory: '판매자',
    sellerRole: '오너',
    seller_id: 101,
  }

  localStorage.setItem('deskit-user', JSON.stringify(sellerUser))
  localStorage.setItem('deskit-auth', 'seller')
  window.dispatchEvent(new Event('deskit-user-updated'))
}

export const loginAdmin = (): void => {
  const adminUser = {
    name: '관리자',
    email: 'admin@test.com',
    signupType: '관리자(임시)',
    memberCategory: '관리자',
  }

  localStorage.setItem('deskit-user', JSON.stringify(adminUser))
  localStorage.setItem('deskit-auth', 'admin')
  window.dispatchEvent(new Event('deskit-user-updated'))
}

export const logout = (): void => {
  ;['deskit-user', 'deskit-auth', 'token'].forEach((key) => localStorage.removeItem(key))
  window.dispatchEvent(new Event('deskit-user-updated'))
}

export const requestLogout = async (): Promise<boolean> => {
  const access = localStorage.getItem('access') || sessionStorage.getItem('access')
  const headers: Record<string, string> = {}
  if (access) {
    headers.access = access
  }
  let success = false
  try {
    const response = await fetch(`${apiBase}/logout`, {
      method: 'POST',
      credentials: 'include',
      headers,
    })
    success = response.ok
  } catch (error) {
    console.error('logout failed', error)
  } finally {
    logout()
  }
  return success
}

export const hydrateSessionUser = async (): Promise<boolean> => {
  try {
    let response = await fetch(`${apiBase}/my`, {credentials: 'include'})
    if (!response.ok) {
      if (response.status === 401) {
        const reissue = await fetch(`${apiBase}/reissue`, {
          method: 'POST',
          credentials: 'include',
        })
        if (!reissue.ok) return false
        response = await fetch(`${apiBase}/my`, {credentials: 'include'})
      }
      if (!response.ok) return false
    }

    if (!getAuthUser()) {
      const authUser = {
        name: '로그인 사용자',
        email: '',
        signupType: '소셜 회원',
        memberCategory: '일반회원',
      }
      localStorage.setItem('deskit-user', JSON.stringify(authUser))
      window.dispatchEvent(new Event('deskit-user-updated'))
    }

    return true
  } catch {
    return false
  }
}
