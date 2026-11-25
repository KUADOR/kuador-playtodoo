import { useRef, useState } from 'react'
import type { KeyboardEvent } from 'react'

// Roving tabindex over a grid of cells (WAI-ARIA APG): only one cell is in the tab order and the
// arrow keys move the focus. Cells that are `false` in the mask are skipped.

export interface GridPosition {
  row: number
  column: number
}

export interface RovingCellProps {
  tabIndex: 0 | -1
  ref: (element: HTMLElement | null) => void
  onFocus: () => void
  onKeyDown: (event: KeyboardEvent<HTMLElement>) => void
}

const ACTIVATION_KEYS = new Set(['Enter', ' '])

function keyOf({ row, column }: GridPosition): string {
  return `${row}:${column}`
}

function focusablePositions(mask: boolean[][]): GridPosition[] {
  return mask.flatMap((cells, row) =>
    cells.flatMap((focusable, column) => (focusable ? [{ row, column }] : [])),
  )
}

function isFocusable(mask: boolean[][], position: GridPosition | null): position is GridPosition {
  return position !== null && mask[position.row]?.[position.column] === true
}

/** The next focusable position for a navigation key; left and right follow reading order, up and down the column. */
function nextPosition(mask: boolean[][], from: GridPosition, key: string): GridPosition | null {
  const ordered = focusablePositions(mask)
  const index = ordered.findIndex((position) => keyOf(position) === keyOf(from))
  const sameColumn = ordered.filter(({ column }) => column === from.column)
  switch (key) {
    case 'ArrowRight':
      return ordered[index + 1] ?? null
    case 'ArrowLeft':
      return ordered[index - 1] ?? null
    case 'ArrowDown':
      return sameColumn.find(({ row }) => row > from.row) ?? null
    case 'ArrowUp':
      return sameColumn.findLast(({ row }) => row < from.row) ?? null
    case 'Home':
      return ordered[0] ?? null
    case 'End':
      return ordered.at(-1) ?? null
    default:
      return null
  }
}

/**
 * @param mask which cells can take the focus.
 * @param preferred the cell that starts in the tab order (e.g. the selection or today), if focusable.
 */
export function useRovingGrid(mask: boolean[][], preferred: GridPosition | null) {
  const [active, setActive] = useState<GridPosition | null>(null)
  const elements = useRef(new Map<string, HTMLElement>())

  const tabStop = [active, preferred].find((position) => isFocusable(mask, position)) ?? focusablePositions(mask)[0]

  function moveFocus(from: GridPosition, key: string): boolean {
    const target = nextPosition(mask, from, key)
    if (!target) return false
    setActive(target)
    elements.current.get(keyOf(target))?.focus()
    return true
  }

  function cellProps(position: GridPosition, onActivate?: () => void): RovingCellProps {
    const key = keyOf(position)
    return {
      tabIndex: tabStop && keyOf(tabStop) === key ? 0 : -1,
      ref: (element) => {
        if (element) elements.current.set(key, element)
        else elements.current.delete(key)
      },
      onFocus: () => setActive(position),
      onKeyDown: (event) => {
        if (ACTIVATION_KEYS.has(event.key)) {
          event.preventDefault()
          onActivate?.()
        } else if (moveFocus(position, event.key)) {
          event.preventDefault()
        }
      },
    }
  }

  return cellProps
}
