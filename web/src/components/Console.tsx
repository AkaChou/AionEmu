import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  AlertTriangle,
  CheckCircle2,
  ChevronRight,
  FileCog,
  Layers,
  Loader2,
  LogOut,
  RefreshCw,
  ShoppingBag,
} from 'lucide-react'
import { apply, listFiles, listGroups, preview, readFile } from '../api/client'
import type {
  ApplyResult,
  ConfigFile,
  ConfigGroupMeta,
  DiffPreview,
  EditableFileMeta,
} from '../api/types'
import { useLanguage } from '../i18n/useLanguage'
import { LANGUAGE_LABELS } from '../i18n/language'
import { DiffPanel } from './DiffPanel'
import { EntryTable } from './EntryTable'
import { Badge, Button, SectionTitle } from './ui'

/**
 * 控制台主视图：左侧配置分组与文件树、右侧键值表、底部差异预览。
 * Console main view: the configuration groups and file tree on the left, the key-value table on the right, the
 * diff preview at the bottom.
 */
export function Console({ onLogout }: { onLogout: () => void }) {
  const { language, t } = useLanguage()
  const [groups, setGroups] = useState<ConfigGroupMeta[]>([])
  const [activeGroup, setActiveGroup] = useState<string | null>(null)
  const [files, setFiles] = useState<EditableFileMeta[]>([])
  const [activeId, setActiveId] = useState<string | null>(null)
  const [config, setConfig] = useState<ConfigFile | null>(null)
  const [draft, setDraft] = useState<Record<string, string>>({})
  const [diff, setDiff] = useState<DiffPreview | null>(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)

  const loadFile = useCallback(async (fileId: string) => {
    setError(null)
    try {
      const loaded = await readFile(fileId)
      setConfig(loaded)
      setDraft({})
      setDiff(null)
    } catch {
      setError(t('message.readFailed'))
    }
  }, [t])

  const selectGroup = useCallback(
    async (groupId: string) => {
      setActiveGroup(groupId)
      setConfig(null)
      setDraft({})
      setDiff(null)
      setNotice(null)
      try {
        const loaded = await listFiles(groupId)
        setFiles(loaded)
        if (loaded.length > 0) {
          setActiveId(loaded[0].id)
          await loadFile(loaded[0].id)
        } else {
          setActiveId(null)
        }
      } catch {
        setError(t('message.listFailed'))
      }
    },
    [loadFile, t],
  )

  useEffect(() => {
    let cancelled = false
    void (async () => {
      try {
        const loaded = await listGroups()
        if (cancelled) {
          return
        }
        setGroups(loaded)
        const first = loaded[0]
        if (first) {
          await selectGroup(first.id)
        }
      } catch {
        if (!cancelled) {
          setError(t('message.listFailed'))
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    })()
    return () => {
      cancelled = true
    }
  }, [selectGroup, t])

  function handleSelect(fileId: string) {
    if (fileId === activeId) {
      return
    }
    setActiveId(fileId)
    void loadFile(fileId)
  }

  function handleEdit(key: string, value: string) {
    setDraft((previous) => ({ ...previous, [key]: value }))
    setNotice(null)
  }

  async function handlePreview() {
    if (!config || Object.keys(draft).length === 0) {
      return
    }
    setBusy(true)
    setError(null)
    try {
      setDiff(await preview(config.id, draft))
    } catch {
      setError(t('message.previewFailed'))
    } finally {
      setBusy(false)
    }
  }

  async function handleApply() {
    if (!config) {
      return
    }
    setBusy(true)
    setError(null)
    setNotice(null)
    try {
      const result: ApplyResult = await apply(config.id, draft)
      if (result.applied) {
        setNotice(t('message.applied', { path: config.path }))
        setDiff(null)
        await loadFile(config.id)
      } else {
        setError(
          result.rolledBack
            ? t('message.rolledBack', { path: config.path })
            : t('message.applyError', { path: config.path }),
        )
        setDiff(null)
        await loadFile(config.id)
      }
    } catch {
      setError(t('message.applyFailed'))
    } finally {
      setBusy(false)
    }
  }

  const activeMeta = useMemo(
    () => files.find((file) => file.id === activeId),
    [files, activeId],
  )
  const dirtyCount = Object.keys(draft).length
  const activeGroupMeta = groups.find((group) => group.id === activeGroup)

  return (
    <div className="flex h-[100dvh] flex-col">
      <header className="flex h-14 shrink-0 items-center gap-3 border-b border-line bg-surface-1 px-4">
        <span className="flex h-8 w-8 items-center justify-center rounded-console bg-accent-muted text-accent">
          <FileCog size={16} strokeWidth={1.75} />
        </span>
        <div className="min-w-0">
          <h1 className="text-sm font-semibold text-text-primary">{t('app.title')}</h1>
          <p className="truncate text-xs text-text-muted">
            {activeMeta ? activeMeta.path : t('app.subtitle')}
          </p>
        </div>

        <div className="ml-auto flex items-center gap-2">
          {notice && (
            <span className="flex items-center gap-1.5 text-xs text-ok">
              <CheckCircle2 size={14} strokeWidth={1.75} />
              {notice}
            </span>
          )}
          {error && (
            <span className="flex items-center gap-1.5 text-xs text-danger">
              <AlertTriangle size={14} strokeWidth={1.75} />
              {error}
            </span>
          )}
          <LanguageSwitch />
          <Button size="sm" variant="ghost" onClick={onLogout} aria-label={t('action.logout')}>
            <LogOut size={14} strokeWidth={1.75} />
          </Button>
        </div>
      </header>

      <div className="flex min-h-0 flex-1">
        <nav className="flex w-[248px] shrink-0 flex-col border-r border-line bg-surface-1">
          <div className="px-3 pb-1 pt-3">
            <SectionTitle className="flex items-center gap-1.5 text-text-secondary">
              <Layers size={13} strokeWidth={1.75} />
              {t('nav.groups')}
            </SectionTitle>
          </div>
          <ul className="flex flex-col gap-0.5 px-2">
            {groups.map((group) => (
              <li key={group.id}>
                <button
                  type="button"
                  onClick={() => void selectGroup(group.id)}
                  aria-current={group.id === activeGroup}
                  className={`flex w-full items-center gap-1.5 rounded-console px-2 py-1.5 text-left transition-colors ${
                    group.id === activeGroup
                      ? 'bg-accent-muted text-accent'
                      : 'text-text-secondary hover:bg-surface-3 hover:text-text-primary'
                  }`}
                >
                  {group.id === 'shop' ? (
                    <ShoppingBag size={13} strokeWidth={1.75} className="shrink-0" />
                  ) : (
                    <ChevronRight size={13} strokeWidth={1.75} className="shrink-0" />
                  )}
                  <span className="truncate text-[13px] font-medium">
                    {language === 'en' ? group.labelEn : group.labelZh}
                  </span>
                  {group.id === 'shop' && (
                    <span className="ml-auto shrink-0 rounded-console-sm border border-line-strong px-1.5 py-px text-[10px] text-text-muted">
                      {t('nav.shopBadge')}
                    </span>
                  )}
                </button>
              </li>
            ))}
          </ul>

          <div className="mt-4 px-3 pb-1">
            <SectionTitle className="text-text-secondary">{t('nav.files')}</SectionTitle>
          </div>
          <ul className="flex min-h-0 flex-1 flex-col gap-0.5 overflow-auto px-2 pb-3">
            {files.map((file) => (
              <li key={file.id}>
                <button
                  type="button"
                  onClick={() => handleSelect(file.id)}
                  aria-current={file.id === activeId}
                  className={`flex w-full flex-col items-start gap-0.5 rounded-console px-2 py-1.5 text-left transition-colors ${
                    file.id === activeId
                      ? 'bg-accent-muted text-accent'
                      : 'text-text-secondary hover:bg-surface-3 hover:text-text-primary'
                  }`}
                >
                  <span className="tabular text-[13px] font-medium">{file.id}</span>
                  <span className="w-full truncate text-[11px] text-text-muted">{file.path}</span>
                </button>
              </li>
            ))}
          </ul>
        </nav>

        <main className="flex min-w-0 flex-1 flex-col">
          {loading ? (
            <div className="flex flex-1 items-center justify-center">
              <Loader2 size={20} className="animate-spin text-text-muted" />
            </div>
          ) : config ? (
            <>
              <div className="flex items-center gap-2 border-b border-line px-4 py-2.5">
                <SectionTitle>{config.id}</SectionTitle>
                <Badge>{t('message.count', { count: config.entries.length })}</Badge>
                <Badge tone="info">{t(`format.${config.format}` as 'format.PROPERTIES')}</Badge>
                {activeGroupMeta && (
                  <Badge tone="neutral">
                    {language === 'en' ? activeGroupMeta.labelEn : activeGroupMeta.labelZh}
                  </Badge>
                )}
                <div className="ml-auto flex items-center gap-2">
                  {dirtyCount > 0 && (
                    <span className="tabular text-xs text-accent">
                      {t('message.changedCount', { count: dirtyCount })}
                    </span>
                  )}
                  <Button
                    size="sm"
                    variant="secondary"
                    onClick={() => void handlePreview()}
                    disabled={dirtyCount === 0 || busy}
                  >
                    {busy ? (
                      <Loader2 size={13} className="animate-spin" />
                    ) : (
                      <RefreshCw size={13} strokeWidth={1.75} />
                    )}
                    {t('action.preview')}
                  </Button>
                </div>
              </div>
              <EntryTable entries={config.entries} draft={draft} onChange={handleEdit} />
              <DiffPanel
                preview={diff}
                busy={busy}
                onApply={() => void handleApply()}
                onDiscard={() => setDiff(null)}
              />
            </>
          ) : activeGroup === 'shop' && files.length === 0 ? (
            <div className="flex flex-1 flex-col items-center justify-center gap-1.5 px-6 text-center">
              <ShoppingBag size={22} className="text-text-muted" strokeWidth={1.5} />
              <p className="text-sm text-text-secondary">{t('nav.shopPlaceholder')}</p>
              <p className="max-w-md text-xs text-text-muted">{t('nav.shopPlaceholderHint')}</p>
            </div>
          ) : (
            <div className="flex flex-1 items-center justify-center text-sm text-text-muted">
              {t('message.selectFile')}
            </div>
          )}
        </main>
      </div>
    </div>
  )
}

/**
 * 语言切换：中文与英文即时切换，选择持久化在本地存储。
 * The language switch: Chinese and English switch instantly and the choice is persisted in the local storage.
 */
function LanguageSwitch() {
  const { language, setLanguage } = useLanguage()
  return (
    <div
      className="flex overflow-hidden rounded-console border border-line-strong"
      role="group"
      aria-label="Language"
    >
      {(['zh', 'en'] as const).map((code) => (
        <button
          key={code}
          type="button"
          onClick={() => setLanguage(code)}
          aria-pressed={language === code}
          className={`px-2 py-1 text-[11px] font-medium transition-colors ${
            language === code
              ? 'bg-accent text-accent-contrast'
              : 'bg-surface-3 text-text-secondary hover:text-text-primary'
          }`}
        >
          {LANGUAGE_LABELS[code]}
        </button>
      ))}
    </div>
  )
}
