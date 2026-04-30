<?php

namespace App\DB;

use MongoDB\Client;
use MongoDB\Database;

class MongoDB
{
    private static ?Database $db = null;

    public static function get(): Database
    {
        if (self::$db === null) {
            $config   = require dirname(__DIR__, 2) . '/config.php';
            $client   = new Client($config['mongodb']['uri']);
            self::$db = $client->selectDatabase($config['mongodb']['database']);
        }
        return self::$db;
    }
}
