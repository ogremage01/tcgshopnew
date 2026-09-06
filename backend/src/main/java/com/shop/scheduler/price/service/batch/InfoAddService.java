package com.shop.scheduler.price.service.batch;

import com.shop.card.entity.FabPrice;
import com.shop.card.entity.MtgPrice;
import com.shop.card.entity.TcgPPrice;

import java.util.Set;

public interface InfoAddService {

    // 카드 정보 추가 서비스

    // 비영어 카드 등 매칭 불가 시 `buildCheckCode*`가 반환하는 플레이스홀더.
    String NON_ENGLISH_FAILURE = "nonenglishfailure";

    /**
     * 카드 번호 / 부가 정보를 생성한다.
     * 
     * @param game     게임
     * @param number   카드 번호
     * @param setAbbrv 세트 약어
     * @return 카드 번호 / 부가 정보
     */
    Set<String> generateCodeNumbers(String game, String number, String setAbbrv);

    /**
     * 스타워즈 카드 번호 / 부가 정보를 생성한다.
     * 
     * @param number   카드 번호
     * @param setAbbrv 세트 약어
     * @return 카드 번호 / 부가 정보
     */
    Set<String> generateCodeNumbersStarWars(String number, String setAbbrv);

    /**
     * 로카나 카드 번호 / 부가 정보를 생성한다.
     * 
     * @param number   카드 번호
     * @param setAbbrv 세트 약어
     * @return 카드 번호 / 부가 정보
     */
    Set<String> generateCodeNumbersLorcana(String number, String setAbbrv);

    /**
     * 리프트바운드 카드 번호 / 부가 정보를 생성한다.
     * 
     * @param number   카드 번호
     * @param setAbbrv 세트 약어
     * @return 카드 번호 / 부가 정보
     */
    Set<String> generateCodeNumbersRiftbound(String number, String setAbbrv);

    /**
     * 플레시 앤 블러드 카드 번호 / 부가 정보를 생성한다.
     * 
     * @param number 카드 번호
     * @return 카드 번호 / 부가 정보
     */
    Set<String> generateCodeNumbersFleshAndBlood(String number);

    /**
     * 이중면 카드 여부를 확인한다.
     * 
     * @param game   게임
     * @param name   카드 이름
     * @param number 카드 번호
     * @return 이중면 카드 여부
     */
    Boolean isDoubleSided(String game, String name, String number);

    /**
     * 프린팅 타입을 가져온다.
     * 
     * @param printing 프린팅 타입
     * @return 프린팅 타입
     */
    String getPrintType(String printing);

    /**
     * FAB foil/finish 값을 check_code·매칭용으로 정규화한다. Non-Foil 계열은 Normal.
     *
     * @param foil FAB foil/finish 원문
     * @return 정규화된 foil 값
     */
    String normalizeFabFoil(String foil);

    /**
     * TcgPPrice에 카드 번호 / 부가 정보를 생성한다.
     * 
     * @param tcgPPrice TcgPPrice
     * @return 카드 번호 / 부가 정보
     */
    String generateCodeNumber(TcgPPrice tcgPPrice);

    // ── 가격 소스 간 매칭 키 생성 ───────────────────────────────────────────

    /**
     * MTG: (세트코드 + 카드번호 + 프린팅타입) 로 만드는 매칭 키. TcgPPrice에 사용
     * 
     * @param tcgPrice TcgPPrice
     * @return 매칭 키
     */
    String buildCheckCodeMtgForTcgP(TcgPPrice tcgPrice);

    /**
     * Fab용 메서드 TcgP에 사용
     * 
     * @param tcgPrice TcgPPrice
     * @return 매칭 키
     */
    String buildCheckCodeFabForTcgP(TcgPPrice tcgPrice);

    /**
     * Fab용 메서드 FabPrice에 사용
     * 
     * @param fabPrice FabPrice
     * @return 매칭 키
     */
    String buildCheckCodeFabForFabPrice(FabPrice fabPrice);

    /**
     * MTG용 메서드 MtgPrice에 사용
     * 
     * @param mtgPrice MtgPrice
     * @return 매칭 키
     */
    String buildCheckCodeMtgForMtgPrice(MtgPrice mtgPrice);

    // ---------SWU,Lorcana,Riftbound 용 tcgp price check code
    // 생성───────────────────────────────────────────
    /**
     * SWU 용 tcgp price check code 생성
     * 
     * @param tcgPPrice TcgPPrice
     * @return 매칭 키
     */
    String buildCheckCodeForSWU(TcgPPrice tcgPPrice);

    /**
     * Lorcana 용 tcgp price check code 생성
     * 
     * @param tcgPPrice TcgPPrice
     * @return 매칭 키
     */
    String buildCheckCodeForLorcana(TcgPPrice tcgPPrice);

    /**
     * Riftbound 용 tcgp price check code 생성
     * 
     * @param tcgPPrice TcgPPrice
     * @return 매칭 키
     */
    String buildCheckCodeForRift(TcgPPrice tcgPPrice);

}
