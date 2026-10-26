import { useCallback, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import {
  dictionaries,
  formatMessage,
  type Language,
} from './dictionaries'
import { LanguageContext, type LanguageContextValue } from './context'

const STORAGE_KEY = 'aion-console-language'

/**
 * 读取持久化的语言偏好；默认中文。
 * Reads the persisted language preference; Chinese is the default.
 */
function initialLanguage(): Language {
  const stored = window.localStorage.getItem(STORAGE_KEY)
  return stored === 'en' || stored === 'zh' ? stored : 'zh'
}

/**
 * 提供语言上下文。
 * Provides the language context.
 */
export function LanguageProvider({ children }: { children: ReactNode }) {
  const [language, setLanguageState] = useState<Language>(initialLanguage)

  useEffect(() => {
    document.documentElement.lang = language === 'zh' ? 'zh-CN' : 'en'
  }, [language])

  const setLanguage = useCallback((next: Language) => {
    setLanguageState(next)
    window.localStorage.setItem(STORAGE_KEY, next)
  }, [])

  const t = useCallback(
    (key: import('./dictionaries').MessageKey, args?: Record<string, string | number>) =>
      formatMessage(dictionaries[language][key], args),
    [language],
  )

  const value = useMemo<LanguageContextValue>(
    () => ({ language, setLanguage, t }),
    [language, setLanguage, t],
  )

  return <LanguageContext.Provider value={value}>{children}</LanguageContext.Provider>
}
