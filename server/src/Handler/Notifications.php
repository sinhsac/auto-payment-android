<?php

namespace App\Handler;

use App\DB\MongoDB;
use MongoDB\BSON\UTCDateTime;

class Notifications
{
    public static function create(): void
    {
        $body = json_decode(file_get_contents('php://input'), true);
        if (empty($body)) {
            http_response_code(400);
            echo json_encode(['success' => false, 'message' => 'Invalid body']);
            return;
        }

        $db = MongoDB::get();
        $db->selectCollection('notifications')->insertOne([
            'pck'         => $body['pck'] ?? null,
            'title'       => $body['title'] ?? null,
            'msg'         => $body['msg'] ?? null,
            'groupKey'    => $body['groupKey'] ?? null,
            'postTime'    => $body['postTime'] ?? null,
            'receivedAt'  => new UTCDateTime(),
        ]);

        echo json_encode(['success' => true]);
    }

    public static function list(): void
    {
        $db = MongoDB::get();
        $limit = min((int)($_GET['limit'] ?? 50), 200);
        $page  = max((int)($_GET['page'] ?? 1), 1);
        $skip  = ($page - 1) * $limit;
        $pck   = $_GET['pck'] ?? null;

        $filter = $pck ? ['pck' => $pck] : [];

        $cursor = $db->selectCollection('notifications')->find(
            $filter,
            [
                'sort'  => ['receivedAt' => -1],
                'limit' => $limit,
                'skip'  => $skip,
            ]
        );

        $items = [];
        foreach ($cursor as $doc) {
            $items[] = [
                'id'         => (string)$doc['_id'],
                'pck'        => $doc['pck'] ?? null,
                'title'      => $doc['title'] ?? null,
                'msg'        => $doc['msg'] ?? null,
                'groupKey'   => $doc['groupKey'] ?? null,
                'postTime'   => $doc['postTime'] ?? null,
                'receivedAt' => isset($doc['receivedAt']) ? $doc['receivedAt']->toDateTime()->format('Y-m-d H:i:s') : null,
            ];
        }

        echo json_encode(['success' => true, 'data' => $items, 'page' => $page, 'limit' => $limit]);
    }
}
