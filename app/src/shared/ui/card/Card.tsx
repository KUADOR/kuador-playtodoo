import type { HTMLAttributes, ReactNode } from 'react'
import { Check } from 'lucide-react'
import a11y from '../a11y.module.css'
import styles from './Card.module.css'

export type CardVariant = 'complex' | 'court' | 'booking' | 'tournament' | 'news'

export const SELECTED_LABEL = 'Seleccionada'
export const LOADING_LABEL = 'Cargando…'

export interface CardProps extends Omit<HTMLAttributes<HTMLDivElement>, 'aria-busy'> {
  /** What the card represents. */
  variant: CardVariant
  selected?: boolean
  /** Shows a skeleton instead of the content. */
  loading?: boolean
  children?: ReactNode
}

export function Card({ variant, selected = false, loading = false, className, children, ...rest }: CardProps) {
  const classNames = [styles.card, className].filter(Boolean).join(' ')

  return (
    <div
      {...rest}
      className={classNames}
      data-variant={variant}
      data-selected={selected || undefined}
      aria-busy={loading || undefined}
    >
      {selected && (
        <span className={styles.selectedMark}>
          <Check className={styles.icon} aria-hidden="true" />
          <span className={a11y.visuallyHidden}>{SELECTED_LABEL}</span>
        </span>
      )}
      {loading ? <CardSkeleton /> : children}
    </div>
  )
}

function CardSkeleton() {
  return (
    <div className={styles.skeleton}>
      <span className={a11y.visuallyHidden}>{LOADING_LABEL}</span>
      <span className={`${styles.skeletonLine} ${styles.skeletonTitle}`} aria-hidden="true" />
      <span className={styles.skeletonLine} aria-hidden="true" />
      <span className={`${styles.skeletonLine} ${styles.skeletonShort}`} aria-hidden="true" />
    </div>
  )
}
