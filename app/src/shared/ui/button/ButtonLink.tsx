import type { AnchorHTMLAttributes } from 'react'
import a11y from '../a11y.module.css'
import type { ButtonVariant } from './Button'
import styles from './Button.module.css'

export interface ButtonLinkProps extends Omit<AnchorHTMLAttributes<HTMLAnchorElement>, 'children' | 'href'> {
  /** Concrete verb shown on the link; it is also its accessible name. */
  label: string
  href: string
  variant?: ButtonVariant
}

/** A navigation that looks like a `Button` (same variants and touch size), kept as a link for assistive tech. */
export function ButtonLink({ label, href, variant = 'primary', className, ...rest }: ButtonLinkProps) {
  const classNames = [styles.button, styles.link, styles[variant], a11y.focusRing, className].filter(Boolean).join(' ')

  return (
    <a {...rest} href={href} className={classNames}>
      <span>{label}</span>
    </a>
  )
}
