<?php
declare(strict_types=1);
require_once dirname(__DIR__, 2) . '/vendor/autoload.php';
session_start();

const ADMIN_PASSWORD = 'sacdeptrai';

if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['password'])) {
    if ($_POST['password'] === ADMIN_PASSWORD) {
        $_SESSION['admin_auth'] = true;
        header('Location: packing.php');
        exit;
    }
    $error = 'Mật khẩu không đúng';
}

if ($_SESSION['admin_auth'] ?? false) {
    header('Location: packing.php');
    exit;
}
?>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Admin Login</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; background: #0f172a; min-height: 100vh; display: flex; align-items: center; justify-content: center; }
        .card { background: #1e293b; border: 1px solid #334155; border-radius: 16px; padding: 40px; width: 380px; }
        .logo { font-size: 32px; margin-bottom: 8px; }
        h1 { color: #f1f5f9; font-size: 22px; margin-bottom: 4px; }
        p { color: #94a3b8; font-size: 14px; margin-bottom: 28px; }
        label { display: block; color: #94a3b8; font-size: 13px; margin-bottom: 6px; }
        input { width: 100%; padding: 11px 14px; background: #0f172a; border: 1px solid #334155; border-radius: 8px; color: #f1f5f9; font-size: 15px; outline: none; }
        input:focus { border-color: #3b82f6; }
        button { width: 100%; margin-top: 16px; padding: 12px; background: #3b82f6; color: #fff; border: none; border-radius: 8px; font-size: 15px; font-weight: 600; cursor: pointer; }
        button:hover { background: #2563eb; }
        .error { color: #f87171; font-size: 13px; margin-top: 10px; }
    </style>
</head>
<body>
<div class="card">
    <div class="logo">⚙️</div>
    <h1>Admin Panel</h1>
    <p>Đăng nhập để tiếp tục</p>
    <form method="POST">
        <label>Mật khẩu</label>
        <input type="password" name="password" autofocus placeholder="••••••••">
        <button type="submit">Đăng nhập</button>
        <?php if (isset($error)): ?>
            <p class="error">⚠ <?= htmlspecialchars($error) ?></p>
        <?php endif; ?>
    </form>
</div>
</body>
</html>
