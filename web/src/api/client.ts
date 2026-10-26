import type {
  ApplyResult,
  ConfigFile,
  ConfigGroupMeta,
  DiffPreview,
  EditableFileMeta,
  SessionState,
} from './types'

const API_BASE = '/api/admin-console'

/**
 * API 请求失败时抛出的错误，携带 HTTP 状态码供界面区分处理。
 * Error thrown on a failed API request, carrying the HTTP status so the UI can react to it.
 */
export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    credentials: 'same-origin',
    headers: init?.body ? { 'Content-Type': 'application/json' } : undefined,
    ...init,
  })
  if (!response.ok) {
    throw new ApiError(response.status, `${init?.method ?? 'GET'} ${path} failed with ${response.status}`)
  }
  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}

/** 登录并建立会话。 */
export function login(username: string, password: string): Promise<SessionState> {
  return request<SessionState>('/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  })
}

/** 退出登录。 */
export function logout(): Promise<void> {
  return request<void>('/logout', { method: 'POST' })
}

/** 查询会话状态。 */
export function session(): Promise<SessionState> {
  return request<SessionState>('/session')
}

/** 列出配置分组：控制台的一级菜单。 */
export function listGroups(): Promise<ConfigGroupMeta[]> {
  return request<ConfigGroupMeta[]>('/config/groups')
}

/** 列出某个分组下的可编辑配置文件。 */
export function listFiles(groupId: string): Promise<EditableFileMeta[]> {
  return request<EditableFileMeta[]>(`/config/groups/${groupId}/files`)
}

/** 读取文件详情。 */
export function readFile(fileId: string): Promise<ConfigFile> {
  return request<ConfigFile>(`/config/files/${fileId}`)
}

/** 预览改动差异，不落盘。 */
export function preview(fileId: string, changes: Record<string, string>): Promise<DiffPreview> {
  return request<DiffPreview>(`/config/files/${fileId}/preview`, {
    method: 'POST',
    body: JSON.stringify({ changes }),
  })
}

/** 应用改动：写盘并热应用。 */
export function apply(fileId: string, changes: Record<string, string>): Promise<ApplyResult> {
  return request<ApplyResult>(`/config/files/${fileId}/apply`, {
    method: 'POST',
    body: JSON.stringify({ changes }),
  })
}
