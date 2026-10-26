/**
 * 管理控制台的界面文案字典。
 * The UI copy dictionaries of the admin console.
 *
 * 键名按「区域.文案」组织；中文为默认语言，英文为备选。
 * The keys are organized as "area.copy"; Chinese is the default language and English the alternative.
 */

export type Language = 'zh' | 'en'

export const LANGUAGE_LABELS: Record<Language, string> = {
  zh: '中文',
  en: 'English',
}

/**
 * 中文字典（默认）。
 * The Chinese dictionary (the default).
 */
const zh = {
  'app.title': 'AionEmu 管理控制台',
  'app.subtitle': '服务器配置与热重载',

  'nav.groups': '配置分组',
  'nav.files': '配置文件',
  'nav.shopPlaceholder': '商城管理即将上线',
  'nav.shopPlaceholderHint': '商品定义与商城开关将在下一批接入。',
  'nav.shopBadge': '筹备中',

  'action.preview': '预览改动',
  'action.apply': '应用',
  'action.discard': '放弃',
  'action.logout': '退出登录',
  'action.retry': '重试',
  'action.language': '语言',
  'action.search': '搜索',

  'login.title': '登录管理控制台',
  'login.username': '用户名',
  'login.password': '密码',
  'login.submit': '登录',
  'login.pending': '正在登录…',
  'login.failed': '登录失败，请检查用户名与密码。',

  'table.key': '键',
  'table.comment': '备注',
  'table.fileValue': '文件值',
  'table.effectiveValue': '生效值',
  'table.source': '来源',
  'table.default': '默认值',

  'table.empty.title': '该文件没有可编辑的键',
  'table.empty.hint': '文件只包含注释，或键名格式无法识别。',
  'table.searchPlaceholder': '搜索键、备注或值',

  'diff.title': '改动预览',
  'diff.empty': '尚无待应用的改动。',
  'diff.hint': '确认后写入文件并热应用到运行中的服务。',
  'diff.applyAndHot': '写入并热应用',
  'diff.applying': '应用中…',
  'diff.line': '第 {line} 行',
  'diff.before': '原值',
  'diff.after': '新值',

  'message.readFailed': '读取配置文件失败。',
  'message.listFailed': '无法加载配置分组。',
  'message.previewFailed': '生成改动预览失败。',
  'message.applyFailed': '写入失败，请检查控制台日志。',
  'message.applied': '已写入 {path} 并热应用。',
  'message.rolledBack': '热应用失败，已回滚到写入前的状态：{path}',
  'message.applyError': '热应用失败：{path}',
  'message.selectFile': '请选择一个配置文件。',
  'message.count': '{count} 项',
  'message.changed': '已改',
  'message.changedCount': '{count} 项未应用',

  'source.FILE': '文件值',
  'source.OVERRIDE_LAYER': '被覆盖层覆盖',
  'source.APPLICATION_YML': '被 application.yml 覆盖',
  'source.ENVIRONMENT': '被环境变量覆盖',
  'source.SYSTEM_PROPERTY': '被系统属性覆盖',
  'source.COMMAND_LINE': '被命令行覆盖',
  'source.DEFAULT': '默认值',

  'format.PROPERTIES': '键值文件',
  'format.ITEM_ID_LIST': 'ID 列表',
} as const

/**
 * 英文字典。
 * The English dictionary.
 */
const en: Record<keyof typeof zh, string> = {
  'app.title': 'AionEmu Admin Console',
  'app.subtitle': 'Server configuration and hot reload',

  'nav.groups': 'Configuration groups',
  'nav.files': 'Configuration files',
  'nav.shopPlaceholder': 'Shop management is coming',
  'nav.shopPlaceholderHint': 'Item definitions and shop switches land in the next batch.',
  'nav.shopBadge': 'Planned',

  'action.preview': 'Preview changes',
  'action.apply': 'Apply',
  'action.discard': 'Discard',
  'action.logout': 'Sign out',
  'action.retry': 'Retry',
  'action.language': 'Language',
  'action.search': 'Search',

  'login.title': 'Sign in to the admin console',
  'login.username': 'User name',
  'login.password': 'Password',
  'login.submit': 'Sign in',
  'login.pending': 'Signing in…',
  'login.failed': 'Sign-in failed; check the user name and the password.',

  'table.key': 'Key',
  'table.comment': 'Description',
  'table.fileValue': 'File value',
  'table.effectiveValue': 'Effective value',
  'table.source': 'Source',
  'table.default': 'Default',

  'table.empty.title': 'This file has no editable key',
  'table.empty.hint': 'The file only holds comments, or its key format is not recognized.',
  'table.searchPlaceholder': 'Search keys, descriptions or values',

  'diff.title': 'Change preview',
  'diff.empty': 'No change is pending.',
  'diff.hint': 'On confirm the file is written and hot-applied to the running server.',
  'diff.applyAndHot': 'Write and hot-apply',
  'diff.applying': 'Applying…',
  'diff.line': 'Line {line}',
  'diff.before': 'Before',
  'diff.after': 'After',

  'message.readFailed': 'Failed to read the configuration file.',
  'message.listFailed': 'Failed to load the configuration groups.',
  'message.previewFailed': 'Failed to build the change preview.',
  'message.applyFailed': 'The write failed; check the console log.',
  'message.applied': 'Written to {path} and hot-applied.',
  'message.rolledBack': 'The hot-apply failed and was rolled back to the previous state: {path}',
  'message.applyError': 'The hot-apply failed: {path}',
  'message.selectFile': 'Select a configuration file.',
  'message.count': '{count} entries',
  'message.changed': 'changed',
  'message.changedCount': '{count} pending',

  'source.FILE': 'File value',
  'source.OVERRIDE_LAYER': 'Overridden by the override layer',
  'source.APPLICATION_YML': 'Overridden by application.yml',
  'source.ENVIRONMENT': 'Overridden by an environment variable',
  'source.SYSTEM_PROPERTY': 'Overridden by a system property',
  'source.COMMAND_LINE': 'Overridden by a command-line argument',
  'source.DEFAULT': 'Default value',

  'format.PROPERTIES': 'Key-value file',
  'format.ITEM_ID_LIST': 'ID list',
}

export type MessageKey = keyof typeof zh

export const dictionaries = { zh, en }

/**
 * 把模板里的占位符替换为实参。
 * Replaces the placeholders in a template with the arguments.
 */
export function formatMessage(template: string, args?: Record<string, string | number>): string {
  if (!args) {
    return template
  }
  return template.replace(/\{(\w+)\}/g, (match, key: string) =>
    key in args ? String(args[key]) : match,
  )
}
