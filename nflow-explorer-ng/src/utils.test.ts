import { formatRelativeTime, formatTimestamp } from "./utils";
import { addDays, subDays } from "date-fns";

test("formatRelativeTime", () => {
    const now = new Date('2021-05-26T06:25:40.722Z');

    expect(formatRelativeTime('2021-05-26T06:25:40.722Z', now)).toBe('less than a minute ago');
    expect(formatRelativeTime(now, now)).toBe('less than a minute ago');

    expect(formatRelativeTime(new Date('2021-05-26T06:25:40.999Z'), now)).toBe('in less than a minute');
    expect(formatRelativeTime(new Date('2021-05-26T06:25:40.000Z'), now)).toBe('less than a minute ago');

    expect(formatRelativeTime(addDays(now, 150), now)).toBe('in 5 months');
    expect(formatRelativeTime(subDays(now, 100), now)).toBe('3 months ago');
});

test('formatTimestamp', () => {
    const now = new Date('2021-05-26T06:25:40.722Z');
    // TODO what timezone?
    expect(formatTimestamp(now)).toBe('2021-05-26 09:25:40');
});
