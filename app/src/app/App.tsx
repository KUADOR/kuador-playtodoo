import { lazy, Suspense } from 'react'
import { resolveRoute } from './routes'
import { HomePage } from './HomePage'

// `import.meta.env.DEV` is replaced at build time, so the catalog is left out of production bundles.
const DesignCatalog = import.meta.env.DEV ? lazy(() => import('../dev/design/DesignCatalog')) : null

export function App() {
  const route = resolveRoute(window.location.pathname, import.meta.env.DEV)

  if (route === 'design-catalog' && DesignCatalog) {
    return (
      <Suspense fallback={null}>
        <DesignCatalog />
      </Suspense>
    )
  }
  return <HomePage />
}
