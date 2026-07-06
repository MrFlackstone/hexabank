import { useTranslation } from 'react-i18next';

const LANGS = ['es', 'en'] as const;

// Selector de idioma (ES | EN). La eleccion se persiste en localStorage por el detector.
export function LanguageSwitcher() {
  const { i18n, t } = useTranslation();
  const active = i18n.resolvedLanguage;

  return (
    <div className="lang-switcher" role="group" aria-label={t('common.language')}>
      {LANGS.map((lng) => (
        <button
          key={lng}
          className={`lang-option ${active === lng ? 'lang-active' : ''}`}
          aria-pressed={active === lng}
          onClick={() => void i18n.changeLanguage(lng)}
        >
          {lng.toUpperCase()}
        </button>
      ))}
    </div>
  );
}
