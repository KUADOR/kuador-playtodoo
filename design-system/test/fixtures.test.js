import assert from 'node:assert/strict';
import { readFile, readdir } from 'node:fs/promises';
import { describe, it } from 'node:test';
import { expectedOutput } from './support/calendar-reference.js';

const FIXTURES_DIR = new URL('../fixtures/calendar/', import.meta.url);
const REQUIRED_FIXTURES = [
  'day-30min', 'day-60min', 'day-90min', 'day-empty', 'day-all-occupied', 'week-mixed', 'month-mixed',
];
const DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/;
const TIME_PATTERN = /^([01]\d|2[0-3]):[0-5]\d$/;
const VIEWS = ['day', 'week', 'month'];
const MODES = ['editable', 'readonly'];
const SLOT_STATUSES = ['available', 'occupied'];

async function loadFixtures() {
  const files = (await readdir(FIXTURES_DIR)).filter((file) => file.endsWith('.json'));
  return Promise.all(files.map(async (file) => JSON.parse(await readFile(new URL(file, FIXTURES_DIR), 'utf8'))));
}

function assertValidInput({ name, input }) {
  assert.ok(Number.isInteger(input.slotDurationMinutes) && input.slotDurationMinutes > 0, `${name}: slotDurationMinutes`);
  assert.ok(VIEWS.includes(input.view), `${name}: view`);
  assert.ok(MODES.includes(input.mode), `${name}: mode`);
  assert.match(input.today, DATE_PATTERN, `${name}: today`);
  for (const day of input.days) {
    assert.match(day.date, DATE_PATTERN, `${name}: day date`);
    const starts = day.slots.map((slot) => slot.start);
    assert.equal(new Set(starts).size, starts.length, `${name}: duplicated slot on ${day.date}`);
    for (const slot of day.slots) {
      assert.match(slot.start, TIME_PATTERN, `${name}: slot start`);
      assert.ok(SLOT_STATUSES.includes(slot.status), `${name}: slot status`);
    }
  }
}

describe('calendar fixtures', () => {
  it('include every case required by the contract', async () => {
    const names = (await loadFixtures()).map((fixture) => fixture.name);
    REQUIRED_FIXTURES.forEach((required) => assert.ok(names.includes(required), `missing fixture ${required}`));
  });

  it('are named like their file and have a valid input', async () => {
    const files = (await readdir(FIXTURES_DIR)).filter((file) => file.endsWith('.json'));
    const fixtures = await loadFixtures();
    fixtures.forEach((fixture, index) => {
      assert.equal(`${fixture.name}.json`, files[index]);
      assertValidInput(fixture);
    });
  });

  it('declare the expected output that the contract produces', async () => {
    for (const fixture of await loadFixtures()) {
      assert.deepEqual(fixture.expected, expectedOutput(fixture.input), fixture.name);
    }
  });

  it('show the same day with three slot durations', async () => {
    const fixtures = await loadFixtures();
    const [thirty, sixty, ninety] = ['day-30min', 'day-60min', 'day-90min'].map((name) =>
      fixtures.find((fixture) => fixture.name === name),
    );
    assert.equal(thirty.input.today, sixty.input.today);
    assert.equal(sixty.input.today, ninety.input.today);
    assert.deepEqual(thirty.expected.day.slots.map((slot) => slot.label), ['18:00–18:30', '18:30–19:00', '19:00–19:30', '19:30–20:00']);
    assert.deepEqual(sixty.expected.day.slots.map((slot) => slot.label), ['18:00–19:00', '19:00–20:00', '20:00–21:00', '21:00–22:00']);
    assert.deepEqual(ninety.expected.day.slots.map((slot) => slot.label), ['17:00–18:30', '18:30–20:00', '20:00–21:30', '22:30–00:00']);
  });

  it('never select an occupied slot nor anything in readonly mode', async () => {
    const fixtures = await loadFixtures();
    assert.equal(fixtures.find((fixture) => fixture.name === 'day-30min').expected.selected, null);
    assert.equal(fixtures.find((fixture) => fixture.name === 'day-all-occupied').expected.selected, null);
    const readonly = fixtures.find((fixture) => fixture.name === 'week-readonly');
    assert.equal(readonly.expected.selected, null);
    assert.ok(readonly.expected.week.cells.flat().every((status) => status !== 'selected'));
  });
});
