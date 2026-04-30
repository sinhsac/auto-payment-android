<?php

return [
    'secret'      => getenv('API_SECRET')   ?: 'CHANGE_ME_SECRET_KEY',
    'noti_secret' => getenv('NOTI_SECRET')  ?: 'nr_9x2Kp7mQvLzT4wYjR8sNdHbEuFcAi',
    'noti_token'  => getenv('NOTI_TOKEN')   ?: '',
    'basic_user'  => getenv('BASIC_USER')   ?: '',
    'basic_pass'  => getenv('BASIC_PASS')   ?: '',
    'base_url'    => getenv('BASE_URL')     ?: 'https://4fc0e50dd3.hotrogiaiphapso.info',

    'mongodb' => [
        'uri'      => getenv('MONGO_URI') ?: 'mongodb://localhost:27017',
        'database' => getenv('MONGO_DB')  ?: 'packing_station',
    ],
];
