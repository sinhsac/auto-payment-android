<?php

namespace App\Middleware;

class Auth
{
    public static function check(array $config): void
    {
        $apiSecret  = $_SERVER['HTTP_X_API_SECRET'] ?? '';
        $authSecret = $_SERVER['HTTP_X_AUTH_SECRET'] ?? '';
        $authToken  = $_SERVER['HTTP_X_AUTH_TOKEN'] ?? '';
        $basicAuth  = $_SERVER['HTTP_AUTHORIZATION'] ?? '';

        $notiSecret = getenv('NOTI_SECRET') ?: ($config['noti_secret'] ?? '');
        $notiToken  = getenv('NOTI_TOKEN')  ?: ($config['noti_token']  ?? '');

        error_log('AUTH DEBUG - received authSecret: [' . $authSecret . '] expected: [' . $notiSecret . ']');

        $authorized =
            ($apiSecret  !== '' && $apiSecret  === $config['secret']) ||
            ($authSecret !== '' && $authSecret === $notiSecret)        ||
            ($authToken  !== '' && $authToken  === $notiToken)         ||
            self::checkBasic($basicAuth, $config);

        if (!$authorized) {
            http_response_code(401);
            echo json_encode(['success' => false, 'message' => 'Unauthorized']);
            exit;
        }
    }

    private static function checkBasic(string $header, array $config): bool
    {
        if (!str_starts_with($header, 'Basic ')) return false;
        $decoded = base64_decode(substr($header, 6));
        [$user, $pass] = explode(':', $decoded, 2) + ['', ''];
        $basicUser = getenv('BASIC_USER') ?: ($config['basic_user'] ?? '');
        $basicPass = getenv('BASIC_PASS') ?: ($config['basic_pass'] ?? '');
        return $user === $basicUser && $pass === $basicPass && $basicUser !== '';
    }
}
