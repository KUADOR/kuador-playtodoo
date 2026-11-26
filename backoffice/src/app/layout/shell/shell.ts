import {
  Component,
  ElementRef,
  Injector,
  afterNextRender,
  computed,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import {
  LucideBuilding2,
  LucideLayers,
  LucideLogOut,
  LucideMenu,
  LucideUserCog,
  LucideX,
} from '@lucide/angular';
import { filter } from 'rxjs';
import { keepFocusInside } from '../../core/a11y/focus-trap';
import { canAccess } from '../../core/auth/access';
import { ROLE_LABELS } from '../../core/auth/platform-user';
import { Session } from '../../core/auth/session';
import { LOGIN_URL } from '../../core/app-paths';
import { NAVIGATION_ITEMS } from './navigation';

const ICON_SIZE_PX = 20;
const ESCAPE_KEY = 'Escape';

/**
 * Layout of every signed-in screen: skip link, side menu with only the allowed sections (a
 * panel under 960 px), the user's role and «Cerrar sesión». After each navigation the focus
 * moves to the page `h1`.
 */
@Component({
  selector: 'app-shell',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    LucideBuilding2,
    LucideLayers,
    LucideLogOut,
    LucideMenu,
    LucideUserCog,
    LucideX,
  ],
  templateUrl: './shell.html',
  styleUrl: './shell.css',
})
export class Shell {
  private readonly session = inject(Session);
  private readonly router = inject(Router);
  private readonly injector = inject(Injector);

  protected readonly iconSize = ICON_SIZE_PX;
  protected readonly menuOpen = signal(false);
  protected readonly user = this.session.user;
  protected readonly roleLabel = computed(() => {
    const user = this.user();
    return user ? ROLE_LABELS[user.role] : '';
  });
  protected readonly items = computed(() =>
    NAVIGATION_ITEMS.filter((item) => canAccess(this.user(), item.access)),
  );

  private readonly navigationPanel = viewChild.required<ElementRef<HTMLElement>>('navigationPanel');
  private readonly menuButton = viewChild.required<ElementRef<HTMLButtonElement>>('menuButton');
  private readonly main = viewChild.required<ElementRef<HTMLElement>>('main');

  constructor() {
    this.router.events
      .pipe(
        filter((event) => event instanceof NavigationEnd),
        takeUntilDestroyed(),
      )
      .subscribe(() => {
        this.menuOpen.set(false);
        afterNextRender(() => this.focusPageHeading(), { injector: this.injector });
      });
  }

  protected openMenu(): void {
    this.menuOpen.set(true);
    afterNextRender(() => this.navigationPanel().nativeElement.querySelector('a')?.focus(), {
      injector: this.injector,
    });
  }

  protected closeMenu(): void {
    this.menuOpen.set(false);
    // The menu button is inert while the panel is open: focus it once that is rendered away.
    afterNextRender(() => this.menuButton().nativeElement.focus(), { injector: this.injector });
  }

  protected handlePanelKeydown(event: KeyboardEvent): void {
    if (!this.menuOpen()) {
      return;
    }
    if (event.key === ESCAPE_KEY) {
      event.preventDefault();
      this.closeMenu();
      return;
    }
    keepFocusInside(event, this.navigationPanel().nativeElement);
  }

  protected signOut(): void {
    this.session.signOut();
    void this.router.navigateByUrl(LOGIN_URL);
  }

  protected skipToContent(event: Event): void {
    event.preventDefault();
    this.focusPageHeading();
  }

  private focusPageHeading(): void {
    const main = this.main().nativeElement;
    const heading = main.querySelector<HTMLElement>('h1') ?? main;
    heading.setAttribute('tabindex', '-1');
    heading.focus();
  }
}
