# Packing Station API Server

## Cấu trúc
```
server/
├── public_html/        ← DocumentRoot của Apache
│   ├── index.php       ← Router chính
│   └── .htaccess       ← Rewrite rules
├── src/
│   ├── DB/MongoDB.php          ← MongoDB singleton
│   ├── Handler/PackingRecords.php
│   ├── Handler/Sessions.php
│   └── Middleware/Auth.php
├── config.php          ← Cấu hình (đọc từ env)
├── composer.json
└── .env.example
```

## Deploy lên server

### 1. Upload code
```bash
# Upload toàn bộ thư mục server/ lên server
# DocumentRoot trỏ vào: .../server/public_html
```

### 2. Cài MongoDB PHP extension
```bash
pecl install mongodb
echo "extension=mongodb.so" >> /etc/php/8.3/apache2/php.ini
systemctl restart apache2
```

### 3. Cài dependencies
```bash
cd /path/to/server
composer install --no-dev
```

### 4. Cấu hình biến môi trường
Thêm vào Apache VirtualHost hoặc file `.env` load qua `SetEnv`:
```apache
SetEnv API_SECRET  uaTEAW5xG8rR3Ok/fmOy71dOuGwq8gc+xLIADtJ52Ww=
SetEnv MONGO_URI   mongodb+srv://vnm076v5:<password>@cluster0.lhp2rtd.mongodb.net/?appName=Cluster0
SetEnv MONGO_DB    packing_station
SetEnv BASE_URL    https://4fc0e50dd3.hotrogiaiphapso.info
```

Hoặc tạo file `/path/to/server/.env` và load trong `config.php`.

### 5. Phân quyền
```bash
chown -R www-data:www-data /path/to/server
chmod -R 755 /path/to/server
```

## API Endpoints

Tất cả request cần header:
```
X-API-Secret: your_secret_key_here
Content-Type: application/json
```

### POST /api/packing-records
```json
{
    "barcodeValue": "1234567890",
    "barcodeFormat": "EAN_13",
    "scannedAt": "2024-01-15T10:30:00Z",
    "workerId": "W001",
    "sessionId": "uuid-here"
}
```

### POST /api/sessions/start
```json
{
    "sessionId": "uuid-here",
    "workerId": "W001",
    "startedAt": "2024-01-15T08:00:00Z"
}
```

### POST /api/sessions/end
```json
{
    "sessionId": "uuid-here",
    "endedAt": "2024-01-15T17:00:00Z"
}
```

## Cập nhật Android app
Trong `NetworkModule.kt`, đổi `BASE_URL`:
```kotlin
private const val BASE_URL = "https://4fc0e50dd3.hotrogiaiphapso.info"
```

Thêm secret vào header trong `OkHttpClient`:
```kotlin
.addInterceptor { chain ->
    chain.proceed(
        chain.request().newBuilder()
            .header("X-API-Secret", BuildConfig.API_SECRET)
            .build()
    )
}
```
