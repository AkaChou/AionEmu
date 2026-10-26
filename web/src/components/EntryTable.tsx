import { useMemo, useState } from 'react'
import { Search } from 'lucide-react'
import type { ConfigEntry } from '../api/types'
import { isOverridden } from '../api/sources'
import { useLanguage } from '../i18n/useLanguage'
import { SourceBadge } from './SourceBadge'
import { Input, Switch } from './ui'

/**
 * 键值编辑表：键与备注并排，按类型分控件，并把「文件值 / 生效值 / 覆盖来源」同时展示。
 * Key-value editor: the key and its description side by side, type-appropriate controls, and the file value,
 * the effective value and the override source all shown at once.
 */
export function EntryTable({
  entries,
  draft,
  onChange,
}: {
  entries: ConfigEntry[]
  draft: Record<string, string>
  onChange: (key: string, value: string) => void
}) {
  const { language, t } = useLanguage()
  const [query, setQuery] = useState('')

  const visible = useMemo(() => {
    const needle = query.trim().toLowerCase()
    if (!needle) {
      return entries
    }
    return entries.filter(
      (entry) =>
        entry.key.toLowerCase().includes(needle) ||
        (entry.fileValue ?? '').toLowerCase().includes(needle) ||
        descriptionOf(entry, language).toLowerCase().includes(needle),
    )
  }, [entries, query, language])

  if (entries.length === 0) {
    return (
      <div className="flex min-h-[200px] flex-col items-center justify-center gap-1 px-6 text-center">
        <p className="text-sm text-text-secondary">{t('table.empty.title')}</p>
        <p className="text-xs text-text-muted">{t('table.empty.hint')}</p>
      </div>
    )
  }

  return (
    <div className="flex min-h-0 flex-1 flex-col">
      <div className="flex items-center gap-2 border-b border-line px-3 py-2">
        <Search size={14} className="shrink-0 text-text-muted" strokeWidth={1.75} />
        <Input
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder={t('table.searchPlaceholder')}
          className="h-8 max-w-xs"
          aria-label={t('table.searchPlaceholder')}
        />
        <span className="tabular ml-auto text-xs text-text-muted">
          {visible.length} / {entries.length}
        </span>
      </div>

      <div className="min-h-0 flex-1 overflow-auto">
        <table className="w-full border-collapse text-sm">
          <thead className="sticky top-0 z-10 bg-surface-2">
            <tr className="border-b border-line text-left">
              <th className="px-3 py-2 text-xs font-medium text-text-secondary">
                {t('table.comment')} / {t('table.key')}
              </th>
              <th className="px-3 py-2 text-xs font-medium text-text-secondary">{t('table.fileValue')}</th>
              <th className="px-3 py-2 text-xs font-medium text-text-secondary">{t('table.default')}</th>
              <th className="px-3 py-2 text-xs font-medium text-text-secondary">
                {t('table.effectiveValue')}
              </th>
              <th className="px-3 py-2 text-xs font-medium text-text-secondary">{t('table.source')}</th>
            </tr>
          </thead>
          <tbody>
            {visible.map((entry) => (
              <EntryRow key={entry.key} entry={entry} draft={draft} onChange={onChange} />
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

/**
 * 取出键的备注：按当前语言选择中文或英文说明，两侧都缺时退回键名。
 * Picks the description of a key: the Chinese or the English text depending on the language, falling back to
 * the key itself when neither is present.
 */
function descriptionOf(entry: ConfigEntry, language: 'zh' | 'en'): string {
  const comment = entry.comment
  if (!comment) {
    return ''
  }
  const primary = language === 'en' ? comment.descriptionEn : comment.description
  const secondary = language === 'en' ? comment.description : comment.descriptionEn
  return primary || secondary || ''
}

function EntryRow({
  entry,
  draft,
  onChange,
}: {
  entry: ConfigEntry
  draft: Record<string, string>
  onChange: (key: string, value: string) => void
}) {
  const { language, t } = useLanguage()
  const current = draft[entry.key] ?? entry.fileValue ?? ''
  const dirty = entry.key in draft && draft[entry.key] !== (entry.fileValue ?? '')
  const overridden = isOverridden(entry.source)
  const description = descriptionOf(entry, language)
  const defaultValue = entry.comment?.defaultValue ?? ''

  return (
    <tr className="border-b border-line/60 last:border-b-0 hover:bg-surface-2/50">
      <td className="max-w-[320px] px-3 py-1.5 align-middle">
        {description ? (
          <span className="block truncate text-[13px] text-text-secondary" title={description}>
            {description}
          </span>
        ) : (
          <span className="block text-[13px] text-text-muted">—</span>
        )}
        <span className="tabular mt-0.5 block truncate text-[11px] text-text-muted" title={entry.key}>
          {entry.key}
        </span>
      </td>
      <td className="w-[220px] px-3 py-1.5 align-middle">
        <ValueControl
          value={current}
          original={entry.fileValue ?? ''}
          dirty={dirty}
          changedLabel={t('message.changed')}
          onChange={(value) => onChange(entry.key, value)}
        />
      </td>
      <td className="w-[150px] px-3 py-1.5 align-middle">
        {defaultValue ? (
          <span className="tabular block truncate text-[13px] text-text-secondary" title={defaultValue}>
            {defaultValue}
          </span>
        ) : (
          <span className="block text-[13px] text-text-muted">—</span>
        )}
      </td>
      <td className="max-w-[220px] px-3 py-1.5 align-middle">
        <span
          className="tabular block truncate text-[13px]"
          title={entry.effectiveValue ?? ''}
        >
          {entry.effectiveValue ?? <span className="text-text-muted">—</span>}
        </span>
      </td>
      <td className="w-[190px] px-3 py-1.5 align-middle">
        <span className="flex items-center gap-1.5">
          <SourceBadge source={entry.source} />
          {overridden && (
            <span className="truncate text-[11px] text-text-muted" title={entry.source}>
              {t(`source.${entry.source}` as 'source.FILE')}
            </span>
          )}
        </span>
      </td>
    </tr>
  )
}

/**
 * 值控件：按值的形态选择数字输入、开关或文本输入。
 * Value control: picks a number input, a switch or a text input from the shape of the value.
 */
function ValueControl({
  value,
  original,
  dirty,
  changedLabel,
  onChange,
}: {
  value: string
  original: string
  dirty: boolean
  changedLabel: string
  onChange: (value: string) => void
}) {
  if (value === 'true' || value === 'false') {
    return (
      <div className="flex items-center gap-2">
        <Switch
          checked={value === 'true'}
          label={value === 'true' ? '切换为 false' : '切换为 true'}
          onCheckedChange={(checked) => onChange(checked ? 'true' : 'false')}
        />
        <span className="tabular text-xs text-text-secondary">{value}</span>
        {dirty && <span className="text-[11px] text-accent">{changedLabel}</span>}
      </div>
    )
  }

  if (/^-?\d+$/.test(value)) {
    return (
      <div className="flex items-center gap-2">
        <Input
          type="number"
          value={value}
          onChange={(event) => onChange(event.target.value)}
          className="tabular h-8"
        />
        {dirty && <span className="text-[11px] text-accent">{changedLabel}</span>}
      </div>
    )
  }

  return (
    <div className="flex items-center gap-2">
      <Input
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="h-8"
        placeholder={original}
      />
      {dirty && <span className="text-[11px] text-accent">{changedLabel}</span>}
    </div>
  )
}
