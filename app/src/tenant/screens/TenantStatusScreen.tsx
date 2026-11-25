import { useEffect, useRef } from 'react'
import type { ReactNode } from 'react'
import { Lock, Search } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import { Alert, Button, ButtonLink, Card } from '../../shared/ui'
import type { ButtonVariant } from '../../shared/ui'
import { TENANT_TEXT } from '../model/tenantText'
import styles from './TenantStatusScreen.module.css'

export type BlockedTenantKind = 'unavailable' | 'not-found'

interface BlockedContent {
  Icon: LucideIcon
  title: string
  text: string
  actionVariant: ButtonVariant
}

// Pantallas PLATODO-26: both screens look alike so «does not exist» is not told apart from «not available»
// more than needed; only «not found» makes the way out the primary action.
const BLOCKED_CONTENT: Record<BlockedTenantKind, BlockedContent> = {
  unavailable: {
    Icon: Lock,
    title: TENANT_TEXT.unavailableTitle,
    text: TENANT_TEXT.unavailableText,
    actionVariant: 'secondary',
  },
  'not-found': {
    Icon: Search,
    title: TENANT_TEXT.notFoundTitle,
    text: TENANT_TEXT.notFoundText,
    actionVariant: 'primary',
  },
}

function StatusLayout({ children }: { children: ReactNode }) {
  return (
    <main className={styles.page}>
      <div className={styles.column}>
        <p className={styles.brand}>{TENANT_TEXT.brand}</p>
        {children}
      </div>
    </main>
  )
}

/** Skeleton of the status card while the subdomain is resolved. */
export function TenantCheckingScreen() {
  return (
    <StatusLayout>
      <Card variant="complex" loading className={styles.card} />
    </StatusLayout>
  )
}

/** The check failed (network or unexpected answer). It is not «not available»: the person can retry. */
export function TenantCheckFailedScreen({ onRetry }: { onRetry: () => void }) {
  return (
    <StatusLayout>
      <Alert
        variant="error"
        title={TENANT_TEXT.checkFailed}
        actions={<Button variant="text" label={TENANT_TEXT.retry} onClick={onRetry} />}
      />
    </StatusLayout>
  )
}

export interface TenantBlockedScreenProps {
  kind: BlockedTenantKind
  platformUrl: string
}

/** «Complejo no disponible» or «Complejo no encontrado». Focus starts on the title so it is read first. */
export function TenantBlockedScreen({ kind, platformUrl }: TenantBlockedScreenProps) {
  const { Icon, title, text, actionVariant } = BLOCKED_CONTENT[kind]
  const titleRef = useRef<HTMLHeadingElement>(null)

  useEffect(() => {
    titleRef.current?.focus()
  }, [kind])

  return (
    <StatusLayout>
      <Card variant="complex" className={styles.card}>
        <div className={styles.content}>
          <Icon className={styles.icon} aria-hidden="true" />
          <h1 ref={titleRef} tabIndex={-1} className={styles.title}>
            {title}
          </h1>
          <p className={styles.text}>{text}</p>
          <ButtonLink href={platformUrl} variant={actionVariant} label={TENANT_TEXT.goToPlatform} className={styles.action} />
        </div>
      </Card>
    </StatusLayout>
  )
}
