/**
 * 管理控制台的 API 类型定义。
 * API type definitions for the admin console.
 */

/** 配置分组：控制台的一级菜单。 */
/**
 * 会话状态：当前登录是否有效。
 */
export interface SessionState {
  authenticated: boolean
}

export interface ConfigGroupMeta {
  id: string
  labelZh: string
  labelEn: string
  directory: string
}

/** 可编辑配置文件的描述符。 */
export interface EditableFileMeta {
  id: string
  path: string
  format: 'PROPERTIES' | 'ITEM_ID_LIST'
  applyTarget: string
  group: string
}

/** 生效值来源：与后端 ValueSource 枚举一一对应。 */
export type ValueSource =
  | 'FILE'
  | 'OVERRIDE_LAYER'
  | 'APPLICATION_YML'
  | 'ENVIRONMENT'
  | 'SYSTEM_PROPERTY'
  | 'COMMAND_LINE'
  | 'DEFAULT'

/** 配置键的备注：来自配置文件里键上方的双语注释块。 */
export interface ConfigKeyComment {
  description: string
  descriptionEn: string
  defaultValue: string
}

/** 文件中的一个键：备注、文件值、生效值与覆盖来源。 */
export interface ConfigEntry {
  key: string
  fileValue: string | null
  effectiveValue: string | null
  source: ValueSource
  comment: ConfigKeyComment | null
}

/** 文件详情。 */
export interface ConfigFile {
  id: string
  path: string
  format: 'PROPERTIES' | 'ITEM_ID_LIST'
  entries: ConfigEntry[]
}

/** 差异行。 */
export interface DiffLine {
  lineNumber: number
  before: string | null
  after: string | null
}

/** 差异预览。 */
export interface DiffPreview {
  lines: DiffLine[]
  changedKeys: Record<string, string>
}

/** 应用结果。 */
export interface ApplyResult {
  id: string
  applied: boolean
  rolledBack: boolean
  changedKeys: Record<string, string>
}
