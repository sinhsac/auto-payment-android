<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/vendor/autoload.php';

use App\DB\MongoDB as DB;
use MongoDB\BSON\UTCDateTime;
use PhpOffice\PhpSpreadsheet\Spreadsheet;
use PhpOffice\PhpSpreadsheet\Writer\Xlsx;
use PhpOffice\PhpSpreadsheet\Style\Alignment;
use PhpOffice\PhpSpreadsheet\Style\Fill;

session_start();

const REPORT_PASSWORD = 'sacdeptrai';
const PAGE_SIZE       = 50;
const EXPORT_BATCH    = 1000;

// ── Auth ──────────────────────────────────────────────────────────────
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['password'])) {
    if ($_POST['password'] === REPORT_PASSWORD) {
        $_SESSION['report_auth'] = true;
    } else {
        $loginError = 'Mật khẩu không đúng';
    }
}
if (isset($_POST['logout'])) {
    session_destroy();
    header('Location: ' . $_SERVER['PHP_SELF']);
    exit;
}

$authed = $_SESSION['report_auth'] ?? false;

// ── Helpers ───────────────────────────────────────────────────────────
function buildFilter(string $worker, string $from, string $to): array
{
    $filter = [];
    if ($worker !== '') {
        $filter['workerId'] = $worker;
    }
    if ($from !== '' || $to !== '') {
        $filter['scannedAt'] = [];
        if ($from !== '') $filter['scannedAt']['$gte'] = $from . 'T00:00:00Z';
        if ($to   !== '') $filter['scannedAt']['$lte'] = $to   . 'T23:59:59Z';
    }
    return $filter;
}

function formatDateTime(mixed $value): string
{
    if ($value instanceof UTCDateTime) {
        return $value->toDateTime()
            ->setTimezone(new DateTimeZone('Asia/Ho_Chi_Minh'))
            ->format('d/m/Y H:i:s');
    }
    if (is_string($value) && $value !== '') {
        try {
            return (new DateTime($value))
                ->setTimezone(new DateTimeZone('Asia/Ho_Chi_Minh'))
                ->format('d/m/Y H:i:s');
        } catch (Exception) {}
    }
    return '';
}

function docToRow(mixed $doc): array
{
    return [
        'recordId'      => (string)($doc['_id']          ?? ''),
        'scannedAt'     => formatDateTime($doc['scannedAt'] ?? ''),
        'workerId'      => (string)($doc['workerId']      ?? ''),
        'barcodeValue'  => (string)($doc['barcodeValue']  ?? ''),
        'barcodeFormat' => (string)($doc['barcodeFormat'] ?? ''),
        'sessionId'     => (string)($doc['sessionId']     ?? ''),
        'imageUrl'      => (string)($doc['imageUrl']      ?? ''),
    ];
}

// ── Main logic ────────────────────────────────────────────────────────
$records     = [];
$totalCount  = 0;
$totalPages  = 1;
$currentPage = 1;

