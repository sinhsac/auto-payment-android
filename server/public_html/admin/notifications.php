<?php
declare(strict_types=1);
require_once dirname(__DIR__, 2) . '/vendor/autoload.php';

use App\DB\MongoDB as DB;
use MongoDB\BSON\UTCDateTime;

require_once 'layout.php';

const NOTI_PAGE_SIZE = 50;

function notiFormatTime(mixed $value): string
{
    if ($value instanceof UTCDateTime) {
        return $value->toDateTime()->setTimezone(new DateTimeZone('Asia/Ho_Chi_Minh'))->format('d/m/Y H:i:s');
    }
    return is_string($value) ? $value : '';
}

function notiPageUrl(int $page, string $pck, string $from, string $to, string $q): string
{
    return '?' . http_build_query(array_filter(['pck' => $pck, 'from' => $from, 'to' => $to, 'q' => $q, 'page' => $page]));
}

$filterPck  = trim($_GET['pck']  ?? '');
$filterFrom = $_GET['from'] ?? '';
$filterTo   = $_GET['to']   ?? '';
$filterQ    = trim($_GET['q']    ?? '');
$currentPage = max(1, (int)($_GET['page'] ?? 1));

$filter = [];
if ($filterPck  !== '') $filter['pck']   = $filterPck;
if ($filterQ    !== '') $filter['$or']   = [['title' => ['$regex' => $filterQ, '$options' => 'i']], ['msg' => ['$regex' => $filterQ, '$options' => 'i']]];
if ($filterFrom !== '' || $filterTo !== '') {
    $filter['receivedAt'] = [];
    if ($filterFrom !== '') $filter['receivedAt']['$gte'] = new UTCDateTime((new DateTime($filterFrom . 'T00:00:00+07:00'))->getTimestamp() * 1000);
    if ($filterTo   !== '') $filter['receivedAt']['$lte'] = new UTCDateTime((new DateTime($filterTo   . 'T23:59:59+07:00'))->getTimestamp() * 1000);
}

$totalCount  = DB::get()->notifications->countDocuments($filter);
$totalPages  = max(1, (int)ceil($totalCount / NOTI_PAGE_SIZE));
$currentPage = min($currentPage, $totalPages);
$cursor      = DB::get()->notifications->find($filter, ['sort' => ['receivedAt' => -1], 'skip' => ($currentPage - 1) * NOTI_PAGE_SIZE, 'limit' => NOTI_PAGE_SIZE]);

$records = [];
foreach ($cursor as $doc) {
    $records[] = [
        'pck'        => (string)($doc['pck']        ?? ''),
        'title'      => (string)($doc['title']      ?? ''),
        'msg'        => (string)($doc['msg']         ?? ''),
        'postTime'   => (string)($doc['postTime']    ?? ''),
        'receivedAt' => notiFormatTime($doc['receivedAt'] ?? ''),
    ];
}

// Lấy danh sách app để filter dropdown
$apps = DB::get()->notifications->distinct('pck', []);
sort($apps);
?>

<!-- Stats -->
<div class="stats">
    <div class="stat"><div class="val"><?= number_format($totalCount) ?></div><div class="lbl">Tổng thông báo</div></div>
    <div class="stat"><div class="val"><?= count($apps) ?></div><div class="lbl">Số ứng dụng</div></div>
    <div class="stat"><div class="val"><?= $totalPages ?></div><div class="lbl">Số trang</div></div>
</div>

<!-- Filter -->
<div class="card" style="margin-bottom:20px">
    <div class="card-body">
        <form method="GET">
            <div class="filter-row">
                <div class="filter-group">
                    <label>Ứng dụng</label>
                    <select name="pck">
                        <option value="">Tất cả</option>
                        <?php foreach ($apps as $app): ?>
                            <option value="<?= htmlspecialchars($app) ?>" <?= $filterPck === $app ? 'selected' : '' ?>>
                                <?= htmlspecialchars($app) ?>
                            </option>
                        <?php endforeach; ?>
                    </select>
                </div>
                <div class="filter-group">
                    <label>Tìm kiếm</label>
                    <input type="text" name="q" value="<?= htmlspecialchars($filterQ) ?>" placeholder="Tiêu đề hoặc nội dung...">
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
                <a href="notifications.php" class="btn btn-ghost">✕ Xóa lọc</a>
            </div>
        </form>
    </div>
