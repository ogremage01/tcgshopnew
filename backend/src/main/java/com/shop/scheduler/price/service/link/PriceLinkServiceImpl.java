package com.shop.scheduler.price.service.link;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import com.shop.log.sync.event.SyncLogEvent;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CompletableFuture;

import com.shop.card.repository.FabPriceRepository;
import com.shop.card.repository.MtgPriceRepository;
import com.shop.card.repository.TcgPPriceRepository;

import com.shop.card.entity.TcgPPrice;
import com.shop.card.entity.MtgPrice;
import com.shop.card.entity.FabPrice;
import com.shop.scheduler.price.service.batch.InfoAddService;

/**
 * mtg_prices·fab_prices와 tcg_p_prices 간의 tcgPPriceId·obPriceId 연결(링크) 담당.
 *
 * ── 처리 전략 ────────────────────────────────────────────────────────────────
 *
 * [이전] UPDATE ... INNER JOIN ... LIMIT N (반복)
 * 문제: LIMIT이 있어도 MariaDB는 매번 풀 조인 스캔으로 N건을 찾는다.
 * 조인 대상이 많을수록 각 배치도 느려지고 락 점유 시간이 누적된다.
 *
 * [현재] SELECT → PK batchUpdate 분리
 * 1. SELECT: check_code JOIN으로 매칭 ID 쌍만 수집 (읽기 전용, 락 없음)
 * 2. UPDATE: 수집한 PK 목록으로 인덱스 직접 갱신 (JOIN 없음, 락 최소화)
 *
 * 기본 링크(syncMtg/Fab)는 PK 커서(m.id / f.id)로 배치를 넘긴다(대량 스캔 완화).
 * 갭 정비용 Rescan 메서드는 커서 없이 NULL 행만 반복 LIMIT 처리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PriceLinkServiceImpl implements PriceLinkService {

  private static final String PRICE_LINK_LOG_SOURCE = "price-link-overwrite";
  private static final String PRICE_LINK_RESCAN_LOG = "price-link-rescan-nulls";
  private static final String PRICE_LINK_CHECK_CODE_REBUILD_LOG = "price-check-code-rebuild";
  private static final String PRICE_LINK_CHECK_CODE_SELECTIVE_REBUILD_LOG = "price-check-code-selective-rebuild";
  private static final String PRICE_LINK_REBUILD_LOG = "price-link-rebuild";
  private static final String PRICE_LINK_FULL_REBUILD_LOG = "price-full-rebuild";
  private static final List<String> SYNC_GAMES = List.of(
      "Magic",
      "Flesh & Blood TCG",
      "Star Wars Unlimited",
      "Lorcana TCG",
      "Riftbound League of Legends Trading Card Game");
  private static final List<String> CODE_NUMBER_REFRESH_GAMES = List.of(
      "Star Wars Unlimited",
      "Lorcana TCG",
      "Riftbound League of Legends Trading Card Game");

  private final AtomicBoolean syncPriceLinkRunning = new AtomicBoolean(false);

  private final JdbcTemplate jdbcTemplate;
  private final ApplicationEventPublisher eventPublisher;
  private final TransactionTemplate transactionTemplate;

  private final FabPriceRepository fabPriceRepository;
  private final MtgPriceRepository mtgPriceRepository;
  private final TcgPPriceRepository tcgPPriceRepository;

  private final InfoAddService infoAddService;

  @Override
  public boolean isSyncPriceLinkRunning() {
    return syncPriceLinkRunning.get();
  }

  @Override
  public boolean startSyncPriceLink() {
    if (!syncPriceLinkRunning.compareAndSet(false, true)) {
      log.warn("Price link sync is already running. Skip this request.");
      return false;
    }
    LocalDateTime startTime = LocalDateTime.now();
    CompletableFuture.runAsync(() -> {
      String result = "failure";
      String message = "";
      try {
        syncFabPriceWithTcgPrice();
        syncMtgPriceWithTcgPrice();
        logPriceLinkDiagnostics("after_cursor_sync");
        result = "success";
      } catch (Exception e) {
        log.error("Price link sync failed with an unexpected exception", e);
        message = e.getMessage() != null && !e.getMessage().isBlank()
            ? e.getMessage()
            : e.getClass().getSimpleName();
      } finally {
        LocalDateTime endTime = LocalDateTime.now();
        eventPublisher.publishEvent(
            new SyncLogEvent("price-link-overwrite", PRICE_LINK_LOG_SOURCE, startTime, endTime, result, message));
        syncPriceLinkRunning.set(false);
      }
    });
    return true;
  }

  @Override
  public boolean startSyncPriceLinkRescanNulls() {
    if (!syncPriceLinkRunning.compareAndSet(false, true)) {
      log.warn("Price link rescan-nulls is already running. Skip this request.");
      return false;
    }
    LocalDateTime startTime = LocalDateTime.now();
    CompletableFuture.runAsync(() -> {
      String result = "failure";
      String message = "";
      try {
        syncFabPriceWithTcgPriceRescanNulls();
        syncMtgPriceWithTcgPriceRescanNulls();
        logPriceLinkDiagnostics("after_rescan_nulls_sync");
        result = "success";
      } catch (Exception e) {
        log.error("Price link rescan-nulls failed", e);
        message = e.getMessage() != null && !e.getMessage().isBlank()
            ? e.getMessage()
            : e.getClass().getSimpleName();
      } finally {
        LocalDateTime endTime = LocalDateTime.now();
        eventPublisher.publishEvent(
            new SyncLogEvent(PRICE_LINK_RESCAN_LOG, PRICE_LINK_RESCAN_LOG, startTime, endTime, result, message));
        syncPriceLinkRunning.set(false);
      }
    });
    return true;
  }

  /** SELECT 한 번에 가져올 행 수 */
  private static final int SELECT_CHUNK = 500;
  /** PK UPDATE batchUpdate 한 묶음 크기 */
  private static final int UPDATE_BATCH = 200;

  private record MtgLinkRow(long mtgId, long tcgId, String condition, String number, String productName) {
  }

  private record FabLinkRow(long fabId, long tcgId, String condition, String number, String productName) {
  }

  private record CheckCodeRow(long id, String checkCode) {
  }

  private static final String SQL_INIT_TCG_CHECK_CODES = """
      UPDATE tcg_p_prices SET check_code = ?, check_code_refined = ? WHERE id = ?
      """;
  private static final String SQL_INIT_MTG_CHECK_CODES = """
      UPDATE mtg_prices SET check_code = ?, check_code_refined = ? WHERE id = ?
      """;
  private static final String SQL_INIT_FAB_CHECK_CODES = """
      UPDATE fab_prices SET check_code = ?, check_code_refined = ? WHERE id = ?
      """;

  private static final String SQL_TCG_CHECK_CODE_ONLY = """
      UPDATE tcg_p_prices SET check_code = ? WHERE id = ?
      """;
  private static final String SQL_MTG_CHECK_CODE_ONLY = """
      UPDATE mtg_prices SET check_code = ? WHERE id = ?
      """;
  private static final String SQL_FAB_CHECK_CODE_ONLY = """
      UPDATE fab_prices SET check_code = ? WHERE id = ?
      """;

  private void batchUpdateInitCheckCodes(String sql, List<CheckCodeRow> rows) {
    if (rows.isEmpty()) {
      return;
    }
    for (List<CheckCodeRow> chunk : partition(rows, UPDATE_BATCH)) {
      jdbcTemplate.batchUpdate(sql, chunk, UPDATE_BATCH, (ps, row) -> {
        ps.setString(1, row.checkCode());
        ps.setString(2, row.checkCode());
        ps.setLong(3, row.id());
      });
    }
  }

  private void batchUpdateCheckCodeOnly(String sql, List<CheckCodeRow> rows) {
    if (rows.isEmpty()) {
      return;
    }
    for (List<CheckCodeRow> chunk : partition(rows, UPDATE_BATCH)) {
      jdbcTemplate.batchUpdate(sql, chunk, UPDATE_BATCH, (ps, row) -> {
        ps.setString(1, row.checkCode());
        ps.setLong(2, row.id());
      });
    }
  }

  /**
   * 기본 MTG 링크: PK 커서(m.id)로 배치를 넘긴다.
   */
  @Override
  public void syncMtgPriceWithTcgPrice() {
    String selectSql = """
        SELECT x.m_id, x.t_id, x.card_condition, x.number, x.product_name
        FROM (
          SELECT m.id AS m_id,
                 t.id AS t_id,
                 t.card_condition,
                 t.number,
                 t.product_name,
                 ROW_NUMBER() OVER (PARTITION BY m.id ORDER BY t.id ASC) AS rn
          FROM mtg_prices m
          INNER JOIN tcg_p_prices t ON m.check_code_refined = t.check_code_refined
          WHERE m.tcg_p_price_id IS NULL
            AND m.id > ?
            AND t.game = 'Magic'
            AND t.card_condition NOT LIKE '%-%'
        ) x
        WHERE x.rn = 1
        ORDER BY x.m_id ASC
        LIMIT ?
        """;

    runMtgLinkSelectUpdateLoop(selectSql, true);
  }

  /**
   * 갭 정비: JOIN에 안 걸렸다가 나중에 맞는 행이 PK 커서 밖으로 밀려 스킵되는 경우를 줄인다.
   */
  @Override
  public void syncMtgPriceWithTcgPriceRescanNulls() {
    String selectSql = """
        SELECT x.m_id, x.t_id, x.card_condition, x.number, x.product_name
        FROM (
          SELECT m.id AS m_id,
                 t.id AS t_id,
                 t.card_condition,
                 t.number,
                 t.product_name,
                 ROW_NUMBER() OVER (PARTITION BY m.id ORDER BY t.id ASC) AS rn
          FROM mtg_prices m
          INNER JOIN tcg_p_prices t ON m.check_code_refined = t.check_code_refined
          WHERE m.tcg_p_price_id IS NULL
            AND t.game = 'Magic'
            AND t.card_condition NOT LIKE '%-%'
        ) x
        WHERE x.rn = 1
        ORDER BY x.m_id ASC
        LIMIT ?
        """;
    runMtgLinkSelectUpdateLoop(selectSql, false);
  }

  private void runMtgLinkSelectUpdateLoop(String selectSql, boolean useIdCursor) {
    String updateMtgSql = "UPDATE mtg_prices SET tcg_p_price_id = ?, con_num_name = ? WHERE id = ?";
    String updateTcgSql = "UPDATE tcg_p_prices SET ob_price_id = ? WHERE id = ?";

    int totalLinked = 0;
    long lastId = 0;

    while (true) {
      List<MtgLinkRow> batch = useIdCursor
          ? jdbcTemplate.query(
              selectSql,
              (rs, i) -> new MtgLinkRow(
                  rs.getLong(1), rs.getLong(2),
                  rs.getString(3), rs.getString(4), rs.getString(5)),
              lastId, SELECT_CHUNK)
          : jdbcTemplate.query(
              selectSql,
              (rs, i) -> new MtgLinkRow(
                  rs.getLong(1), rs.getLong(2),
                  rs.getString(3), rs.getString(4), rs.getString(5)),
              SELECT_CHUNK);

      if (batch.isEmpty()) {
        break;
      }

      for (List<MtgLinkRow> chunk : partition(batch, UPDATE_BATCH)) {
        transactionTemplate.executeWithoutResult(__ -> {
          jdbcTemplate.batchUpdate(updateMtgSql, chunk, UPDATE_BATCH,
              (ps, row) -> {
                ps.setLong(1, row.tcgId());
                ps.setString(2, row.condition() + "-" + row.number() + "-" + row.productName());
                ps.setLong(3, row.mtgId());
              });
          jdbcTemplate.batchUpdate(updateTcgSql, chunk, UPDATE_BATCH,
              (ps, row) -> {
                ps.setLong(1, row.mtgId());
                ps.setLong(2, row.tcgId());
              });
        });
      }

      totalLinked += batch.size();
      if (useIdCursor) {
        lastId = batch.get(batch.size() - 1).mtgId();
      }
      if (batch.size() < SELECT_CHUNK) {
        break;
      }
    }

    log.info("MTG price link sync{}: linked {} rows", useIdCursor ? "" : " (rescan-nulls)", totalLinked);
    backfillTcgObPriceIdFromMtgLinks();
  }

  @Override
  public void syncFabPriceWithTcgPrice() {
    String selectSql = """
        SELECT x.f_id, x.t_id, x.card_condition, x.number, x.product_name
        FROM (
          SELECT f.id AS f_id,
                 t.id AS t_id,
                 t.card_condition,
                 t.number,
                 t.product_name,
                 ROW_NUMBER() OVER (PARTITION BY f.id ORDER BY t.id ASC) AS rn
          FROM fab_prices f
          INNER JOIN tcg_p_prices t ON f.check_code_refined = t.check_code_refined
          WHERE f.tcg_p_price_id IS NULL
            AND f.id > ?
            AND t.game = 'Flesh & Blood TCG'
        ) x
        WHERE x.rn = 1
        ORDER BY x.f_id ASC
        LIMIT ?
        """;
    runFabLinkSelectUpdateLoop(selectSql, true);
  }

  @Override
  public void syncFabPriceWithTcgPriceRescanNulls() {
    String selectSql = """
        SELECT x.f_id, x.t_id, x.card_condition, x.number, x.product_name
        FROM (
          SELECT f.id AS f_id,
                 t.id AS t_id,
                 t.card_condition,
                 t.number,
                 t.product_name,
                 ROW_NUMBER() OVER (PARTITION BY f.id ORDER BY t.id ASC) AS rn
          FROM fab_prices f
          INNER JOIN tcg_p_prices t ON f.check_code_refined = t.check_code_refined
          WHERE f.tcg_p_price_id IS NULL
            AND t.game = 'Flesh & Blood TCG'
        ) x
        WHERE x.rn = 1
        ORDER BY x.f_id ASC
        LIMIT ?
        """;
    runFabLinkSelectUpdateLoop(selectSql, false);
  }

  private void runFabLinkSelectUpdateLoop(String selectSql, boolean useIdCursor) {
    String updateFabSql = "UPDATE fab_prices SET tcg_p_price_id = ?, con_num_name = ? WHERE id = ?";
    String updateTcgSql = "UPDATE tcg_p_prices SET ob_price_id = ? WHERE id = ?";

    int totalLinked = 0;
    long lastId = 0;

    while (true) {
      List<FabLinkRow> batch = useIdCursor
          ? jdbcTemplate.query(
              selectSql,
              (rs, i) -> new FabLinkRow(
                  rs.getLong(1), rs.getLong(2),
                  rs.getString(3), rs.getString(4), rs.getString(5)),
              lastId, SELECT_CHUNK)
          : jdbcTemplate.query(
              selectSql,
              (rs, i) -> new FabLinkRow(
                  rs.getLong(1), rs.getLong(2),
                  rs.getString(3), rs.getString(4), rs.getString(5)),
              SELECT_CHUNK);

      if (batch.isEmpty()) {
        break;
      }

      for (List<FabLinkRow> chunk : partition(batch, UPDATE_BATCH)) {
        transactionTemplate.executeWithoutResult(__ -> {
          jdbcTemplate.batchUpdate(updateFabSql, chunk, UPDATE_BATCH,
              (ps, row) -> {
                ps.setLong(1, row.tcgId());
                ps.setString(2, row.condition() + "-" + row.number() + "-" + row.productName());
                ps.setLong(3, row.fabId());
              });
          jdbcTemplate.batchUpdate(updateTcgSql, chunk, UPDATE_BATCH,
              (ps, row) -> {
                ps.setLong(1, row.fabId());
                ps.setLong(2, row.tcgId());
              });
        });
      }

      totalLinked += batch.size();
      if (useIdCursor) {
        lastId = batch.get(batch.size() - 1).fabId();
      }
      if (batch.size() < SELECT_CHUNK) {
        break;
      }
    }

    log.info("FAB price link sync{}: linked {} rows", useIdCursor ? "" : " (rescan-nulls)", totalLinked);
    backfillTcgObPriceIdFromFabLinks();
    if (!useIdCursor) {
      log.info("update complete (rescan-nulls)");
    } else {
      log.info("update complete");
    }
  }

  @Override
  public void backfillTcgObPriceIdFromMtgLinks() {
    String selectSql = """
        SELECT t.id, MIN(m.id)
        FROM tcg_p_prices t
        INNER JOIN mtg_prices m ON m.tcg_p_price_id = t.id
        WHERE t.game = 'Magic'
          AND (t.ob_price_id IS NULL OR t.ob_price_id <> m.id)
          AND t.id > ?
        GROUP BY t.id
        ORDER BY t.id ASC
        LIMIT ?
        """;
    int total = runSelectThenUpdate(selectSql,
        "UPDATE tcg_p_prices SET ob_price_id = ? WHERE id = ?");
    log.info("MTG ob_price_id backfill: updated {} rows", total);
  }

  @Override
  public void backfillTcgObPriceIdFromFabLinks() {
    String selectSql = """
        SELECT t.id, MIN(f.id)
        FROM tcg_p_prices t
        INNER JOIN fab_prices f ON f.tcg_p_price_id = t.id
        WHERE t.game = 'Flesh & Blood TCG'
          AND (t.ob_price_id IS NULL OR t.ob_price_id <> f.id)
          AND t.id > ?
        GROUP BY t.id
        ORDER BY t.id ASC
        LIMIT ?
        """;
    int total = runSelectThenUpdate(selectSql,
        "UPDATE tcg_p_prices SET ob_price_id = ? WHERE id = ?");
    log.info("FAB ob_price_id backfill: updated {} rows", total);
  }

  @Override
  public int runSelectThenUpdate(String selectSql, String updateSql) {
    int total = 0;
    long lastId = 0;

    while (true) {
      List<long[]> batch = jdbcTemplate.query(
          selectSql,
          (rs, i) -> new long[] { rs.getLong(1), rs.getLong(2) },
          lastId, SELECT_CHUNK);

      if (batch.isEmpty()) {
        break;
      }

      for (List<long[]> chunk : partition(batch, UPDATE_BATCH)) {
        jdbcTemplate.batchUpdate(updateSql, chunk, UPDATE_BATCH,
            (ps, row) -> {
              ps.setLong(1, row[1]);
              ps.setLong(2, row[0]);
            });
      }

      total += batch.size();
      lastId = batch.get(batch.size() - 1)[0];
      if (batch.size() < SELECT_CHUNK) {
        break;
      }
    }

    return total;
  }

  @Override
  public <T> List<List<T>> partition(List<T> list, int size) {
    java.util.List<java.util.List<T>> chunks = new java.util.ArrayList<>();
    for (int i = 0; i < list.size(); i += size) {
      chunks.add(list.subList(i, Math.min(i + size, list.size())));
    }
    return chunks;
  }

  private void performRebuildCheckCodes() {
    tcgPPriceRepository.resetCheckCodes(List.of("Magic", "Flesh & Blood TCG"));
    mtgPriceRepository.resetCheckCodes();
    fabPriceRepository.resetCheckCodes();

    List<TcgPPrice> mtgTcgPPrices = tcgPPriceRepository.findByGame("Magic");
    List<TcgPPrice> fabTcgPPrices = tcgPPriceRepository.findByGame("Flesh & Blood TCG");
    List<MtgPrice> mtgPrices = mtgPriceRepository.findAll();
    List<FabPrice> fabPrices = fabPriceRepository.findAll();

    List<CheckCodeRow> tcgMtgRows = new ArrayList<>(mtgTcgPPrices.size());
    for (TcgPPrice p : mtgTcgPPrices) {
      tcgMtgRows.add(new CheckCodeRow(p.getId(), infoAddService.buildCheckCodeMtgForTcgP(p)));
    }
    batchUpdateInitCheckCodes(SQL_INIT_TCG_CHECK_CODES, tcgMtgRows);
    log.info("TCG MTG check_code rebuild: updated {} rows", tcgMtgRows.size());

    List<CheckCodeRow> tcgFabRows = new ArrayList<>(fabTcgPPrices.size());
    for (TcgPPrice p : fabTcgPPrices) {
      tcgFabRows.add(new CheckCodeRow(p.getId(), infoAddService.buildCheckCodeFabForTcgP(p)));
    }
    batchUpdateInitCheckCodes(SQL_INIT_TCG_CHECK_CODES, tcgFabRows);
    log.info("TCG FAB check_code rebuild: updated {} rows", tcgFabRows.size());

    List<CheckCodeRow> mtgRows = new ArrayList<>(mtgPrices.size());
    for (MtgPrice p : mtgPrices) {
      mtgRows.add(new CheckCodeRow(p.getId(), infoAddService.buildCheckCodeMtgForMtgPrice(p)));
    }
    batchUpdateInitCheckCodes(SQL_INIT_MTG_CHECK_CODES, mtgRows);
    log.info("MTG check_code rebuild: updated {} rows", mtgRows.size());

    List<CheckCodeRow> fabRows = new ArrayList<>(fabPrices.size());
    for (FabPrice p : fabPrices) {
      fabRows.add(new CheckCodeRow(p.getId(), infoAddService.buildCheckCodeFabForFabPrice(p)));
    }
    batchUpdateInitCheckCodes(SQL_INIT_FAB_CHECK_CODES, fabRows);
    log.info("FAB check_code rebuild: updated {} rows", fabRows.size());

    logPriceLinkDiagnostics("after_check_code_rebuild");
  }

  /**
   * NULL 초기화 없이 check_code(및 조건에 따라 refined)만 재계산한다.
   * {@code check_code}와 {@code check_code_refined}가 같으면 둘 다 새 값으로 맞추고,
   * 다르면 refined는 유지하고 check_code만 갱신한다.
   */
  private void performRebuildCheckCodesSelective() {
    List<MtgPrice> mtgPrices = mtgPriceRepository.findAll();
    List<FabPrice> fabPrices = fabPriceRepository.findAll();

    for (String game : SYNC_GAMES) {
      List<TcgPPrice> tcgPPrices = tcgPPriceRepository.findByGame(game);
      List<CheckCodeRow> tcgBoth = new ArrayList<>();
      List<CheckCodeRow> tcgCodeOnly = new ArrayList<>();
      for (TcgPPrice p : tcgPPrices) {
        String built = buildCheckCodeForTcgPByGame(p, game);
        if (built == null || built.isBlank()) {
          continue;
        }
        if (Objects.equals(p.getCheckCode(), p.getCheckCodeRefined())) {
          tcgBoth.add(new CheckCodeRow(p.getId(), built));
        } else {
          tcgCodeOnly.add(new CheckCodeRow(p.getId(), built));
        }
      }
      batchUpdateInitCheckCodes(SQL_INIT_TCG_CHECK_CODES, tcgBoth);
      batchUpdateCheckCodeOnly(SQL_TCG_CHECK_CODE_ONLY, tcgCodeOnly);
      log.info("TCG {} check_code selective rebuild: both={}, check_code_only={}",
          game, tcgBoth.size(), tcgCodeOnly.size());
    }

    List<CheckCodeRow> mtgBoth = new ArrayList<>();
    List<CheckCodeRow> mtgCodeOnly = new ArrayList<>();
    for (MtgPrice p : mtgPrices) {
      String built = infoAddService.buildCheckCodeMtgForMtgPrice(p);
      if (Objects.equals(p.getCheckCode(), p.getCheckCodeRefined())) {
        mtgBoth.add(new CheckCodeRow(p.getId(), built));
      } else {
        mtgCodeOnly.add(new CheckCodeRow(p.getId(), built));
      }
    }
    batchUpdateInitCheckCodes(SQL_INIT_MTG_CHECK_CODES, mtgBoth);
    batchUpdateCheckCodeOnly(SQL_MTG_CHECK_CODE_ONLY, mtgCodeOnly);
    log.info("MTG check_code selective rebuild: both={}, check_code_only={}", mtgBoth.size(), mtgCodeOnly.size());

    List<CheckCodeRow> fabBoth = new ArrayList<>();
    List<CheckCodeRow> fabCodeOnly = new ArrayList<>();
    for (FabPrice p : fabPrices) {
      String built = infoAddService.buildCheckCodeFabForFabPrice(p);
      if (Objects.equals(p.getCheckCode(), p.getCheckCodeRefined())) {
        fabBoth.add(new CheckCodeRow(p.getId(), built));
      } else {
        fabCodeOnly.add(new CheckCodeRow(p.getId(), built));
      }
    }
    batchUpdateInitCheckCodes(SQL_INIT_FAB_CHECK_CODES, fabBoth);
    batchUpdateCheckCodeOnly(SQL_FAB_CHECK_CODE_ONLY, fabCodeOnly);
    log.info("FAB check_code selective rebuild: both={}, check_code_only={}", fabBoth.size(), fabCodeOnly.size());

    logPriceLinkDiagnostics("after_check_code_selective_rebuild");
  }

  private void performRebuildLinks() {
    tcgPPriceRepository.resetLinks(SYNC_GAMES);
    mtgPriceRepository.resetLinks();
    fabPriceRepository.resetLinks();
    syncFabPriceWithTcgPrice();
    syncMtgPriceWithTcgPrice();
    logPriceLinkDiagnostics("after_link_rebuild");
  }

  private String buildCheckCodeForTcgPByGame(TcgPPrice price, String game) {
    if (price == null || game == null) {
      return null;
    }
    return switch (game) {
      case "Magic" -> infoAddService.buildCheckCodeMtgForTcgP(price);
      case "Flesh & Blood TCG" -> infoAddService.buildCheckCodeFabForTcgP(price);
      case "Star Wars Unlimited" -> infoAddService.buildCheckCodeForSWU(price);
      case "Lorcana TCG" -> infoAddService.buildCheckCodeForLorcana(price);
      case "Riftbound League of Legends Trading Card Game" ->
        infoAddService.buildCheckCodeForRift(price);
      default -> null;
    };
  }

  private void refreshCodeNumbersForSpecialGames() {
    for (String game : CODE_NUMBER_REFRESH_GAMES) {
      List<TcgPPrice> prices = tcgPPriceRepository.findByGame(game);
      List<TcgPPrice> changed = new ArrayList<>();
      for (TcgPPrice price : prices) {
        String latestCodeNumber = infoAddService.generateCodeNumber(price);
        if (!Objects.equals(price.getCodeNumber(), latestCodeNumber)) {
          price.setCodeNumber(latestCodeNumber);
          changed.add(price);
        }
      }
      if (!changed.isEmpty()) {
        tcgPPriceRepository.saveAll(changed);
      }
      log.info("TCG {} codeNumber refresh: scanned={}, updated={}", game, prices.size(), changed.size());
    }
  }

  private void logPriceLinkDiagnostics(String phase) {
    try {
      Long mtgUnlinked = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM mtg_prices WHERE tcg_p_price_id IS NULL", Long.class);
      Long mtgJoinable = jdbcTemplate.queryForObject("""
          SELECT COUNT(*) FROM mtg_prices m
          WHERE m.tcg_p_price_id IS NULL
            AND m.check_code_refined IS NOT NULL
            AND EXISTS (
              SELECT 1 FROM tcg_p_prices t
              WHERE t.check_code_refined = m.check_code_refined
                AND t.game = 'Magic'
                AND t.card_condition NOT LIKE '%-%')
          """, Long.class);
      Long fabUnlinked = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM fab_prices WHERE tcg_p_price_id IS NULL", Long.class);
      Long fabJoinable = jdbcTemplate.queryForObject("""
          SELECT COUNT(*) FROM fab_prices f
          WHERE f.tcg_p_price_id IS NULL
            AND f.check_code_refined IS NOT NULL
            AND EXISTS (
              SELECT 1 FROM tcg_p_prices t
              WHERE t.check_code_refined = f.check_code_refined
                AND t.game = 'Flesh & Blood TCG')
          """, Long.class);
      log.info(
          "Price link diagnostics [{}]: mtg_unlinked={}, mtg_joinable_by_refined={}, fab_unlinked={}, fab_joinable_by_refined={}",
          phase, mtgUnlinked, mtgJoinable, fabUnlinked, fabJoinable);
    } catch (Exception e) {
      log.warn("Price link diagnostics [{}] skipped: {}", phase, e.getMessage());
    }
  }

  @Override
  public boolean startRebuildCheckCodes() {
    if (!syncPriceLinkRunning.compareAndSet(false, true)) {
      log.warn("Price link job is already running. Skip startRebuildCheckCodes.");
      return false;
    }
    LocalDateTime startTime = LocalDateTime.now();
    CompletableFuture.runAsync(() -> {
      String result = "failure";
      String message = "";
      try {
        performRebuildCheckCodes();
        result = "success";
      } catch (Exception e) {
        log.error("check_code rebuild failed", e);
        message = e.getMessage() != null && !e.getMessage().isBlank()
            ? e.getMessage()
            : e.getClass().getSimpleName();
      } finally {
        syncPriceLinkRunning.set(false);
        eventPublisher.publishEvent(new SyncLogEvent(
            PRICE_LINK_CHECK_CODE_REBUILD_LOG, PRICE_LINK_CHECK_CODE_REBUILD_LOG,
            startTime, LocalDateTime.now(), result, message));
      }
    });
    return true;
  }

  @Override
  public boolean startRebuildCheckCodesSelective() {
    if (!syncPriceLinkRunning.compareAndSet(false, true)) {
      log.warn("Price link job is already running. Skip startRebuildCheckCodesSelective.");
      return false;
    }
    LocalDateTime startTime = LocalDateTime.now();
    CompletableFuture.runAsync(() -> {
      String result = "failure";
      String message = "";
      try {
        refreshCodeNumbersForSpecialGames();
        performRebuildCheckCodesSelective();
        result = "success";
      } catch (Exception e) {
        log.error("check_code selective rebuild failed", e);
        message = e.getMessage() != null && !e.getMessage().isBlank()
            ? e.getMessage()
            : e.getClass().getSimpleName();
      } finally {
        syncPriceLinkRunning.set(false);
        eventPublisher.publishEvent(new SyncLogEvent(
            PRICE_LINK_CHECK_CODE_SELECTIVE_REBUILD_LOG, PRICE_LINK_CHECK_CODE_SELECTIVE_REBUILD_LOG,
            startTime, LocalDateTime.now(), result, message));
      }
    });
    return true;
  }

  @Override
  public boolean startRebuildLinks() {
    if (!syncPriceLinkRunning.compareAndSet(false, true)) {
      log.warn("Price link job is already running. Skip startRebuildLinks.");
      return false;
    }
    LocalDateTime startTime = LocalDateTime.now();
    CompletableFuture.runAsync(() -> {
      String result = "failure";
      String message = "";
      try {
        performRebuildLinks();
        result = "success";
      } catch (Exception e) {
        log.error("Link rebuild failed", e);
        message = e.getMessage() != null && !e.getMessage().isBlank()
            ? e.getMessage()
            : e.getClass().getSimpleName();
      } finally {
        syncPriceLinkRunning.set(false);
        eventPublisher.publishEvent(new SyncLogEvent(
            PRICE_LINK_REBUILD_LOG, PRICE_LINK_REBUILD_LOG,
            startTime, LocalDateTime.now(), result, message));
      }
    });
    return true;
  }

  @Override
  public boolean startFullRebuild() {
    if (!syncPriceLinkRunning.compareAndSet(false, true)) {
      log.warn("Price link job is already running. Skip startFullRebuild.");
      return false;
    }
    LocalDateTime startTime = LocalDateTime.now();
    CompletableFuture.runAsync(() -> {
      String result = "failure";
      String message = "";
      try {
        performRebuildCheckCodes();
        performRebuildLinks();
        result = "success";
      } catch (Exception e) {
        log.error("Full rebuild failed", e);
        message = e.getMessage() != null && !e.getMessage().isBlank()
            ? e.getMessage()
            : e.getClass().getSimpleName();
      } finally {
        syncPriceLinkRunning.set(false);
        eventPublisher.publishEvent(new SyncLogEvent(
            PRICE_LINK_FULL_REBUILD_LOG, PRICE_LINK_FULL_REBUILD_LOG,
            startTime, LocalDateTime.now(), result, message));
      }
    });
    return true;
  }
}
