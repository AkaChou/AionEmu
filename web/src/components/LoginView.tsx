import { useState, type FormEvent } from 'react'
import { KeyRound, Loader2 } from 'lucide-react'
import { ApiError, login } from '../api/client'
import { useLanguage } from '../i18n/useLanguage'
import { Button, Input } from './ui'

/**
 * 登录视图：控制台的唯一入口门槛。
 * Login view: the single entry gate of the console.
 */
export function LoginView({ onAuthenticated }: { onAuthenticated: () => void }) {
  const { t } = useLanguage()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setBusy(true)
    try {
      await login(username, password)
      onAuthenticated()
    } catch (cause) {
      setError(
        cause instanceof ApiError && cause.status === 401
          ? t('login.failed')
          : t('message.readFailed'),
      )
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="flex min-h-[100dvh] items-center justify-center px-4 py-12">
      <form
        onSubmit={handleSubmit}
        className="w-full max-w-sm rounded-console border border-line bg-surface-1 p-6"
      >
        <div className="mb-6 flex items-center gap-2.5">
          <span className="flex h-9 w-9 items-center justify-center rounded-console bg-accent-muted text-accent">
            <KeyRound size={18} strokeWidth={1.75} />
          </span>
          <div>
            <h1 className="text-base font-semibold text-text-primary">{t('app.title')}</h1>
            <p className="text-xs text-text-muted">{t('app.subtitle')}</p>
          </div>
        </div>

        <div className="flex flex-col gap-3">
          <label className="flex flex-col gap-1.5">
            <span className="text-xs font-medium text-text-secondary">{t('login.username')}</span>
            <Input
              value={username}
              onChange={(event) => setUsername(event.target.value)}
              autoComplete="username"
              autoFocus
              required
            />
          </label>
          <label className="flex flex-col gap-1.5">
            <span className="text-xs font-medium text-text-secondary">{t('login.password')}</span>
            <Input
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              autoComplete="current-password"
              required
            />
          </label>
        </div>

        {error && (
          <p role="alert" className="mt-3 text-xs text-danger">
            {error}
          </p>
        )}

        <Button type="submit" variant="primary" className="mt-5 w-full" disabled={busy}>
          {busy ? <Loader2 size={15} className="animate-spin" /> : null}
          {busy ? t('login.pending') : t('login.submit')}
        </Button>
      </form>
    </div>
  )
}
