import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { ApiError, asApiError } from '../../../core/api/api-error';
import { COMMON_MESSAGES, errorMessage } from '../../../core/api/error-messages';
import { Page } from '../../../core/api/page';
import { Alert } from '../../../shared/ui/alert/alert';
import { Button } from '../../../shared/ui/button/button';
import { Dialog } from '../../../shared/ui/dialog/dialog';
import { LinkButton } from '../../../shared/ui/link-button/link-button';
import { Pagination } from '../../../shared/ui/pagination/pagination';
import { Skeleton } from '../../../shared/ui/skeleton/skeleton';
import { StatusBadge } from '../../../shared/ui/status-badge/status-badge';
import { NEW_PLAN_URL, PLAN_FIELDS_URL, planUrl } from '../plan-paths';
import { PLAN_STATUS_BADGES, limitsLabel, periodicityLabel, priceLabel } from '../plan-texts';
import { Plan, PlansApi } from '../plans-api';
import { PlansNotice } from '../plans-notice';
import { PUBLISH_TEXTS, publishDialogHeading, publishedNotice } from '../publish-texts';

const SKELETON_ROWS = 4;

type PlansState =
  | { readonly status: 'loading' }
  | { readonly status: 'error'; readonly message: string }
  | { readonly status: 'ready'; readonly page: Page<Plan> };

/** Publication the SUPER_ADMIN is confirming, with the reason it failed if it did. */
interface PublishTarget {
  readonly plan: Plan;
  readonly error: string | null;
  /** The api said the plan lacks required data: offer to edit it. */
  readonly incomplete: boolean;
}

/** P10 · Planes: the commercial catalogue, with «Publicar» for drafts (CA3). SUPER_ADMIN only. */
@Component({
  selector: 'app-plans-page',
  imports: [Alert, Button, Dialog, LinkButton, Pagination, RouterLink, Skeleton, StatusBadge],
  templateUrl: './plans-page.html',
  styleUrl: './plans-page.css',
})
export class PlansPage {
  private readonly api = inject(PlansApi);

  protected readonly newPlanUrl = NEW_PLAN_URL;
  protected readonly planFieldsUrl = PLAN_FIELDS_URL;
  protected readonly planUrl = planUrl;
  protected readonly statusBadges = PLAN_STATUS_BADGES;
  protected readonly priceLabel = priceLabel;
  protected readonly periodicityLabel = periodicityLabel;
  protected readonly limitsLabel = limitsLabel;
  protected readonly publishTexts = PUBLISH_TEXTS;
  protected readonly publishDialogHeading = publishDialogHeading;
  protected readonly skeletonRows = SKELETON_ROWS;

  protected readonly state = signal<PlansState>({ status: 'loading' });
  protected readonly notice = signal<string | null>(inject(PlansNotice).take());
  protected readonly publishTarget = signal<PublishTarget | null>(null);
  protected readonly publishing = signal(false);

  constructor() {
    void this.load(0);
  }

  protected async load(page: number): Promise<void> {
    this.state.set({ status: 'loading' });
    try {
      this.state.set({ status: 'ready', page: await firstValueFrom(this.api.list(page)) });
    } catch (error) {
      this.state.set({
        status: 'error',
        message: errorMessage(asApiError(error), COMMON_MESSAGES.loadFailed),
      });
    }
  }

  protected askToPublish(plan: Plan): void {
    this.notice.set(null);
    this.publishTarget.set({ plan, error: null, incomplete: false });
  }

  protected closePublish(): void {
    if (!this.publishing()) {
      this.publishTarget.set(null);
    }
  }

  protected async publish(): Promise<void> {
    const target = this.publishTarget();
    if (target === null || this.publishing()) {
      return;
    }
    this.publishing.set(true);
    try {
      const published = await firstValueFrom(this.api.publish(target.plan.id));
      this.replacePlan(published);
      this.publishTarget.set(null);
      this.notice.set(publishedNotice(published.name));
    } catch (error) {
      this.handlePublishFailure(target, asApiError(error));
    } finally {
      this.publishing.set(false);
    }
  }

  private handlePublishFailure(target: PublishTarget, error: ApiError): void {
    if (error.code === 'PLAN_NOT_FOUND') {
      this.publishTarget.set(null);
      this.notice.set(null);
      void this.load(0);
      return;
    }
    this.publishTarget.set({
      ...target,
      incomplete: error.code === 'PLAN_INCOMPLETE',
      error: errorMessage(error, COMMON_MESSAGES.saveFailed, {
        PLAN_INCOMPLETE: PUBLISH_TEXTS.incomplete,
      }),
    });
  }

  private replacePlan(plan: Plan): void {
    this.state.update((current) =>
      current.status === 'ready'
        ? {
            ...current,
            page: {
              ...current.page,
              items: current.page.items.map((item) => (item.id === plan.id ? plan : item)),
            },
          }
        : current,
    );
  }
}
