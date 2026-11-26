import { Component, ElementRef, input, signal, viewChildren } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

export interface TabLink {
  readonly label: string;
  /** Absolute router link of the tab. */
  readonly link: string;
}

const NEXT_KEYS = ['ArrowRight', 'ArrowDown'];
const PREVIOUS_KEYS = ['ArrowLeft', 'ArrowUp'];

let nextTabNavId = 0;

/**
 * «Pestañas» of «Identidad visual» v3 where each tab has its own address (a router link). Arrow
 * keys move between tabs and Enter opens one; the content goes in the projected tab panel,
 * labelled by the active tab. Under 600 px the tabs scroll horizontally.
 */
@Component({
  selector: 'app-tab-nav',
  imports: [RouterLink, RouterLinkActive],
  template: `
    <div class="tab-nav__list" role="tablist" [attr.aria-label]="label()">
      @for (tab of tabs(); track tab.link; let index = $index) {
        <a
          #tabLink
          #active="routerLinkActive"
          class="tab-nav__tab"
          role="tab"
          [id]="tabId(index)"
          [routerLink]="tab.link"
          routerLinkActive="tab-nav__tab--active"
          [routerLinkActiveOptions]="{ exact: true }"
          (isActiveChange)="$event && activeIndex.set(index)"
          [attr.aria-selected]="active.isActive"
          [attr.aria-controls]="panelId"
          [attr.tabindex]="active.isActive ? 0 : -1"
          (keydown)="moveFocus($event, index)"
          >{{ tab.label }}</a
        >
      }
    </div>
    <div
      class="tab-nav__panel"
      role="tabpanel"
      [id]="panelId"
      [attr.aria-labelledby]="tabId(activeIndex())"
    >
      <ng-content />
    </div>
  `,
  styles: `
    :host {
      --tab-nav-underline: 3px;

      display: flex;
      flex-direction: column;
      gap: var(--space-4);
    }

    .tab-nav__list {
      display: flex;
      gap: var(--space-2);
      overflow-x: auto;
      border-bottom: var(--border-width-sm) solid var(--color-border);
    }

    .tab-nav__tab {
      display: inline-flex;
      flex-shrink: 0;
      align-items: center;
      min-height: var(--size-touch-min);
      padding: 0 var(--space-3);
      border-bottom: var(--tab-nav-underline) solid transparent;
      color: var(--color-fg-muted);
      font-size: var(--font-size-md);
      font-weight: var(--font-weight-semibold);
      text-decoration: none;
    }

    .tab-nav__tab--active {
      border-bottom-color: var(--color-primary);
      color: var(--color-primary);
    }

    .tab-nav__tab:focus-visible {
      outline: var(--border-focus-ring) solid var(--color-focus);
      outline-offset: calc(-1 * var(--border-focus-offset));
    }
  `,
})
export class TabNav {
  readonly tabs = input.required<readonly TabLink[]>();
  readonly label = input.required<string>();

  private readonly id = nextTabNavId++;
  protected readonly panelId = `app-tab-nav-${this.id}-panel`;
  protected readonly activeIndex = signal(0);
  private readonly tabLinks = viewChildren<ElementRef<HTMLAnchorElement>>('tabLink');

  protected tabId(index: number): string {
    return `app-tab-nav-${this.id}-tab-${index}`;
  }

  protected moveFocus(event: KeyboardEvent, index: number): void {
    const step = NEXT_KEYS.includes(event.key) ? 1 : PREVIOUS_KEYS.includes(event.key) ? -1 : 0;
    if (step === 0) {
      return;
    }
    event.preventDefault();
    const links = this.tabLinks();
    links[(index + step + links.length) % links.length].nativeElement.focus();
  }
}
