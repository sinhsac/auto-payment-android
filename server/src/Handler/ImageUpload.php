<?php

namespace App\Handler;

use App\DB\MongoDB;

class ImageUpload
{
    public static function handle(): void
    {
        if (empty($_POST['recordId'])) {
            http_response_code(400);
            echo json_encode(['success' => false, 'message' => 'Missing recordId']);
            return;
        }

        if (!isset($_FILES['image']) || $_FILES['image']['error'] !== UPLOAD_ERR_OK) {
            http_response_code(400);
            echo json_encode(['success' => false, 'message' => 'No image uploaded']);
            return;
        }

        $recordId = preg_replace('/[^a-zA-Z0-9\-]/', '', $_POST['recordId']);
        if (empty($recordId)) {
            http_response_code(400);
            echo json_encode(['success' => false, 'message' => 'Invalid recordId']);
            return;
        }

        // Kiểm tra mime type thực sự
        $finfo = finfo_open(FILEINFO_MIME_TYPE);
        $mime  = finfo_file($finfo, $_FILES['image']['tmp_name']);
        finfo_close($finfo);

        if ($mime !== 'image/jpeg') {
            http_response_code(400);
            echo json_encode(['success' => false, 'message' => 'Only JPEG allowed']);
            return;
        }

        $dir = __DIR__ . '/../../public_html/images/';
        if (!is_dir($dir)) {
            mkdir($dir, 0755, true);
        }

        $filename = $recordId . '.jpg';
        $dest     = $dir . $filename;

        if (!move_uploaded_file($_FILES['image']['tmp_name'], $dest)) {
            http_response_code(500);
            echo json_encode(['success' => false, 'message' => 'Failed to save image']);
            return;
        }

        $config  = require dirname(__DIR__, 2) . '/config.php';
        $baseUrl = $config['base_url'] ?? '';
        $url     = $baseUrl . '/images/' . $filename;

        // Cập nhật imageUrl vào MongoDB record
        MongoDB::get()->packing_records->updateOne(
            ['recordId' => $recordId],
            ['$set' => ['imageUrl' => $url]]
        );

        echo json_encode(['success' => true, 'url' => $url]);
    }
}
