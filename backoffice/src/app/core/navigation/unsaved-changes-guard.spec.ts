import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { SUPER_ADMIN_USER } from '../../../testing/platform-fixtures';
import { provideTestApi, signInAs } from '../../../testing/platform-testing';
import { LeaveConfirmation, unsavedChangesGuard } from './unsaved-changes-guard';

describe('unsavedChangesGuard', () => {
  const page: LeaveConfirmation = { confirmLeave: () => false };

  function check(): unknown {
    return TestBed.runInInjectionContext(() =>
      unsavedChangesGuard(
        page,
        {} as ActivatedRouteSnapshot,
        {} as RouterStateSnapshot,
        {} as RouterStateSnapshot,
      ),
    );
  }

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [provideTestApi()] });
  });

  it('asks the page while there is a session', async () => {
    await signInAs(SUPER_ADMIN_USER);

    expect(check()).toBe(false);
  });

  it('lets the person leave without asking when there is no session', () => {
    expect(check()).toBe(true);
  });
});
