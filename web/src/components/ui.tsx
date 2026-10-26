import { useEffect } from 'react'
import type { ButtonHTMLAttributes, HTMLAttributes, InputHTMLAttributes, ReactNode } from 'react'
import { X } from 'lucide-react'
import { cn } from '../lib/utils'

/**
 * 基础 UI 原子组件。
 * Base UI atoms.
 *
 * 形状一致性：统一 6px 圆角刻度（--radius-console），所有组件共用。
 * Shape consistency: a single 6px radius scale shared by every component.
 */

type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger'
type ButtonSize = 'sm' | 'md'

const BUTTON_VARIANTS: Record<ButtonVariant, string> = {
  // 主按钮：琥珀底 + 深色字，对比度远超 WCAG AA。
  primary: 'bg-accent text-accent-contrast hover:bg-accent-hover active:translate-y-px',
  secondary:
    'bg-surface-3 text-text-primary border border-line-strong hover:border-accent hover:text-accent active:translate-y-px',
  ghost: 'text-text-secondary hover:bg-surface-3 hover:text-text-primary active:translate-y-px',
  danger: 'bg-danger text-white hover:bg-danger/85 active:translate-y-px',
}

const BUTTON_SIZES: Record<ButtonSize, string> = {
  sm: 'h-7 px-2.5 text-xs',
  md: 'h-9 px-3.5 text-sm',
}

export function Button({
  variant = 'secondary',
  size = 'md',
  className,
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: ButtonVariant; size?: ButtonSize }) {
  return (
    <button
      className={cn(
        'inline-flex items-center justify-center gap-1.5 rounded-console font-medium transition-colors',
        'disabled:pointer-events-none disabled:opacity-45',
        BUTTON_SIZES[size],
        BUTTON_VARIANTS[variant],
        className,
      )}
      {...props}
    />
  )
}

export function Input({ className, ...props }: InputHTMLAttributes<HTMLInputElement>) {
  return (
    <input
      className={cn(
        'h-9 w-full rounded-console border border-line bg-surface-1 px-2.5 text-sm text-text-primary',
        'placeholder:text-text-muted focus:border-accent focus:outline-none',
        'disabled:opacity-50',
        className,
      )}
      {...props}
    />
  )
}

export function Switch({
  checked,
  onCheckedChange,
  disabled,
  label,
}: {
  checked: boolean
  onCheckedChange: (checked: boolean) => void
  disabled?: boolean
  label: string
}) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      aria-label={label}
      disabled={disabled}
      onClick={() => onCheckedChange(!checked)}
      className={cn(
        // 轨道固定 36px，滑块 14px；溢出裁掉，滑块永远压在轨道内，不会盖住旁边的文字。
        // The track is a fixed 36px and the knob 14px; the overflow is clipped so the knob always stays
        // inside the track and never covers the text next to it.
        'relative h-5 w-9 shrink-0 overflow-hidden rounded-full border transition-colors disabled:opacity-50',
        checked ? 'border-accent bg-accent' : 'border-line-strong bg-surface-3',
      )}
    >
      <span
        className={cn(
          // 以 left-0 锚定：绝对定位元素缺少 left 时静态位置随内容流漂移，滑块会溢出轨道。
          // Anchored with left-0: an absolutely positioned element without a left drifts with the content
          // flow and the knob escapes the track.
          'absolute left-0 top-0.5 h-3.5 w-3.5 rounded-full transition-transform',
          // 轨道 36px、边框 1px、滑块 14px：关闭态左边距 2px，打开态右边距 2px，行程 16px。
          // Track 36px, border 1px, knob 14px: 2px inset on the left when off, 2px on the right when on,
          // a 16px travel.
          checked ? 'translate-x-[18px] bg-accent-contrast' : 'translate-x-[2px] bg-text-muted',
        )}
      />
    </button>
  )
}

export function Card({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn('rounded-console border border-line bg-surface-1', className)}
      {...props}
    />
  )
}

export function Badge({
  tone = 'neutral',
  className,
  children,
}: {
  tone?: 'neutral' | 'accent' | 'ok' | 'danger' | 'info'
  className?: string
  children: ReactNode
}) {
  const tones = {
    neutral: 'border-line-strong bg-surface-3 text-text-secondary',
    accent: 'border-accent/40 bg-accent-muted text-accent',
    ok: 'border-ok/40 bg-ok-muted text-ok',
    danger: 'border-danger/40 bg-danger-muted text-danger',
    info: 'border-info/40 bg-info-muted text-info',
  }
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1 rounded-console-sm border px-1.5 py-0.5 text-[11px] font-medium',
        tones[tone],
        className,
      )}
    >
      {children}
    </span>
  )
}

/** 区块标题：普通字号，不使用全大写宽字距的装饰性 eyebrow。 */
export function SectionTitle({ children, className }: { children: ReactNode; className?: string }) {
  return (
    <h2 className={cn('text-sm font-semibold text-text-primary', className)}>{children}</h2>
  )
}

/**
 * 模态对话框：居中浮层，遮罩点击与 Esc 均可关闭。
 * Modal dialog: a centered overlay closed by clicking the backdrop or pressing Esc.
 */
export function Modal({
  open,
  title,
  onClose,
  children,
  footer,
}: {
  open: boolean
  title: ReactNode
  onClose: () => void
  children: ReactNode
  footer?: ReactNode
}) {
  useEffect(() => {
    if (!open) {
      return
    }
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        event.stopPropagation()
        onClose()
      }
    }
    document.addEventListener('keydown', onKeyDown)
    return () => document.removeEventListener('keydown', onKeyDown)
  }, [open, onClose])

  if (!open) {
    return null
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div
        className="absolute inset-0 bg-surface-0/80"
        onClick={onClose}
        aria-hidden="true"
      />
      <div
        role="dialog"
        aria-modal="true"
        className="relative flex max-h-[80dvh] w-full max-w-3xl flex-col overflow-hidden rounded-console border border-line-strong bg-surface-1 shadow-2xl shadow-surface-0/60"
      >
        <div className="flex shrink-0 items-center gap-2 border-b border-line px-4 py-3">
          <h2 className="text-sm font-semibold text-text-primary">{title}</h2>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close"
            className="ml-auto rounded-console p-1 text-text-secondary transition-colors hover:bg-surface-3 hover:text-text-primary"
          >
            <X size={15} strokeWidth={1.75} />
          </button>
        </div>
        <div className="min-h-0 flex-1 overflow-auto">{children}</div>
        {footer && (
          <div className="flex shrink-0 items-center gap-2 border-t border-line px-4 py-3">
            {footer}
          </div>
        )}
      </div>
    </div>
  )
}
