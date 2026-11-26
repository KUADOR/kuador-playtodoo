import 'dart:convert';
import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:playtodoo/features/tenant/data/http_tenant_api.dart';
import 'package:playtodoo/features/tenant/domain/tenant_api.dart';

/// A real HTTP server on the loopback interface that answers every request
/// with one status and body, and remembers the last request.
class _FakeTenantHost {
  _FakeTenantHost._(this._server);

  final HttpServer _server;
  int status = HttpStatus.ok;
  String body = '';
  String contentType = 'application/json';
  Duration delay = Duration.zero;
  HttpRequest? lastRequest;

  static Future<_FakeTenantHost> start() async {
    final host = _FakeTenantHost._(
      await HttpServer.bind(InternetAddress.loopbackIPv4, 0),
    );
    host._server.listen(host._answer);
    return host;
  }

  Uri get apiBaseUrl => Uri.parse('http://127.0.0.1:${_server.port}/api/v1');

  Future<void> _answer(HttpRequest request) async {
    lastRequest = request;
    await Future<void>.delayed(delay);
    request.response
      ..statusCode = status
      ..headers.contentType = ContentType.parse(contentType)
      ..write(body);
    await request.response.close();
  }

  void answerProblem(int problemStatus, String code) {
    status = problemStatus;
    contentType = 'application/problem+json';
    body = jsonEncode({
      'type': 'about:blank',
      'title': code,
      'status': problemStatus,
      'code': code,
    });
  }

  Future<void> close() => _server.close(force: true);
}

void main() {
  late _FakeTenantHost host;
  late HttpClient client;

  setUp(() async {
    host = await _FakeTenantHost.start();
    client = HttpClient();
  });

  tearDown(() async {
    client.close(force: true);
    await host.close();
  });

  HttpTenantApi apiOf({Duration timeout = HttpTenantApi.defaultTimeout}) =>
      HttpTenantApi(
        apiBaseUrl: host.apiBaseUrl,
        httpClient: client,
        timeout: timeout,
      );

  test(
    'calls GET /tenant on the host of the complex without credentials',
    () async {
      host.body = jsonEncode({
        'status': 'ACTIVE',
        'displayName': 'Canchas El Sol',
      });

      await apiOf().getTenantAccess();

      expect(host.lastRequest?.method, 'GET');
      expect(host.lastRequest?.uri.path, '/api/v1/tenant');
      expect(
        host.lastRequest?.headers.value(HttpHeaders.authorizationHeader),
        isNull,
      );
    },
  );

  test('maps 200 to an active complex with its name', () async {
    host.body = jsonEncode({
      'status': 'ACTIVE',
      'displayName': 'Canchas El Sol',
    });

    final access = await apiOf().getTenantAccess();

    expect(access, isA<ActiveTenant>());
    expect((access as ActiveTenant).displayName, 'Canchas El Sol');
  });

  test('maps 403 TENANT_UNAVAILABLE to an unavailable complex', () async {
    host.answerProblem(HttpStatus.forbidden, 'TENANT_UNAVAILABLE');

    expect(await apiOf().getTenantAccess(), isA<UnavailableTenant>());
  });

  test('maps 404 TENANT_NOT_FOUND to a complex not found', () async {
    host.answerProblem(HttpStatus.notFound, 'TENANT_NOT_FOUND');

    expect(await apiOf().getTenantAccess(), isA<TenantNotFound>());
  });

  final answersOutsideTheContract = <String, void Function(_FakeTenantHost)>{
    'a 404 without a problem body (for example, a proxy page)': (host) {
      host
        ..status = HttpStatus.notFound
        ..contentType = 'text/html'
        ..body = '<html>Not Found</html>';
    },
    'a 500 problem': (host) =>
        host.answerProblem(HttpStatus.internalServerError, 'INTERNAL_ERROR'),
    'a 200 without the complex name': (host) =>
        host.body = jsonEncode({'status': 'ACTIVE'}),
    'a 200 that is not JSON': (host) => host.body = 'ok',
  };
  answersOutsideTheContract.forEach((description, answer) {
    test(
      'reports $description as a failed check instead of guessing',
      () async {
        answer(host);

        await expectLater(
          apiOf().getTenantAccess(),
          throwsA(isA<TenantCheckFailure>()),
        );
      },
    );
  });

  test(
    'reports a host that does not answer in time as a failed check',
    () async {
      host
        ..body = jsonEncode({
          'status': 'ACTIVE',
          'displayName': 'Canchas El Sol',
        })
        ..delay = const Duration(seconds: 2);

      await expectLater(
        apiOf(timeout: const Duration(milliseconds: 100)).getTenantAccess(),
        throwsA(isA<TenantCheckFailure>()),
      );
    },
  );

  test('reports a refused connection as a failed check', () async {
    final unreachable = host.apiBaseUrl;
    await host.close();

    await expectLater(
      HttpTenantApi(
        apiBaseUrl: unreachable,
        httpClient: client,
      ).getTenantAccess(),
      throwsA(isA<TenantCheckFailure>()),
    );
  });
}
