import { createContext } from 'react'
import type { Language } from './dictionaries'

/**
 * 语言上下文：默认中文，切换后立即生效并持久化到本地存储。
 * The language context: Chinese by default, a switch takes effect immediately and is persisted in the local
 * storage.
 */
export interface LanguageContextValue {
  language: Language
  setLanguage: (language: Language) => void
  t: (key: import('./dictionaries').MessageKey, args?: Record<string, string | number>) => string
}

export const LanguageContext = createContext<LanguageContextValue | null>(null)
