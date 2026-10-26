import type { ValueSource } from './types'

/**
 * 生效来源的展示色调：被覆盖的来源用危险色，默认值用信息色。
 * The display tone of every effective value source: an overridden source uses the danger tone and the default
 * value uses the info tone.
 */
export const SOURCE_TONES: Record<
  ValueSource,
  'neutral' | 'accent' | 'ok' | 'danger' | 'info'
> = {
  FILE: 'neutral',
  OVERRIDE_LAYER: 'danger',
  APPLICATION_YML: 'danger',
  ENVIRONMENT: 'danger',
  SYSTEM_PROPERTY: 'danger',
  COMMAND_LINE: 'danger',
  DEFAULT: 'info',
}

/**
 * 该来源是否意味着「改文件不会生效」。
 * Whether the source means editing the file will not take effect.
 */
export function isOverridden(source: ValueSource) {
  return (
    source === 'OVERRIDE_LAYER' ||
    source === 'APPLICATION_YML' ||
    source === 'ENVIRONMENT' ||
    source === 'SYSTEM_PROPERTY' ||
    source === 'COMMAND_LINE'
  )
}
