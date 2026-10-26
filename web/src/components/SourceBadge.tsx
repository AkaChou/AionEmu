import type { ValueSource } from '../api/types'
import { SOURCE_TONES } from '../api/sources'
import { useLanguage } from '../i18n/useLanguage'
import { Badge } from './ui'

/**
 * 生效来源徽标：把「文件值」与「真正生效的值」之间的差异显式呈现给运维。
 * Effective source badge: makes the gap between the file value and the value that actually takes effect
 * explicit to the operator.
 */
export function SourceBadge({ source }: { source: ValueSource }) {
  const { t } = useLanguage()
  return (
    <Badge tone={SOURCE_TONES[source]}>{t(`source.${source}` as 'source.FILE')}</Badge>
  )
}
