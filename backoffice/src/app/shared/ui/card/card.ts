import { Component, input } from '@angular/core';
import { LucideCheck } from '@lucide/angular';

export type CardVariant = 'complex' | 'court' | 'booking' | 'tournament' | 'news';

const CHECK_ICON_SIZE_PX = 20;
const SELECTED_TEXT = 'Seleccionada';
const LOADING_TEXT = 'Cargando…';

/**
 * Content container for a complex, court, booking, tournament or news item. Follows the
 * shared contract in design-system/docs/components/card.md. Projected content is replaced
 * by a skeleton while `loading` is true.
 */
@Component({
  selector: 'app-card',
  imports: [LucideCheck],
  templateUrl: './card.html',
  styleUrl: './card.css',
  host: {
    '[attr.data-variant]': 'variant()',
    '[class.card--selected]': 'selected()',
    '[attr.aria-busy]': 'loading() || null',
  },
})
export class Card {
  readonly variant = input.required<CardVariant>();
  readonly selected = input(false);
  readonly loading = input(false);

  protected readonly checkIconSize = CHECK_ICON_SIZE_PX;
  protected readonly selectedText = SELECTED_TEXT;
  protected readonly loadingText = LOADING_TEXT;
}
