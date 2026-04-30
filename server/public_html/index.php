<?php

declare(strict_types=1);

require_once dirname(__DIR__) . '/vendor/autoload.php';

use App\Middleware\Auth;
use App\Handler\PackingRecords;
use App\Handler\Sessions;
use App\Handler\ImageUpload;
use App\Handler\Notifications;

// JSON response mặc định
header('Content-Type: application/json');

$config = require dirname(__DIR__) . '/config.php';

// Auth check cho mọi request
Auth::check($config);

// Router đơn giản
$method = $_SERVER['REQUEST_METHOD'];
$path   = parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH);
// Chuẩn hóa: bỏ trailing slash
$path   = rtrim($path, '/');

match (true) {
    $method === 'POST' && $path === '/api/packing-records'       => PackingRecords::create(),
    $method === 'POST' && $path === '/api/packing-records/image' => ImageUpload::handle(),
    $method === 'POST' && $path === '/api/sessions/start'        => Sessions::start(),
    $method === 'POST' && $path === '/api/sessions/end'          => Sessions::end(),
    $method === 'POST' && $path === '/api/notifications'         => Notifications::create(),
    $method === 'GET'  && $path === '/api/notifications'         => Notifications::list(),
    default => (function () {
        http_response_code(404);
        echo json_encode(['success' => false, 'message' => 'Not found']);
    })(),
};