</div>

<!-- Table -->
<div class="card">
    <?php if (empty($records)): ?>
        <div class="empty">Không có thông báo nào</div>
    <?php else: ?>
    <div class="table-wrap">
        <table>
            <thead>
                <tr>
                    <th>#</th>
                    <th>Ứng dụng</th>
                    <th>Tiêu đề</th>
                    <th>Nội dung</th>
                    <th>Thời gian gửi</th>
                    <th>Nhận lúc</th>
                </tr>
            </thead>
            <tbody>
                <?php $startIdx = ($currentPage - 1) * NOTI_PAGE_SIZE + 1; ?>
                <?php foreach ($records as $i => $row): ?>
                <tr>
                    <td style="color:#94a3b8"><?= $startIdx + $i ?></td>
                    <td>
                        <span class="badge badge-blue" style="font-size:11px;max-width:160px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;display:inline-block" title="<?= htmlspecialchars($row['pck']) ?>">
                            <?= htmlspecialchars(basename(str_replace('.', '/', $row['pck']))) ?>
                        </span>
                    </td>
                    <td style="max-width:200px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap" title="<?= htmlspecialchars($row['title']) ?>">
                        <strong><?= htmlspecialchars($row['title']) ?></strong>
                    </td>
                    <td style="max-width:300px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;color:#64748b" title="<?= htmlspecialchars($row['msg']) ?>">
                        <?= htmlspecialchars($row['msg']) ?>
                    </td>
                    <td style="white-space:nowrap;color:#64748b;font-size:12px"><?= htmlspecialchars($row['postTime']) ?></td>
                    <td style="white-space:nowrap;font-size:12px"><?= htmlspecialchars($row['receivedAt']) ?></td>
                </tr>
                <?php endforeach; ?>
            </tbody>
        </table>
    </div>
    <div class="pagination">
        <span class="info">Hiển thị <?= $startIdx ?>–<?= min($startIdx + NOTI_PAGE_SIZE - 1, $totalCount) ?> / <?= number_format($totalCount) ?></span>
        <div class="pages">
            <?php if ($currentPage > 1): ?>
                <a href="<?= notiPageUrl(1, $filterPck, $filterFrom, $filterTo, $filterQ) ?>">«</a>
                <a href="<?= notiPageUrl($currentPage - 1, $filterPck, $filterFrom, $filterTo, $filterQ) ?>">‹</a>
            <?php endif; ?>
            <?php $start = max(1, $currentPage - 3); $end = min($totalPages, $currentPage + 3); ?>
            <?php if ($start > 1) echo '<span class="dots">…</span>'; ?>
            <?php for ($p = $start; $p <= $end; $p++): ?>
                <?php if ($p === $currentPage): ?><span class="active"><?= $p ?></span>
                <?php else: ?><a href="<?= notiPageUrl($p, $filterPck, $filterFrom, $filterTo, $filterQ) ?>"><?= $p ?></a><?php endif; ?>
            <?php endfor; ?>
            <?php if ($end < $totalPages) echo '<span class="dots">…</span>'; ?>
            <?php if ($currentPage < $totalPages): ?>
                <a href="<?= notiPageUrl($currentPage + 1, $filterPck, $filterFrom, $filterTo, $filterQ) ?>">›</a>
                <a href="<?= notiPageUrl($totalPages, $filterPck, $filterFrom, $filterTo, $filterQ) ?>">»</a>
            <?php endif; ?>
        </div>
    </div>
    <?php endif; ?>
</div>

    </div><!-- .content -->
</div><!-- .main -->
</body>
</html>
