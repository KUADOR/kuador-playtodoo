import { Component, computed, input, output } from '@angular/core';
import {
  LucideCircleAlert,
  LucideCircleCheck,
  LucideInfo,
  LucideTriangleAlert,
  LucideX,
} from '@lucide/angular';

export type AlertTone = 'success' | 'warning' | 'error' | 'info';

const ICON_SIZE_PX = 20;
const DISMISS_LABEL = 'Cerrar aviso';

/**
 * Notice of «Identidad visual» v3 («Aviso»). A fixed notice describes a current condition and
 * cannot be dismissed; a result notice (`dismissible`) confirms an action and stays until the
 * user closes it. Errors are announced with role="alert"; the rest with role="status".
 */
@Component({
  selector: 'app-alert',
  imports: [LucideCircleAlert, LucideCircleCheck, LucideInfo, LucideTriangleAlert, LucideX],
  templateUrl: './alert.html',
  styleUrl: './alert.css',
  host: {
    '[attr.data-tone]': 'tone()',
    '[attr.role]': 'role()',
  },
})
export class Alert {
  readonly tone = input.required<AlertTone>();
  readonly heading = input<string>();
  readonly dismissible = input(false);

  readonly dismiss = output<void>();

  protected readonly iconSize = ICON_SIZE_PX;
  protected readonly dismissLabel = DISMISS_LABEL;
  protected readonly role = computed(() => (this.tone() === 'error' ? 'alert' : 'status'));
}
