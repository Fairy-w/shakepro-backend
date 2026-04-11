import http from './http'

export interface LoginRequest {
  username: string
  password: string
}

export interface AdminProfile {
  id: number
  username: string
  nickname: string
  role: string
}

export interface AdminLoginResponse {
  token: string
  tokenType: string
  expireSeconds: number
  user: AdminProfile
}

export const authApi = {
  login(data: LoginRequest) {
    return http.post('/admin/auth/login', data) as Promise<AdminLoginResponse>
  },
  getMe() {
    return http.get('/admin/auth/me') as Promise<AdminProfile>
  },
}