if ($authed) {
    $filterWorker = trim($_GET['worker'] ?? '');
    $filterFrom   = $_GET['from']   ?? '';
    $filterTo     = $_GET['to']     ?? '';
    $currentPage  = max(1, (int)($_GET['page'] ?? 1));
    $doExport     = isset($_GET['export']);

    $filter = buildFilter($filterWorker, $filterFrom, $filterTo);

    // ── Export: batch fetch ───────────────────────────────────────────
    if ($doExport) {
        $totalCount = DB::get()->packing_records->countDocuments($filter);

        $spreadsheet = new Spreadsheet();
        $sheet       = $spreadsheet->getActiveSheet();
        $sheet->setTitle('Báo cáo');

        $headers = ['STT', 'Ngày giờ quét', 'Worker ID', 'Mã vạch', 'Định dạng', 'Session ID'];
        foreach ($headers as $col => $title) {
            $sheet->setCellValue(chr(65 + $col) . '1', $title);
        }
        $sheet->getStyle('A1:F1')->applyFromArray([
            'font'      => ['bold' => true, 'color' => ['rgb' => 'FFFFFF']],
            'fill'      => ['fillType' => Fill::FILL_SOLID, 'startColor' => ['rgb' => '1976D2']],
            'alignment' => ['horizontal' => Alignment::HORIZONTAL_CENTER],
        ]);

        $rowIndex   = 2;
        $globalIdx  = 1;
        $skip       = 0;

        while ($skip < $totalCount) {
            $cursor = DB::get()->packing_records->find(
                $filter,
                ['sort' => ['scannedAt' => 1], 'skip' => $skip, 'limit' => EXPORT_BATCH]
            );

            foreach ($cursor as $doc) {
                $row = docToRow($doc);
                $r   = $rowIndex++;
                $sheet->setCellValue("A$r", $globalIdx++);
                $sheet->setCellValue("B$r", $row['scannedAt']);
                $sheet->setCellValue("C$r", $row['workerId']);
                $sheet->setCellValue("D$r", $row['barcodeValue']);
                $sheet->setCellValue("E$r", $row['barcodeFormat']);
                $sheet->setCellValue("F$r", $row['sessionId']);

                if ($globalIdx % 2 === 0) {
                    $sheet->getStyle("A$r:F$r")->applyFromArray([
                        'fill' => ['fillType' => Fill::FILL_SOLID, 'startColor' => ['rgb' => 'F5F5F5']],
                    ]);
                }
            }
            $skip += EXPORT_BATCH;
        }

        foreach (range('A', 'F') as $col) {
            $sheet->getColumnDimension($col)->setAutoSize(true);
        }

        $filename = 'bao-cao-' . date('Ymd-His') . '.xlsx';
        header('Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
        header("Content-Disposition: attachment; filename=\"$filename\"");
        header('Cache-Control: max-age=0');
        (new Xlsx($spreadsheet))->save('php://output');
        exit;
    }

    // ── Preview: paginated ────────────────────────────────────────────
    $totalCount = DB::get()->packing_records->countDocuments($filter);
    $totalPages = max(1, (int)ceil($totalCount / PAGE_SIZE));
    $currentPage = min($currentPage, $totalPages);
    $skip = ($currentPage - 1) * PAGE_SIZE;

    $cursor = DB::get()->packing_records->find(
        $filter,
        ['sort' => ['scannedAt' => -1], 'skip' => $skip, 'limit' => PAGE_SIZE]
    );
    foreach ($cursor as $doc) {
        $records[] = docToRow($doc);
    }
}

function pageUrl(int $page, string $worker, string $from, string $to): string
{
    return '?' . http_build_query(array_filter([
        'worker' => $worker,
        'from'   => $from,
        'to'     => $to,
        'page'   => $page,
    ]));
}
?>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Báo cáo - Packing Station</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; background: #f0f2f5; min-height: 100vh; }

        .login-wrap { display: flex; align-items: center; justify-content: center; min-height: 100vh; }
        .login-box { background: #fff; padding: 40px; border-radius: 12px; box-shadow: 0 4px 24px rgba(0,0,0,.1); width: 360px; }
        .login-box h1 { font-size: 22px; margin-bottom: 8px; }
        .login-box p { color: #666; font-size: 14px; margin-bottom: 28px; }
        .login-box input { width: 100%; padding: 12px 14px; border: 1px solid #ddd; border-radius: 8px; font-size: 15px; outline: none; }
        .login-box input:focus { border-color: #1976D2; }
        .login-box button { width: 100%; margin-top: 14px; padding: 12px; background: #1976D2; color: #fff; border: none; border-radius: 8px; font-size: 15px; cursor: pointer; }
        .login-box button:hover { background: #1565C0; }
        .error { color: #d32f2f; font-size: 13px; margin-top: 10px; }

        .header { background: #1976D2; color: #fff; padding: 16px 24px; display: flex; justify-content: space-between; align-items: center; }
        .header h1 { font-size: 18px; }
        .header button { background: rgba(255,255,255,.2); border: none; color: #fff; padding: 8px 16px; border-radius: 6px; cursor: pointer; font-size: 13px; }
        .header button:hover { background: rgba(255,255,255,.3); }

        .container { max-width: 1200px; margin: 24px auto; padding: 0 16px; }

        .card { background: #fff; border-radius: 10px; padding: 20px 24px; box-shadow: 0 1px 4px rgba(0,0,0,.08); margin-bottom: 20px; }
        .card h2 { font-size: 14px; color: #555; margin-bottom: 16px; font-weight: 600; text-transform: uppercase; letter-spacing: .5px; }

        .filter-row { display: flex; gap: 12px; flex-wrap: wrap; align-items: flex-end; }
        .filter-group { display: flex; flex-direction: column; gap: 5px; }
        .filter-group label { font-size: 12px; color: #666; font-weight: 500; }
        .filter-group input { padding: 9px 12px; border: 1px solid #ddd; border-radius: 7px; font-size: 14px; outline: none; min-width: 150px; }
        .filter-group input:focus { border-color: #1976D2; }

        .btn { padding: 9px 18px; border: none; border-radius: 7px; font-size: 14px; cursor: pointer; text-decoration: none; display: inline-block; }
        .btn-primary  { background: #1976D2; color: #fff; }
        .btn-primary:hover  { background: #1565C0; }
        .btn-success  { background: #388E3C; color: #fff; }
        .btn-success:hover  { background: #2E7D32; }
        .btn-secondary { background: #f5f5f5; color: #333; border: 1px solid #ddd; }
        .btn-secondary:hover { background: #eee; }

        .stats { display: flex; gap: 24px; margin-bottom: 16px; }
        .stat-box { background: #fff; border-radius: 8px; padding: 14px 20px; box-shadow: 0 1px 4px rgba(0,0,0,.08); flex: 1; }
        .stat-box .val { font-size: 28px; font-weight: 700; color: #1976D2; }
        .stat-box .lbl { font-size: 12px; color: #888; margin-top: 2px; }

        .table-card { background: #fff; border-radius: 10px; box-shadow: 0 1px 4px rgba(0,0,0,.08); overflow: hidden; }
        .table-wrap { overflow-x: auto; }
        table { width: 100%; border-collapse: collapse; font-size: 14px; }
        thead th { background: #1976D2; color: #fff; padding: 11px 14px; text-align: left; font-weight: 500; white-space: nowrap; }
        tbody tr:nth-child(even) { background: #fafafa; }
        tbody tr:hover { background: #E3F2FD; }
        tbody td { padding: 9px 14px; border-bottom: 1px solid #f0f0f0; }
        .badge { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 12px; background: #E3F2FD; color: #1565C0; }
        .empty { text-align: center; padding: 48px; color: #999; }

        /* Pagination */
        .pagination { display: flex; align-items: center; justify-content: space-between; padding: 12px 16px; border-top: 1px solid #f0f0f0; flex-wrap: wrap; gap: 8px; }
        .pagination .info { font-size: 13px; color: #666; }
        .pages { display: flex; gap: 4px; flex-wrap: wrap; }
        .pages a, .pages span {
            display: inline-block; padding: 5px 10px; border-radius: 5px;
            font-size: 13px; text-decoration: none; border: 1px solid #ddd; color: #333;
        }
        .pages a:hover { background: #E3F2FD; border-color: #1976D2; color: #1976D2; }
        .pages span.active { background: #1976D2; color: #fff; border-color: #1976D2; }
        .pages span.dots { border: none; color: #999; }

        /* Image modal */
        .img-btn { background: none; border: none; cursor: pointer; font-size: 18px; padding: 2px 6px; border-radius: 4px; transition: background .15s; }
        .img-btn:hover { background: #E3F2FD; }
        .img-btn.no-img { opacity: .25; cursor: default; }
        .img-btn.no-img:hover { background: none; }
        .modal-overlay { display: none; position: fixed; inset: 0; background: rgba(0,0,0,.75); z-index: 1000; align-items: center; justify-content: center; }
        .modal-overlay.open { display: flex; }
        .modal-box { background: #fff; border-radius: 12px; padding: 16px; max-width: 90vw; max-height: 90vh; display: flex; flex-direction: column; gap: 12px; }
        .modal-box img { max-width: 80vw; max-height: 75vh; object-fit: contain; border-radius: 6px; }
        .modal-meta { font-size: 13px; color: #555; }
        .modal-close { align-self: flex-end; background: #f5f5f5; border: none; padding: 6px 14px; border-radius: 6px; cursor: pointer; font-size: 13px; }
        .modal-close:hover { background: #eee; }
    </style>
</head>
<body>

<?php if (!$authed): ?>
<div class="login-wrap">
    <div class="login-box">
        <h1>📦 Packing Station</h1>
        <p>Nhập mật khẩu để xem báo cáo</p>
        <form method="POST">
            <input type="password" name="password" placeholder="Mật khẩu" autofocus>
            <button type="submit">Đăng nhập</button>
            <?php if (isset($loginError)): ?>
                <p class="error">⚠ <?= htmlspecialchars($loginError) ?></p>
            <?php endif; ?>
        </form>
    </div>
</div>

<?php else: ?>
<div class="header">
    <h1>📦 Báo cáo quét mã vạch</h1>
    <form method="POST" style="margin:0">
        <button type="submit" name="logout">Đăng xuất</button>
    </form>
</div>

<div class="container">

    <!-- Filter -->
    <div class="card">
        <h2>🔍 Bộ lọc</h2>
        <form method="GET">
            <div class="filter-row">
                <div class="filter-group">
                    <label>Worker ID</label>
                    <input type="text" name="worker" value="<?= htmlspecialchars($filterWorker) ?>" placeholder="Tất cả">
                </div>
                <div class="filter-group">
                    <label>Từ ngày</label>
                    <input type="date" name="from" value="<?= htmlspecialchars($filterFrom) ?>">
                </div>
                <div class="filter-group">
                    <label>Đến ngày</label>
                    <input type="date" name="to" value="<?= htmlspecialchars($filterTo) ?>">
                </div>
                <button type="submit" class="btn btn-primary">Lọc</button>
                <a href="?" class="btn btn-secondary">Xóa lọc</a>
                <?php if ($totalCount > 0): ?>
                <a href="?<?= http_build_query(array_filter(['worker' => $filterWorker, 'from' => $filterFrom, 'to' => $filterTo])) ?>&export=1"
                   class="btn btn-success">⬇ Xuất Excel (<?= number_format($totalCount) ?> dòng)</a>
                <?php endif; ?>
            </div>
        </form>
    </div>

    <!-- Stats -->
    <?php if ($totalCount > 0): ?>
    <div class="stats">
        <div class="stat-box">
            <div class="val"><?= number_format($totalCount) ?></div>
            <div class="lbl">Tổng bản ghi</div>
        </div>
        <div class="stat-box">
            <div class="val"><?= $totalPages ?></div>
            <div class="lbl">Số trang (<?= PAGE_SIZE ?> dòng/trang)</div>
        </div>
        <div class="stat-box">
            <div class="val"><?= $currentPage ?></div>
            <div class="lbl">Trang hiện tại</div>
        </div>
    </div>
    <?php endif; ?>

    <!-- Table -->
    <div class="table-card">
        <?php if (empty($records)): ?>
            <div class="empty">Không có dữ liệu. Hãy thử thay đổi bộ lọc.</div>
        <?php else: ?>
        <div class="table-wrap">
            <table>
                <thead>
                    <tr>
                        <th>#</th>
                        <th>Ngày giờ quét</th>
                        <th>Worker ID</th>
                        <th>Mã vạch</th>
                        <th>Định dạng</th>
                        <th>Session ID</th>
                        <th style="text-align:center">Ảnh</th>
                    </tr>
                </thead>
                <tbody>
                    <?php
                    $startIdx = ($currentPage - 1) * PAGE_SIZE + 1;
                    foreach ($records as $i => $row):
                    ?>
                    <tr>
                        <td style="color:#999"><?= $startIdx + $i ?></td>
                        <td><?= htmlspecialchars($row['scannedAt']) ?></td>
                        <td><strong><?= htmlspecialchars($row['workerId']) ?></strong></td>
                        <td><?= htmlspecialchars($row['barcodeValue']) ?></td>
                        <td><span class="badge"><?= htmlspecialchars($row['barcodeFormat']) ?></span></td>
                        <td style="font-size:12px;color:#aaa"><?= htmlspecialchars(substr($row['sessionId'], 0, 8)) ?>…</td>
                        <td style="text-align:center">
                            <?php if ($row['imageUrl'] !== ''): ?>
                                <button class="img-btn" onclick="openModal('<?= htmlspecialchars($row['imageUrl'], ENT_QUOTES) ?>','<?= htmlspecialchars($row['barcodeValue'], ENT_QUOTES) ?>','<?= htmlspecialchars($row['scannedAt'], ENT_QUOTES) ?>')" title="Xem ảnh">🖼️</button>
                            <?php else: ?>
                                <button class="img-btn no-img" disabled title="Không có ảnh">🖼️</button>
                            <?php endif; ?>
                        </td>
                    </tr>
                    <?php endforeach; ?>
                </tbody>
            </table>
        </div>

        <!-- Pagination -->
        <div class="pagination">
            <span class="info">
                Hiển thị <?= $startIdx ?>–<?= min($startIdx + PAGE_SIZE - 1, $totalCount) ?>
                trong <?= number_format($totalCount) ?> bản ghi
            </span>
            <div class="pages">
                <?php if ($currentPage > 1): ?>
                    <a href="<?= pageUrl(1, $filterWorker, $filterFrom, $filterTo) ?>">«</a>
                    <a href="<?= pageUrl($currentPage - 1, $filterWorker, $filterFrom, $filterTo) ?>">‹</a>
                <?php endif; ?>

                <?php
                // Hiển thị tối đa 7 trang xung quanh trang hiện tại
                $start = max(1, $currentPage - 3);
                $end   = min($totalPages, $currentPage + 3);
                if ($start > 1) echo '<span class="dots">…</span>';
                for ($p = $start; $p <= $end; $p++):
                ?>
                    <?php if ($p === $currentPage): ?>
                        <span class="active"><?= $p ?></span>
                    <?php else: ?>
                        <a href="<?= pageUrl($p, $filterWorker, $filterFrom, $filterTo) ?>"><?= $p ?></a>
                    <?php endif; ?>
                <?php endfor; ?>
                <?php if ($end < $totalPages) echo '<span class="dots">…</span>'; ?>

                <?php if ($currentPage < $totalPages): ?>
                    <a href="<?= pageUrl($currentPage + 1, $filterWorker, $filterFrom, $filterTo) ?>">›</a>
                    <a href="<?= pageUrl($totalPages, $filterWorker, $filterFrom, $filterTo) ?>">»</a>
                <?php endif; ?>
            </div>
        </div>
        <?php endif; ?>
    </div>
</div>
<?php endif; ?>

<!-- Image Modal -->
<div class="modal-overlay" id="imgModal" onclick="closeModal(event)">
    <div class="modal-box">
        <button class="modal-close" onclick="closeModal()">✕ Đóng</button>
        <img id="modalImg" src="" alt="Evidence">
        <div class="modal-meta">
            <strong id="modalBarcode"></strong><br>
            <span id="modalTime" style="color:#999"></span>
        </div>
    </div>
</div>

<script>
function openModal(url, barcode, time) {
    document.getElementById('modalImg').src = url;
    document.getElementById('modalBarcode').textContent = barcode;
    document.getElementById('modalTime').textContent = time;
    document.getElementById('imgModal').classList.add('open');
}
function closeModal(e) {
    if (!e || e.target === document.getElementById('imgModal')) {
        document.getElementById('imgModal').classList.remove('open');
        document.getElementById('modalImg').src = '';
    }
}
document.addEventListener('keydown', e => { if (e.key === 'Escape') closeModal(); });
</script>

</body>
</html>
