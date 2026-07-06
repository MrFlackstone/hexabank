import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import LanguageDetector from 'i18next-browser-languagedetector';
import { es } from './es';
import { en } from './en';

// i18n del SPA. Idioma por defecto español; detecta el guardado (localStorage) o el del navegador y
// cae a 'es'. Un unico namespace (translation). Se importa por efecto en main.tsx antes de renderizar.
void i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources: { es: { translation: es }, en: { translation: en } },
    fallbackLng: 'es',
    supportedLngs: ['es', 'en'],
    detection: { order: ['localStorage', 'navigator'], caches: ['localStorage'] },
    interpolation: { escapeValue: false }, // React ya escapa; evita doble escape
  });

// Mantiene <html lang> sincronizado con el idioma activo (accesibilidad/SEO).
const applyDocumentLang = (lng: string) => {
  document.documentElement.lang = lng;
};
applyDocumentLang(i18n.resolvedLanguage ?? 'es');
i18n.on('languageChanged', applyDocumentLang);

export default i18n;
