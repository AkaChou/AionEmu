import type { DiffPreview } from '../api/types'
import { useLanguage } from '../i18n/useLanguage'
import { Badge, Modal } from './ui'

/**
 * 差异预览对话框：写入前的逐行对照居中浮出，确认后才落盘。
 * Diff preview dialog: a line-by-line comparison shown in a centered overlay before writing.
 */
export function DiffPanel({
  preview,
  busy,
  onApply,
  onDiscard,
}: {
  preview: DiffPreview | null
  busy: boolean
  onApply: () => void
  onDiscard: () => void
}) {
  const { t } = useLanguage()

  if (!preview) {
    return null
  }

  const changedCount = Object.keys(preview.changedKeys).length

  return (
    <Modal
      open
      title={
        <span className="flex items-center gap-2">
          {t('diff.title')}
          <Badge tone="accent">{t('message.count', { count: changedCount })}</Badge>
        </span>
      }
      onClose={onDiscard}
      footer={
        <>
          <span className="mr-auto text-xs text-text-muted">
            {changedCount === 0 ? t('diff.empty') : t('diff.hint')}
          </span>
          <button
            type="button"
            onClick={onDiscard}
            disabled={busy}
            className="rounded-console px-3 py-1.5 text-xs text-text-secondary transition-colors hover:bg-surface-3 hover:text-text-primary disabled:opacity-45"
          >
            {t('action.discard')}
          </button>
          <button
            type="button"
            onClick={onApply}
            disabled={busy || changedCount === 0}
            className="rounded-console bg-accent px-3.5 py-1.5 text-xs font-medium text-accent-contrast transition-colors hover:bg-accent-hover disabled:opacity-45"
          >
            {busy ? t('diff.applying') : t('diff.applyAndHot')}
          </button>
        </>
      }
    >
      {changedCount === 0 ? (
        <p className="px-4 py-8 text-center text-sm text-text-secondary">{t('diff.empty')}</p>
      ) : (
        <div className="px-0 py-1">
          {preview.lines.map((line) => (
            <div key={line.lineNumber} className="tabular flex text-[12px] leading-5">
              <span className="w-12 shrink-0 select-none px-3 text-right text-text-muted">
                {line.lineNumber}
              </span>
              <span className="min-w-0 flex-1 whitespace-pre-wrap break-all px-2">
                {line.before !== null && (
                  <span className="block bg-danger-muted/60 text-danger line-through decoration-danger/40">
                    {line.before}
                  </span>
                )}
                {line.after !== null && (
                  <span className="block bg-ok-muted/60 text-ok">{line.after}</span>
                )}
              </span>
            </div>
          ))}
        </div>
      )}
    </Modal>
  )
}
