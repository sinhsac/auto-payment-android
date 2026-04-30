<?php

namespace App\Handler;

use App\DB\MongoDB;
use MongoDB\BSON\UTCDateTime;

class Sessions
{
    public static function start(): void
    {
        $body = json_decode(file_get_contents('php://input'), true);

        if (empty($body['sessionId']) || empty($body['workerId'])) {
            http_response_code(400);
            echo json_encode(['success' => false, 'message' => 'Missing required fields']);
            return;
        }

        $doc = [
            'sessionId' => $body['sessionId'],
            'workerId'  => $body['workerId'],
            'startedAt' => $body['startedAt'],
            'endedAt'   => null,
            'isActive'  => true,
            'createdAt' => new UTCDateTime(),
        ];

        MongoDB::get()->packing_sessions->updateOne(
            ['sessionId' => $body['sessionId']],
            ['$set' => $doc],
            ['upsert' => true]
        );

        http_response_code(201);
        echo json_encode(['success' => true, 'message' => 'Session started']);
    }

    public static function end(): void
    {
        $body = json_decode(file_get_contents('php://input'), true);

        if (empty($body['sessionId'])) {
            http_response_code(400);
            echo json_encode(['success' => false, 'message' => 'Missing sessionId']);
            return;
        }

        MongoDB::get()->packing_sessions->updateOne(
            ['sessionId' => $body['sessionId']],
            ['$set' => [
                'endedAt'  => $body['endedAt'],
                'isActive' => false,
            ]]
        );

        echo json_encode(['success' => true, 'message' => 'Session ended']);
    }
}
