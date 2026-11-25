import 'package:flutter/foundation.dart';

/// Moments of the hold that are announced to screen readers. The time is not
/// read every second (Pantallas · componente 2).
enum HoldMilestone { oneMinuteLeft, thirtySecondsLeft, expired }

/// Remaining time of a pending booking, measured on the server clock.
///
/// The deadline is the server `expiresAt`. The device clock may differ from
/// the server one, so [clockSkew] (server minus device) is estimated from the
/// `createdAt` of the booking when its response arrives. Whatever is shown,
/// the server decides whether the booking can still be confirmed.
@immutable
class HoldCountdown {
  const HoldCountdown({
    required this.expiresAt,
    required this.holdLength,
    this.clockSkew = Duration.zero,
  });

  /// Countdown of [expiresAt] for a booking whose response arrived at
  /// [receivedAt] on the device clock.
  factory HoldCountdown.fromServer({
    required DateTime createdAt,
    required DateTime expiresAt,
    required DateTime receivedAt,
  }) => HoldCountdown(
    expiresAt: expiresAt,
    holdLength: expiresAt.difference(createdAt),
    clockSkew: createdAt.difference(receivedAt),
  );

  static const Duration warningThreshold = Duration(minutes: 1);
  static const Duration finalThreshold = Duration(seconds: 30);

  final DateTime expiresAt;

  /// Whole hold, used to show the progress.
  final Duration holdLength;
  final Duration clockSkew;

  /// Never negative, rounded up to whole seconds so `0:00` only shows once
  /// the time is really over.
  Duration remainingAt(DateTime deviceNow) {
    final remaining = expiresAt.difference(deviceNow.add(clockSkew));
    if (remaining <= Duration.zero) return Duration.zero;
    final seconds = (remaining.inMilliseconds / Duration.millisecondsPerSecond)
        .ceil();
    return Duration(seconds: seconds);
  }

  /// Fraction of the hold still left, from 1 to 0.
  double fractionLeft(Duration remaining) {
    if (holdLength <= Duration.zero) return 0;
    return (remaining.inMilliseconds / holdLength.inMilliseconds).clamp(0, 1);
  }

  static bool isWarning(Duration remaining) => remaining <= warningThreshold;

  /// The last milestone passed when the remaining time went from [before] to
  /// [after]. After a jump (the app was in background) only the latest one is
  /// returned, so a stale announcement is never read.
  static HoldMilestone? milestoneBetween(Duration before, Duration after) {
    bool crossed(Duration threshold) =>
        before > threshold && after <= threshold;
    if (crossed(Duration.zero)) return HoldMilestone.expired;
    if (crossed(finalThreshold)) return HoldMilestone.thirtySecondsLeft;
    if (crossed(warningThreshold)) return HoldMilestone.oneMinuteLeft;
    return null;
  }
}

/// `4:32`.
String formatRemaining(Duration remaining) {
  final minutes = remaining.inMinutes;
  final seconds = remaining.inSeconds % Duration.secondsPerMinute;
  return '$minutes:${seconds.toString().padLeft(2, '0')}';
}
