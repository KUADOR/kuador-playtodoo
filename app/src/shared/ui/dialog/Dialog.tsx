import { useEffect, useId, useRef } from 'react'
import type { KeyboardEvent, ReactNode } from 'react'
import { createPortal } from 'react-dom'
import a11y from '../a11y.module.css'
import styles from './Dialog.module.css'

const FOCUSABLE = [
  'a[href]',
  'button:not([disabled])',
  'input:not([disabled])',
  'select:not([disabled])',
  'textarea:not([disabled])',
  '[tabindex]:not([tabindex="-1"])',
].join(',')

export interface DialogProps {
  title: string
  /** Esc, the backdrop or a close action was used. The caller unmounts the dialog. */
  onClose: () => void
  /** Optional text read with the title (`aria-describedby`). */
  description?: string
  children?: ReactNode
  /** Buttons at the bottom; stacked on narrow screens. */
  actions?: ReactNode
}

/**
 * Modal dialog (Pantallas · componente 3): traps the focus, closes with Esc and returns the focus to
 * the element that opened it. It is rendered while mounted; nested dialogs stack on top.
 */
export function Dialog({ title, onClose, description, children, actions }: DialogProps) {
  const backdropRef = useRef<HTMLDivElement>(null)
  const panelRef = useRef<HTMLDivElement>(null)
  const titleId = useId()
  const descriptionId = useId()

  useEffect(() => {
    const opener = document.activeElement instanceof HTMLElement ? document.activeElement : null
    const restoreBackground = makeBackgroundInert(backdropRef.current)
    const panel = panelRef.current
    const firstFocusable = panel?.querySelector<HTMLElement>(FOCUSABLE)
    ;(firstFocusable ?? panel)?.focus()
    return () => {
      restoreBackground()
      opener?.focus()
    }
  }, [])

  function handleKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    if (event.key === 'Escape') {
      // Only the top dialog reacts: the event must not reach a dialog underneath (React bubbles across portals).
      event.stopPropagation()
      onClose()
    } else if (event.key === 'Tab') {
      event.stopPropagation()
      keepFocusInside(event)
    }
  }

  function keepFocusInside(event: KeyboardEvent<HTMLDivElement>) {
    const focusables = [...(panelRef.current?.querySelectorAll<HTMLElement>(FOCUSABLE) ?? [])]
    if (focusables.length === 0) {
      event.preventDefault()
      return
    }
    const first = focusables[0]
    const last = focusables[focusables.length - 1]
    const active = document.activeElement
    if (event.shiftKey && (active === first || active === panelRef.current)) {
      event.preventDefault()
      last.focus()
    } else if (!event.shiftKey && active === last) {
      event.preventDefault()
      first.focus()
    }
  }

  return createPortal(
    <div ref={backdropRef} className={styles.backdrop} onMouseDown={(event) => event.target === event.currentTarget && onClose()}>
      <div
        ref={panelRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        aria-describedby={description ? descriptionId : undefined}
        tabIndex={-1}
        className={`${styles.panel} ${a11y.focusRing}`}
        onKeyDown={handleKeyDown}
      >
        <h2 id={titleId} className={styles.title}>
          {title}
        </h2>
        {description && (
          <p id={descriptionId} className={styles.description}>
            {description}
          </p>
        )}
        {children}
        {actions && <div className={styles.actions}>{actions}</div>}
      </div>
    </div>,
    document.body,
  )
}

/**
 * Everything outside the dialog becomes `inert`: unreachable by keyboard, pointer and screen readers.
 * A nested dialog also makes the dialog underneath inert. Returns the function that undoes it.
 */
function makeBackgroundInert(dialogRoot: HTMLElement | null): () => void {
  const background = [...document.body.children].filter(
    (element) => element !== dialogRoot && !element.hasAttribute('inert'),
  )
  background.forEach((element) => element.setAttribute('inert', ''))
  return () => background.forEach((element) => element.removeAttribute('inert'))
}
