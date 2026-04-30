<?php
declare(strict_types=1);
require_once dirname(__DIR__, 2) . '/vendor/autoload.php';

use App\DB\MongoDB as DB;
use MongoDB\BSON\UTCDateTime;
use PhpOffice\PhpSpreadsheet\Spreadsheet;
use PhpOffice\PhpSpreadsheet\Writer\Xlsx;
use PhpOffice\PhpSpreadsheet\Style\Alignment;
use PhpOffice\PhpSpreadsheet\Style\Fill;

require_once 'layout.php';

const PAGE_SIZE    = 50;
const EXPORT_BATCH = 1000;

function formatDateTime(mixed $value): string
{
    if ($value instanceof UTCDateTime) {
        return $value->toDateTime()->setTimezone(new DateTimeZone('Asia/Ho_Chi_Minh'))->format('d/m/Y H:i:s');
    }
    if (is_string($value) && $value !== '') {
        try { return (new DateTime($value))->setTimezone(new DateTimeZone('Asia/Ho_Chi_Minh'))->format('d/m/Y H:i:s'); }
        catch (Exception) {}
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

function buildFilter(string $worker, string $from, string $to): array
{
    $filter = [];
    if ($worker !== '') $filter['workerId'] = $worker;
    if ($from !== '' || $to !== '') {
        $filter['scannedAt'] = [];
        if ($from !== '') $filter['scannedAt']['$gte'] = $from . 'T00:00:00Z';
        if ($to   !== '') $filter['scannedAt']['$lte'] = $to   . 'T23:59:59Z';
    }
    return $filter;
}

function pageUrl(int $page, string $worker, string $from, string $to): string
{
    return '?' . http_build_query(array_filter(['worker' => $worker, 'from' => $from, 'to' => $to, 'page' => $page]));
}

$filterWorker = trim($_GET['worker'] ?? '');
$filterFrom   = $_GET['from'] ?? '';
$filterTo     = $_GET['to']   ?? '';
$currentPage  = max(1, (int)($_GET['page'] ?? 1));
$filter       = buildFilter($filterWorker, $filterFrom, $filterTo);

// Export
if (isset($_GET['export'])) {
    $totalCount  = DB::get()->packing_records->countDocuments($filter);
    $spreadsheet = new Spreadsheet();
    $sheet       = $spreadsheet->getActiveSheet()->setTitle('Báo cáo');
    $headers     = ['STT', 'Ngày giờ quét', 'Worker ID', 'Mã vạch', 'Định dạng', 'Session ID'];
    foreach ($headers as $col => $title) $sheet->setCellValue(chr(65 + $col) . '1', $title);
    $sheet->getStyle('A1:F1')->applyFromArray([
        'font' => ['bold' => true, 'color' => ['rgb' => 'FFFFFF']],
        'fill' => ['fillType' => Fill::FILL_SOLID, 'startColor' => ['rgb' => '3b82f6']],
        'alignment' => ['horizontal' => Alignment::HORIZONTAL_CENTER],
    ]);
    $rowIndex = 2; $globalIdx = 1; $skip = 0;
    while ($skip < $totalCount) {
        $cursor = DB::get()->packing_records->find($filter, ['sort' => ['scannedAt' => 1], 'skip' => $skip, 'limit' => EXPORT_BATCH]);
        foreach ($cursor as $doc) {
            $row = docToRow($doc); $r = $rowIndex++;
            $sheet->setCellValue("A$r", $globalIdx++);
            $sheet->setCellValue("B$r", $row['scannedAt']);
            $sheet->setCellValue("C$r", $row['workerId']);
            $sheet->setCellValue("D$r", $row['barcodeValue']);
            $sheet->setCellValue("E$r", $row['barcodeFormat']);
            $sheet->setCellValue("F$r", $row['sessionId']);
        }
        $skip += EXPORT_BATCH;
    }
    foreach (range('A', 'F') as $col) $sheet->getColumnDimension($col)->setAutoSize(true);
    header('Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
    header('Content-Disposition: attachment; filename="packing-' . date('Ymd-His') . '.xlsx"');
    (new Xlsx($spreadsheet))->save('php://output');
    exit;
}

$totalCount  = DB::get()->packing_records->countDocuments($filter);
$totalPages  = max(1, (int)ceil($totalCount / PAGE_SIZE));
$currentPage = min($currentPage, $totalPages);
$cursor      = DB::get()->packing_records->find($filter, ['sort' => ['scannedAt' => -1], 'skip' => ($currentPage - 1) * PAGE_SIZE, 'limit' => PAGE_SIZE]);
$records     = [];
foreach ($cursor as $doc) $records[] = docToRow($doc);
?>

<!-- Stats -->
<div class="stats">
    <div class="stat"><div class="val"><?= number_format($totalCount) ?></div><div class="lbl">Tổng bản ghi</div></div>
    <div class="stat"><div class="val"><?= $totalPages ?></div><div class="lbl">Số trang</div></div>
    <div class="stat"><div class="val"><?= $currentPage ?></div><div class="lbl">Trang hiện tại</div></div>
</div>

<!-- Filter -->
<div class="card" style="margin-bottom:20px">
    <div class="card-body">
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
                <button type="submit" class="btn btn-primary">🔍 Lọc</button>
                <a href="packing.php" class="btn btn-ghost">✕ Xóa lọc</a>
                <?php if ($totalCount > 0): ?>
                <a href="?<?= http_build_query(array_filter(['worker' => $filterWorker, 'from' => $filterFrom, 'to' => $filterTo])) ?>&export=1" class="btn btn-success">⬇ Excel</a>
                <?php endif; ?>
            </div>
        </form>
    </div>
</div>

<!-- Table -->
<div class="card">
    <?php if (empty($records)): ?>
        <div class="empty">Không có dữ liệu</div>
    <?php else: ?>
    <div class="table-wrap">
        <table>
            <thead>
                <tr>
                    <th>#</th><th>Ngày giờ quét</th><th>Worker ID</th><th>Mã vạch</th><th>Định dạng</th><th>Session</th><th style="text-align:center">Ảnh</th>
                </tr>
            </thead>
            <tbody>
                <?php $startIdx = ($currentPage - 1) * PAGE_SIZE + 1; ?>
                <?php foreach ($records as $i => $row): ?>
                <tr>
                    <td style="color:#94a3b8"><?= $startIdx + $i ?></td>
                    <td><?= htmlspecialchars($row['scannedAt']) ?></td>
                    <td><strong><?= htmlspecialchars($row['workerId']) ?></strong></td>
                    <td><?= htmlspecialchars($row['barcodeValue']) ?></td>
                    <td><span class="badge badge-blue"><?= htmlspecialchars($row['barcodeFormat']) ?></span></td>
                    <td style="color:#94a3b8;font-size:12px"><?= htmlspecialchars(substr($row['sessionId'], 0, 8)) ?>…</td>
                    <td style="text-align:center">
                        <?php if ($row['imageUrl'] !== ''): ?>
                            <button onclick="openModal('<?= htmlspecialchars($row['imageUrl'], ENT_QUOTES) ?>','<?= htmlspecialchars($row['barcodeValue'], ENT_QUOTES) ?>','<?= htmlspecialchars($row['scannedAt'], ENT_QUOTES) ?>')" style="background:none;border:none;cursor:pointer;font-size:16px">🖼️</button>
                        <?php else: ?>
                            <span style="opacity:.3;font-size:16px">🖼️</span>
                        <?php endif; ?>
                    </td>
                </tr>
                <?php endforeach; ?>
            </tbody>
        </table>
    </div>
    <div class="pagination">
        <span class="info">Hiển thị <?= $startIdx ?>–<?= min($startIdx + PAGE_SIZE - 1, $totalCount) ?> / <?= number_format($totalCount) ?></span>
        <div class="pages">
            <?php if ($currentPage > 1): ?>
                <a href="<?= pageUrl(1, $filterWorker, $filterFrom, $filterTo) ?>">«</a>
                <a href="<?= pageUrl($currentPage - 1, $filterWorker, $filterFrom, $filterTo) ?>">‹</a>
            <?php endif; ?>
            <?php $start = max(1, $currentPage - 3); $end = min($totalPages, $currentPage + 3); ?>
            <?php if ($start > 1) echo '<span class="dots">…</span>'; ?>
            <?php for ($p = $start; $p <= $end; $p++): ?>
                <?php if ($p === $currentPage): ?><span class="active"><?= $p ?></span>
                <?php else: ?><a href="<?= pageUrl($p, $filterWorker, $filterFrom, $filterTo) ?>"><?= $p ?></a><?php endif; ?>
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

<!-- Image Modal -->
<div id="modal" onclick="if(event.target===this)closeModal()" style="display:none;position:fixed;inset:0;background:rgba(0,0,0,.75);z-index:1000;align-items:center;justify-content:center">
    <div style="background:#fff;border-radius:12px;padding:20px;max-width:90vw;display:flex;flex-direction:column;gap:12px">
        <button onclick="closeModal()" style="align-self:flex-end;background:#f1f5f9;border:none;padding:6px 14px;border-radius:6px;cursor:pointer">✕ Đóng</button>
        <img id="modalImg" src="" style="max-width:80vw;max-height:75vh;object-fit:contain;border-radius:6px">
        <div style="font-size:13px;color:#555"><strong id="modalBarcode"></strong><br><span id="modalTime" style="color:#94a3b8"></span></div>
    </div>
</div>
<script>
function openModal(url,b,t){document.getElementById('modalImg').src=url;document.getElementById('modalBarcode').textContent=b;document.getElementById('modalTime').textContent=t;document.getElementById('modal').style.display='flex';}
function closeModal(){document.getElementById('modal').style.display='none';document.getElementById('modalImg').src='';}
document.addEventListener('keydown',e=>{if(e.key==='Escape')closeModal();});
</script>

    </div><!-- .content -->
</div><!-- .main -->
</body>
</html>
