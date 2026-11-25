interface ImportMetaEnv {
  /** Root URL of Playtodoo for this environment (destination of «Ir a Playtodoo»). Optional. */
  readonly VITE_PLATFORM_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
