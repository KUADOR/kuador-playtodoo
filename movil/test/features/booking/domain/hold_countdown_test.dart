import 'package:flutter_test/flutter_test.dart';
import 'package:playtodoo/features/booking/domain/hold_countdown.dart';

void main() {
  final createdAt = DateTime.utc(2026, 12, 4, 23);
  final expiresAt = createdAt.add(const Duration(minutes: 5));

  group('HoldCountdown', () {
    test('counts down from the server expiresAt', () {
      final countdown = HoldCountdown.fromServer(
        createdAt: createdAt,
        expiresAt: expiresAt,
        receivedAt: createdAt,
      );

      expect(countdown.remainingAt(createdAt), const Duration(minutes: 5));
      expect(
        countdown.remainingAt(createdAt.add(const Duration(seconds: 28))),
        const Duration(minutes: 4, seconds: 32),
      );
    });

    test('rounds partial seconds up and never goes below zero', () {
      final countdown = HoldCountdown.fromServer(
        createdAt: createdAt,
        expiresAt: expiresAt,
        receivedAt: createdAt,
      );

      expect(
        countdown.remainingAt(
          expiresAt.subtract(const Duration(milliseconds: 200)),
        ),
        const Duration(seconds: 1),
      );
      expect(
        countdown.remainingAt(expiresAt.add(const Duration(minutes: 1))),
        Duration.zero,
      );
    });

    test('a device clock two minutes behind the server shows the server '
        'remaining time', () {
      final deviceReceivedAt = createdAt.subtract(const Duration(minutes: 2));
      final countdown = HoldCountdown.fromServer(
        createdAt: createdAt,
        expiresAt: expiresAt,
        receivedAt: deviceReceivedAt,
      );

      expect(
        countdown.remainingAt(deviceReceivedAt.add(const Duration(minutes: 1))),
        const Duration(minutes: 4),
      );
    });

    test('fraction left goes from 1 to 0', () {
      final countdown = HoldCountdown.fromServer(
        createdAt: createdAt,
        expiresAt: expiresAt,
        receivedAt: createdAt,
      );

      expect(countdown.fractionLeft(const Duration(minutes: 5)), 1);
      expect(countdown.fractionLeft(const Duration(seconds: 150)), 0.5);
      expect(countdown.fractionLeft(Duration.zero), 0);
    });

    test('the last minute is a warning', () {
      expect(HoldCountdown.isWarning(const Duration(seconds: 61)), isFalse);
      expect(HoldCountdown.isWarning(const Duration(minutes: 1)), isTrue);
    });
  });

  group('milestoneBetween', () {
    Duration s(int seconds) => Duration(seconds: seconds);

    test('reports 1:00, 0:30 and the end when crossed', () {
      expect(
        HoldCountdown.milestoneBetween(s(61), s(60)),
        HoldMilestone.oneMinuteLeft,
      );
      expect(
        HoldCountdown.milestoneBetween(s(31), s(30)),
        HoldMilestone.thirtySecondsLeft,
      );
      expect(
        HoldCountdown.milestoneBetween(s(1), Duration.zero),
        HoldMilestone.expired,
      );
    });

    test('is silent between milestones', () {
      expect(HoldCountdown.milestoneBetween(s(299), s(298)), isNull);
      expect(HoldCountdown.milestoneBetween(s(60), s(59)), isNull);
    });

    test('after a jump only the latest milestone is reported', () {
      expect(
        HoldCountdown.milestoneBetween(s(200), s(20)),
        HoldMilestone.thirtySecondsLeft,
      );
      expect(
        HoldCountdown.milestoneBetween(s(200), Duration.zero),
        HoldMilestone.expired,
      );
    });
  });

  test('formatRemaining shows minutes and two-digit seconds', () {
    expect(formatRemaining(const Duration(minutes: 4, seconds: 32)), '4:32');
    expect(formatRemaining(const Duration(seconds: 5)), '0:05');
    expect(formatRemaining(const Duration(minutes: 5)), '5:00');
  });
}
