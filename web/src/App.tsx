import { useEffect, useState } from 'react'
import { session } from './api/client'
import { Console } from './components/Console'
import { LoginView } from './components/LoginView'
import { useLanguage } from './i18n/useLanguage'

/**
 * 控制台根组件：按会话状态在登录视图与主视图之间切换。
 * Console root: switches between the login view and the main view based on the session state.
 */
export default function App() {
  const { t } = useLanguage()
  const [authenticated, setAuthenticated] = useState<boolean | null>(null)

  useEffect(() => {
    let cancelled = false
    void (async () => {
      try {
        const state = await session()
        if (!cancelled) {
          setAuthenticated(state.authenticated)
        }
      } catch {
        if (!cancelled) {
          setAuthenticated(false)
        }
      }
    })()
    return () => {
      cancelled = true
    }
  }, [])

  if (authenticated === null) {
    return (
      <div className="flex min-h-[100dvh] items-center justify-center">
        <span className="text-sm text-text-muted">{t('login.pending')}</span>
      </div>
    )
  }

  if (!authenticated) {
    return <LoginView onAuthenticated={() => setAuthenticated(true)} />
  }

  return <Console onLogout={() => setAuthenticated(false)} />
}
