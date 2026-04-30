<?php

namespace App\Handler;

use App\DB\MongoDB;
use MongoDB\BSON\UTCDateTime;

class PackingRecords
{
    public static function create(): void
    {
        $body = json_decode(file_get_contents('php://input'), true);

        if (empty($body['barcodeValue']) || empty($body['workerId']) || empty($body['sessionId'])) {
            http_response_code(400);
            echo json_encode(['success' => false, 'message' => 'Missing required fields']);
            return;
        }

        $doc = [
            'recordId'      => $body['recordId'] ?? null,
            'barcodeValue'  => $body['barcodeValue'],
            'barcodeFormat' => $body['barcodeFormat'] ?? 'UNKNOWN',
            'scannedAt'     => $body['scannedAt'],
            'workerId'      => $body['workerId'],
            'sessionId'     => $body['sessionId'],
            'createdAt'     => new UTCDateTime(),
        ];

        MongoDB::get()->packing_records->insertOne($doc);

        http_response_code(201);
        echo json_encode(['success' => true, 'message' => 'Record saved']);
    }
}
