import type { ButtonHTMLAttributes, MouseEvent } from 'react'
import { LoaderCircle } from 'lucide-react'
import a11y from '../a11y.module.css'
import styles from './Button.module.css'

export type ButtonVariant = 'primary' | 'secondary' | 'danger' | 'text'

export const DEFAULT_LOADING_LABEL = 'Guardando…'

export interface ButtonProps
  extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'children' | 'aria-busy' | 'aria-disabled'> {
  /** Concrete verb shown on the button; it is also its accessible name. */
  label: string
  variant?: ButtonVariant
  /** Shows a spinner, blocks activation and announces the busy state. */
  loading?: boolean
  /** Text shown while `loading`. */
  loadingLabel?: string
}

export function Button({
  label,
  variant = 'primary',
  loading = false,
  loadingLabel = DEFAULT_LOADING_LABEL,
  disabled = false,
  type = 'button',
  className,
  onClick,
  ...rest
}: ButtonProps) {
  // While loading the button stays focusable (aria-disabled instead of disabled) so keyboard focus is not lost.
  const handleClick = (event: MouseEvent<HTMLButtonElement>) => {
    if (loading) {
      event.preventDefault()
      return
    }
    onClick?.(event)
  }

  const classNames = [styles.button, styles[variant], a11y.focusRing, className].filter(Boolean).join(' ')

  return (
    <button
      {...rest}
      type={type}
      className={classNames}
      disabled={disabled}
      aria-disabled={loading || undefined}
      aria-busy={loading || undefined}
      data-loading={loading || undefined}
      onClick={handleClick}
    >
      {loading && <LoaderCircle className={styles.spinner} aria-hidden="true" />}
      <span>{loading ? loadingLabel : label}</span>
    </button>
  )
}
