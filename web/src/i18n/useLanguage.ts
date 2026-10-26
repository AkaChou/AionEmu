import { useContext } from 'react'
import { LanguageContext, type LanguageContextValue } from './context'

/**
 * 读取语言上下文；必须在 LanguageProvider 内部使用。
 * Reads the language context; it must be used inside a LanguageProvider.
 */
export function useLanguage(): LanguageContextValue {
  const context = useContext(LanguageContext)
  if (!context) {
    throw new Error('useLanguage must be used inside a LanguageProvider')
  }
  return context
}
