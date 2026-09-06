package com.shop.scheduler.price.service.batch;

import java.util.Set;

import org.springframework.stereotype.Service;

import com.shop.card.entity.FabPrice;
import com.shop.card.entity.MtgPrice;
import com.shop.card.entity.TcgPPrice;

import lombok.extern.slf4j.Slf4j;

/**
 * 포트폴리오 공개본 스텁. 카드 번호·check_code 생성 규칙은 포함하지 않습니다.
 */
@Slf4j
@Service
public class InfoAddServiceImpl implements InfoAddService {

    @Override
    public Set<String> generateCodeNumbers(String game, String number, String setAbbrv) {
        return Set.of();
    }

    @Override
    public Set<String> generateCodeNumbersStarWars(String number, String setAbbrv) {
        return Set.of();
    }

    @Override
    public Set<String> generateCodeNumbersLorcana(String number, String setAbbrv) {
        return Set.of();
    }

    @Override
    public Set<String> generateCodeNumbersRiftbound(String number, String setAbbrv) {
        return Set.of();
    }

    @Override
    public Set<String> generateCodeNumbersFleshAndBlood(String number) {
        return Set.of();
    }

    @Override
    public Boolean isDoubleSided(String game, String name, String number) {
        return false;
    }

    @Override
    public String getPrintType(String printing) {
        return "";
    }

    @Override
    public String normalizeFabFoil(String foil) {
        return foil;
    }

    @Override
    public String generateCodeNumber(TcgPPrice tcgPPrice) {
        return "";
    }

    @Override
    public String buildCheckCodeMtgForTcgP(TcgPPrice tcgPrice) {
        return omitted();
    }

    @Override
    public String buildCheckCodeFabForTcgP(TcgPPrice tcgPrice) {
        return omitted();
    }

    @Override
    public String buildCheckCodeFabForFabPrice(FabPrice fabPrice) {
        return omitted();
    }

    @Override
    public String buildCheckCodeMtgForMtgPrice(MtgPrice mtgPrice) {
        return omitted();
    }

    @Override
    public String buildCheckCodeForSWU(TcgPPrice tcgPPrice) {
        return omitted();
    }

    @Override
    public String buildCheckCodeForLorcana(TcgPPrice tcgPPrice) {
        return omitted();
    }

    @Override
    public String buildCheckCodeForRift(TcgPPrice tcgPPrice) {
        return omitted();
    }

    private static String omitted() {
        log.warn("Portfolio snapshot: check-code generation is omitted.");
        return "";
    }
}
