<?php
declare(strict_types=1);
if (session_status() === PHP_SESSION_NONE) session_start();

if (!($_SESSION['admin_auth'] ?? false)) {
    header('Location: index.php');
    exit;
}

if (isset($_POST['logout'])) {
    session_destroy();
    header('Location: index.php');
    exit;
}

$currentPage = basename($_SERVER['PHP_SELF'], '.php');

$nav = [
    'packing'       => ['icon' => '📦', 'label' => 'Packing Records'],
    'notifications' => ['icon' => '🔔', 'label' => 'Notifications'],
];
?>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><?= $nav[$currentPage]['label'] ?? 'Admin' ?> — Admin Panel</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; background: #f1f5f9; min-height: 100vh; display: flex; }

        /* Sidebar */
        .sidebar { width: 220px; background: #0f172a; min-height: 100vh; display: flex; flex-direction: column; flex-shrink: 0; }
        .sidebar-logo { padding: 24px 20px 16px; border-bottom: 1px solid #1e293b; }
        .sidebar-logo .icon { font-size: 24px; }
        .sidebar-logo h2 { color: #f1f5f9; font-size: 16px; margin-top: 6px; }
        .sidebar-logo p { color: #64748b; font-size: 12px; }
        .sidebar-nav { padding: 12px 0; flex: 1; }
        .sidebar-nav a { display: flex; align-items: center; gap: 10px; padding: 10px 20px; color: #94a3b8; text-decoration: none; font-size: 14px; transition: all .15s; }
        .sidebar-nav a:hover { background: #1e293b; color: #f1f5f9; }
        .sidebar-nav a.active { background: #1e3a5f; color: #60a5fa; border-right: 3px solid #3b82f6; }
        .sidebar-nav a .icon { font-size: 16px; width: 20px; text-align: center; }
        .sidebar-footer { padding: 16px 20px; border-top: 1px solid #1e293b; }
        .sidebar-footer form { margin: 0; }
        .sidebar-footer button { width: 100%; padding: 8px; background: #1e293b; color: #94a3b8; border: none; border-radius: 6px; font-size: 13px; cursor: pointer; }
        .sidebar-footer button:hover { background: #334155; color: #f1f5f9; }

        /* Main */
        .main { flex: 1; display: flex; flex-direction: column; min-width: 0; }
        .topbar { background: #fff; border-bottom: 1px solid #e2e8f0; padding: 14px 24px; display: flex; align-items: center; justify-content: space-between; }
        .topbar h1 { font-size: 18px; color: #0f172a; }
        .topbar .meta { font-size: 13px; color: #94a3b8; }
        .content { padding: 24px; flex: 1; }

        /* Cards */
        .card { background: #fff; border-radius: 10px; box-shadow: 0 1px 3px rgba(0,0,0,.08); margin-bottom: 20px; }
        .card-header { padding: 16px 20px; border-bottom: 1px solid #f1f5f9; display: flex; align-items: center; justify-content: space-between; }
        .card-header h2 { font-size: 14px; font-weight: 600; color: #374151; }
        .card-body { padding: 20px; }

        /* Stats */
        .stats { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 16px; margin-bottom: 20px; }
        .stat { background: #fff; border-radius: 10px; padding: 16px 20px; box-shadow: 0 1px 3px rgba(0,0,0,.08); }
        .stat .val { font-size: 28px; font-weight: 700; color: #3b82f6; }
        .stat .lbl { font-size: 12px; color: #94a3b8; margin-top: 2px; }

        /* Filter */
        .filter-row { display: flex; gap: 10px; flex-wrap: wrap; align-items: flex-end; }
        .filter-group { display: flex; flex-direction: column; gap: 4px; }
        .filter-group label { font-size: 12px; color: #64748b; font-weight: 500; }
        .filter-group input, .filter-group select { padding: 8px 12px; border: 1px solid #e2e8f0; border-radius: 7px; font-size: 14px; outline: none; background: #fff; color: #0f172a; }
        .filter-group input:focus, .filter-group select:focus { border-color: #3b82f6; }

        /* Buttons */
        .btn { padding: 8px 16px; border: none; border-radius: 7px; font-size: 13px; font-weight: 500; cursor: pointer; text-decoration: none; display: inline-flex; align-items: center; gap: 5px; }
        .btn-primary { background: #3b82f6; color: #fff; }
        .btn-primary:hover { background: #2563eb; }
        .btn-success { background: #22c55e; color: #fff; }
        .btn-success:hover { background: #16a34a; }
        .btn-ghost { background: #f1f5f9; color: #374151; border: 1px solid #e2e8f0; }
        .btn-ghost:hover { background: #e2e8f0; }

        /* Table */
        .table-wrap { overflow-x: auto; }
        table { width: 100%; border-collapse: collapse; font-size: 13px; }
        thead th { background: #f8fafc; color: #374151; padding: 10px 14px; text-align: left; font-weight: 600; border-bottom: 2px solid #e2e8f0; white-space: nowrap; }
        tbody tr:hover { background: #f8fafc; }
        tbody td { padding: 9px 14px; border-bottom: 1px solid #f1f5f9; color: #374151; }
        .badge { display: inline-block; padding: 2px 8px; border-radius: 20px; font-size: 11px; font-weight: 500; }
        .badge-blue { background: #dbeafe; color: #1d4ed8; }
        .badge-green { background: #dcfce7; color: #15803d; }
        .badge-gray { background: #f1f5f9; color: #475569; }
        .empty { text-align: center; padding: 48px; color: #94a3b8; font-size: 14px; }

        /* Pagination */
        .pagination { display: flex; align-items: center; justify-content: space-between; padding: 12px 20px; border-top: 1px solid #f1f5f9; flex-wrap: wrap; gap: 8px; }
        .pagination .info { font-size: 13px; color: #64748b; }
        .pages { display: flex; gap: 4px; }
        .pages a, .pages span { display: inline-block; padding: 5px 10px; border-radius: 6px; font-size: 13px; text-decoration: none; border: 1px solid #e2e8f0; color: #374151; }
        .pages a:hover { background: #dbeafe; border-color: #3b82f6; color: #1d4ed8; }
        .pages span.active { background: #3b82f6; color: #fff; border-color: #3b82f6; }
        .pages span.dots { border: none; color: #94a3b8; }
    </style>
</head>
<body>

<div class="sidebar">
    <div class="sidebar-logo">
        <div class="icon">⚙️</div>
        <h2>Admin Panel</h2>
        <p>Management System</p>
    </div>
    <nav class="sidebar-nav">
        <?php foreach ($nav as $page => $item): ?>
        <a href="<?= $page ?>.php" class="<?= $currentPage === $page ? 'active' : '' ?>">
            <span class="icon"><?= $item['icon'] ?></span>
            <?= $item['label'] ?>
        </a>
        <?php endforeach; ?>
    </nav>
    <div class="sidebar-footer">
        <form method="POST">
            <button type="submit" name="logout">🚪 Đăng xuất</button>
        </form>
    </div>
</div>

<div class="main">
    <div class="topbar">
        <h1><?= $nav[$currentPage]['icon'] ?? '' ?> <?= $nav[$currentPage]['label'] ?? '' ?></h1>
        <span class="meta"><?= date('d/m/Y H:i') ?></span>
    </div>
    <div class="content">
