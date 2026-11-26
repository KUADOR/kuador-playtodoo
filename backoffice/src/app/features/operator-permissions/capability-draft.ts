import { CapabilityToggle, OperatorCapability } from './operator-capabilities-api';

/** Enablement the SUPER_ADMIN is editing, by capability key. */
export type CapabilityDraft = Readonly<Record<string, boolean>>;

export function draftOf(capabilities: readonly OperatorCapability[]): CapabilityDraft {
  return Object.fromEntries(capabilities.map((capability) => [capability.key, capability.enabled]));
}

/** Only the capabilities whose enablement differs from what the api returned. */
export function pendingChanges(
  capabilities: readonly OperatorCapability[],
  draft: CapabilityDraft,
): CapabilityToggle[] {
  return capabilities
    .filter((capability) => (draft[capability.key] ?? capability.enabled) !== capability.enabled)
    .map((capability) => ({ key: capability.key, enabled: !capability.enabled }));
}
