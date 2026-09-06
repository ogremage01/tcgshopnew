package com.shop.scheduler.price.source.openbinder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.shop.card.entity.FabPrice;
import com.shop.card.entity.MtgPrice;
import com.shop.card.metadata.entity.FabSetInfo;
import com.shop.card.metadata.entity.MtgSetInfo;
import com.shop.card.metadata.repository.FabSetInfoRepository;
import com.shop.card.metadata.repository.MtgSetInfoRepository;
import com.shop.scheduler.price.dto.FabSetInfoRawDto;
import com.shop.scheduler.price.dto.MtgSetInfoRawDto;
import com.shop.scheduler.price.service.batch.InfoAddService;
import com.shop.scheduler.price.service.batch.PriceBatchSaver;
import com.shop.scheduler.price.service.link.PriceLinkServiceImpl;
import com.shop.scheduler.price.source.client.PriceFetchClient;
import com.shop.scheduler.price.source.openbinder.parser.ObPriceParser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenBinderSyncServiceImpl implements OpenBinderSyncService {

    private final PriceFetchClient fetchClient;
    private final PriceBatchSaver priceBatchSaver;
    private final OpenBinderMarketPriceOverlayService overlayService;
    private final InfoAddService infoAddService;
    private final PriceLinkServiceImpl priceLinkService;
    private final ObPriceParser obPriceParser = new ObPriceParser();
    private final MtgSetInfoRepository mtgSetInfoRepository;
    private final FabSetInfoRepository fabSetInfoRepository;

    @Override
    public OpenBinderSyncResult syncAll() {
        String fabResult = syncFabPrices();
        String mtgResult = syncMtgPrices();
        String mtgSetInfoResult = syncMtgSetInfo();
        String fabSetInfoResult = syncFabSetInfo();
        String message = buildStepMessage(fabResult, mtgResult, mtgSetInfoResult, fabSetInfoResult);

        priceLinkService.startSyncPriceLink();
        overlayService.applyMtgMarketPricesToTcgP();
        overlayService.applyFabMarketPricesToTcgP();

        String aggregate = aggregateResult(fabResult, mtgResult, mtgSetInfoResult, fabSetInfoResult);
        if (!"success".equals(aggregate)) {
            log.warn("Open Binder sync finished with {}. {}", aggregate, message);
        }
        return new OpenBinderSyncResult(aggregate, message);
    }

    @Override
    public String syncMtgPrices() {
        try {
            List<MtgPrice> rows = obPriceParser.parseMtgPrices(fetchClient.fetchMtgPriceRows());
            if (rows == null || rows.isEmpty()) {
                return failure("empty_response");
            }
            rows.forEach(row -> {
                row.setId(null);
                row.setTcgPPriceId(null);
                String checkCode = infoAddService.buildCheckCodeMtgForMtgPrice(row);
                if (checkCode != null) {
                    row.setCheckCode(checkCode);
                    row.setCheckCodeRefined(checkCode);
                }
            });
            priceBatchSaver.saveMtgBatch(rows);
            return success("rows=" + rows.size());
        } catch (WebClientResponseException e) {
            return failureHttp(e);
        } catch (Exception e) {
            log.error("MTG price sync failed", e);
            return failureException(e);
        }
    }

    @Override
    public String syncFabPrices() {
        try {
            List<FabPrice> rows = obPriceParser.parseFabPrices(fetchClient.fetchFabPriceRows());
            if (rows == null || rows.isEmpty()) {
                return failure("empty_response");
            }
            rows.forEach(row -> {
                row.setId(null);
                row.setTcgPPriceId(null);
                row.setFoil(infoAddService.normalizeFabFoil(row.getFoil()));
                row.setCheckCode(infoAddService.buildCheckCodeFabForFabPrice(row));
                row.setCheckCodeRefined(row.getCheckCode());
            });
            priceBatchSaver.saveFabBatch(rows);
            return success("rows=" + rows.size());
        } catch (WebClientResponseException e) {
            return failureHttp(e);
        } catch (Exception e) {
            log.error("FAB price sync failed", e);
            return failureException(e);
        }
    }

    @Override
    public String syncMtgSetInfo() {
        List<MtgSetInfo> savingRows = new ArrayList<>();
        Set<String> seenSetCodes = new HashSet<>();
        int fetched = 0;
        try {
            List<MtgSetInfoRawDto> rows = fetchClient.fetchMtgSetInfo();
            if (rows == null || rows.isEmpty()) {
                return failure("empty_response");
            }
            fetched = rows.size();
            for (MtgSetInfoRawDto row : rows) {
                if (row == null || isBlankSetCode(row.getSetCode()) || parseReleaseDate(row.getReleaseDate()) == null) {
                    continue;
                }
                if (!seenSetCodes.add(row.getSetCode()) || mtgSetInfoExists(row.getSetCode())) {
                    continue;
                }
                MtgSetInfo mtgSetInfo = new MtgSetInfo();
                mtgSetInfo.setSetCode(row.getSetCode());
                mtgSetInfo.setName(row.getName());
                mtgSetInfo.setNameK(row.getNameK());
                mtgSetInfo.setReleaseDate(parseReleaseDate(row.getReleaseDate()));
                mtgSetInfo.setType(row.getType());
                mtgSetInfo.setCreatedAt(LocalDateTime.now());
                savingRows.add(mtgSetInfo);
            }
        } catch (WebClientResponseException e) {
            return failureHttp(e);
        } catch (Exception e) {
            log.error("MTG set info sync failed", e);
            return failureException(e);
        }
        mtgSetInfoRepository.saveAll(savingRows);
        return success("inserted=" + savingRows.size() + ",fetched=" + fetched);
    }

    @Override
    public String syncFabSetInfo() {
        List<FabSetInfo> savingRows = new ArrayList<>();
        Set<String> seenSetCodes = new HashSet<>();
        int fetched = 0;
        int inserted = 0;
        int updated = 0;
        try {
            List<FabSetInfoRawDto> rows = fetchClient.fetchFabSetInfo();
            if (rows == null || rows.isEmpty()) {
                return failure("empty_response");
            }
            fetched = rows.size();
            for (FabSetInfoRawDto row : rows) {
                if (row == null || isBlankSetCode(row.getSetCode())) {
                    continue;
                }
                if (!seenSetCodes.add(row.getSetCode())) {
                    continue;
                }
                if (row.getName().isEmpty()) {
                    continue;
                }
                Optional<FabSetInfo> existing = fabSetInfoRepository.findBySetCode(row.getSetCode());
                FabSetInfo fabSetInfo;
                if (existing.isPresent()) {
                    fabSetInfo = existing.get();
                    updated++;
                } else {
                    fabSetInfo = new FabSetInfo();
                    fabSetInfo.setSetCode(row.getSetCode());
                    inserted++;
                }
                fabSetInfo.setName(row.getName());
                fabSetInfo.setPorder(row.getPorder());
                fabSetInfo.setCategory(row.getCategory());
                savingRows.add(fabSetInfo);
            }
        } catch (WebClientResponseException e) {
            return failureHttp(e);
        } catch (Exception e) {
            log.error("FAB set info sync failed", e);
            return failureException(e);
        }
        fabSetInfoRepository.saveAll(savingRows);
        return success("inserted=" + inserted + ",updated=" + updated + ",fetched=" + fetched);
    }

    private static String buildStepMessage(String fabResult, String mtgResult, String mtgSetInfoResult,
            String fabSetInfoResult) {
        return "fabPrices=" + fabResult
                + ", mtgPrices=" + mtgResult
                + ", mtgSetInfo=" + mtgSetInfoResult
                + ", fabSetInfo=" + fabSetInfoResult;
    }

    private static String aggregateResult(String... stepResults) {
        long failureCount = Stream.of(stepResults).filter(OpenBinderSyncServiceImpl::isFailure).count();
        if (failureCount == stepResults.length) {
            return "failure";
        }
        if (failureCount > 0) {
            return "partial_success";
        }
        return "success";
    }

    private static boolean isFailure(String result) {
        return result != null && result.startsWith("failure");
    }

    private static String success(String detail) {
        return "success:" + detail;
    }

    private static String failure(String reason) {
        return "failure:" + reason;
    }

    private static String failureHttp(WebClientResponseException e) {
        return failure("http_" + e.getStatusCode().value());
    }

    private static String failureException(Exception e) {
        String type = e.getClass().getSimpleName();
        String msg = e.getMessage();
        if (msg != null && !msg.isBlank()) {
            String trimmed = msg.length() > 120 ? msg.substring(0, 120) : msg;
            return failure(type + ":" + trimmed.replace(',', ';'));
        }
        return failure(type);
    }

    private static boolean isBlankSetCode(String setCode) {
        return setCode == null || setCode.isBlank();
    }

    private LocalDateTime parseReleaseDate(String releaseDate) {
        if (releaseDate == null || releaseDate.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(releaseDate, DateTimeFormatter.ofPattern("yyyyMMdd")).atStartOfDay();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private Boolean mtgSetInfoExists(String setCode) {
        return mtgSetInfoRepository.existsBySetCode(setCode);
    }

}
