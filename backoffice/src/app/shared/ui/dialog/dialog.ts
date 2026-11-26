import {
  Component,
  ElementRef,
  OnDestroy,
  afterNextRender,
  inject,
  input,
  output,
  viewChild,
} from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { focusableElements, keepFocusInside } from '../../../core/a11y/focus-trap';

const ESCAPE_KEY = 'Escape';
/** Mark the element (or a wrapper of it) that should get the focus when the dialog opens. */
export const DIALOG_INITIAL_FOCUS_ATTRIBUTE = 'data-dialog-initial-focus';

let nextDialogId = 0;

/**
 * Modal dialog of «Identidad visual» v3. Render it with `@if`: it traps the focus, starts on the
 * element marked with `data-dialog-initial-focus` (or the first focusable one), closes with
 * Escape unless `busy`, and gives the focus back to the trigger when it is removed.
 */
@Component({
  selector: 'app-dialog',
  templateUrl: './dialog.html',
  styleUrl: './dialog.css',
})
export class Dialog implements OnDestroy {
  readonly heading = input.required<string>();
  /** While true (for example saving), the dialog cannot be closed. */
  readonly busy = input(false);

  readonly closeRequest = output<void>();

  protected readonly headingId = `app-dialog-${nextDialogId++}-title`;
  private readonly panel = viewChild.required<ElementRef<HTMLElement>>('panel');
  private readonly trigger = inject(DOCUMENT).activeElement as HTMLElement | null;

  constructor() {
    afterNextRender(() => this.focusInitialElement());
  }

  ngOnDestroy(): void {
    this.trigger?.focus();
  }

  protected handleKeydown(event: KeyboardEvent): void {
    if (event.key === ESCAPE_KEY) {
      event.preventDefault();
      if (!this.busy()) {
        this.closeRequest.emit();
      }
      return;
    }
    keepFocusInside(event, this.panel().nativeElement);
  }

  private focusInitialElement(): void {
    const panel = this.panel().nativeElement;
    const marked = panel.querySelector<HTMLElement>(`[${DIALOG_INITIAL_FOCUS_ATTRIBUTE}]`);
    const target = marked ? (focusableElements(marked)[0] ?? marked) : focusableElements(panel)[0];
    (target ?? panel).focus();
  }
}
