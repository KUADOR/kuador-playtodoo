import type { ReactNode } from 'react'
import { CircleAlert, CircleCheck, Info, TriangleAlert } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import styles from './Alert.module.css'

export type AlertVariant = 'error' | 'warning' | 'success' | 'info'

const ICONS: Record<AlertVariant, LucideIcon> = {
  error: CircleAlert,
  warning: TriangleAlert,
  success: CircleCheck,
  info: Info,
}

export interface AlertProps {
  variant: AlertVariant
  /** Short title; the state is never told only with color. */
  title: string
  children?: ReactNode
  /** Links or buttons that resolve the situation. */
  actions?: ReactNode
  id?: string
}

/** Inline alert (Pantallas · componente 3). Errors interrupt screen readers; the rest are polite. */
export function Alert({ variant, title, children, actions, id }: AlertProps) {
  const Icon = ICONS[variant]
  return (
    <div id={id} role={variant === 'error' ? 'alert' : 'status'} className={styles.alert} data-variant={variant}>
      <Icon className={styles.icon} aria-hidden="true" />
      <div className={styles.body}>
        <p className={styles.title}>{title}</p>
        {children && <div className={styles.text}>{children}</div>}
        {actions && <div className={styles.actions}>{actions}</div>}
      </div>
    </div>
  )
}
