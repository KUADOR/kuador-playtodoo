import { INITIAL_CAPABILITIES } from '../../../testing/platform-fixtures';
import { draftOf, pendingChanges } from './capability-draft';

describe('capability draft', () => {
  it('starts equal to what the api returned, so nothing is pending', () => {
    const draft = draftOf(INITIAL_CAPABILITIES.items);

    expect(pendingChanges(INITIAL_CAPABILITIES.items, draft)).toEqual([]);
  });

  it('keeps only the capabilities that differ, with their new value', () => {
    const draft = {
      ...draftOf(INITIAL_CAPABILITIES.items),
      COMPLEX_CREATE: true,
      TENANT_DATA_ACCESS: false,
    };

    expect(pendingChanges(INITIAL_CAPABILITIES.items, draft)).toEqual([
      { key: 'COMPLEX_CREATE', enabled: true },
    ]);
  });
});
