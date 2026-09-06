package com.shop.scheduler.price.service.batch;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.shop.card.entity.FabPrice;
import com.shop.card.entity.MtgPrice;
import com.shop.card.entity.TcgPPrice;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InfoAddServiceImpl implements InfoAddService {

    /** 파싱·코드 생성 조건 불충족 시 저장·매칭용 플레이스홀더(동기화가 예외로 중단되지 않도록). */
    private static final String PARSING_FAILURE = "parsingfailure";

    private static final String FAB_GAME_NAME = "fabtcg";

    private final String CROSS_SYMBOL = "†";

    // ── 문자열 처리 ──────────────────────────────────────────────────────────
    private static String nf(String s) {
        return s == null ? "" : s;
    }

    private static Set<String> singleParsingFailureSet() {
        Set<String> s = new HashSet<>(1);
        s.add(PARSING_FAILURE);
        return s;
    }

    // ── 카드 번호 / 부가 정보 생성 ──────────────────────────────────────────

    @Override
    public Set<String> generateCodeNumbers(String game, String number, String setAbbrv) {
        if (game == null || game.isBlank()) {
            return singleParsingFailureSet();
        }
        return switch (game) {
            case "Star Wars Unlimited" -> generateCodeNumbersStarWars(number, setAbbrv);
            case "Lorcana TCG" -> generateCodeNumbersLorcana(number, setAbbrv);
            case "Riftbound League of Legends Trading Card Game" -> generateCodeNumbersRiftbound(number, setAbbrv);
            case "Flesh & Blood TCG" -> generateCodeNumbersFleshAndBlood(number);
            default -> singleParsingFailureSet();
        };
    }

    @Override
    public Set<String> generateCodeNumbersStarWars(String number, String setAbbrv) {
        Set<String> result = new HashSet<>();
        String set = nf(setAbbrv);
        if (number == null || number.isBlank()) {
            return singleParsingFailureSet();
        } else if (!number.contains("/")) {
            result.add(set + "-" + number.strip());
        } else if (number.contains("//")) {
            String[] numbers = number.split("//");
            for (String num : numbers) {
                String part = nf(num).split("/")[0].strip();
                if (!part.isEmpty()) {
                    result.add(set + "-" + part);
                }
            }
            if (result.isEmpty()) {
                return singleParsingFailureSet();
            }
        } else if (number.contains("/")) {
            String[] numbers = number.split("/");
            for (String num : numbers) {
                String stripped = nf(num).strip();
                if (!stripped.isEmpty()) {
                    result.add(set + "-" + stripped);
                }
            }
            if (result.isEmpty()) {
                return singleParsingFailureSet();
            }
        }
        return result;
    }

    @Override
    public Set<String> generateCodeNumbersLorcana(String number, String setAbbrv) {
        Set<String> result = new HashSet<>();
        String set = nf(setAbbrv);
        if (number == null || number.isBlank()) {
            return singleParsingFailureSet();
        } else if (!number.contains("/")) {
            result.add(set + "-" + number.strip());
        } else if (number.contains("/")) {
            String head = number.split("/")[0];
            result.add(set + "-" + nf(head).strip());
        }
        return result;
    }

    @Override
    public Set<String> generateCodeNumbersRiftbound(String number, String setAbbrv) {
        Set<String> result = new HashSet<>();
        String set = nf(setAbbrv);
        if (number == null || number.isBlank()) {
            return singleParsingFailureSet();
        } else if (!number.contains("/")) {
            result.add(set + "-" + number.strip());
        } else if (number.contains("/")) {
            String head = number.split("/")[0];
            result.add(set + "-" + nf(head).strip());
        }
        return result;
    }

    @Override
    public Set<String> generateCodeNumbersFleshAndBlood(String number) {
        Set<String> result = new HashSet<>();
        if (number == null || number.isBlank()) {
            return singleParsingFailureSet();
        } else if (!number.contains("/")) {
            String stripped = number.strip();
            String letters = stripped.replaceAll("[0-9]", "");
            String digits = stripped.replaceAll("[^0-9]", "");
            if (letters.isEmpty() && digits.isEmpty()) {
                return singleParsingFailureSet();
            }
            result.add(letters + "-" + digits);
            return result;
        } else if (number.contains("//")) {
            String[] numbers = number.split("//");
            for (String num : numbers) {
                String n = nf(num).strip();
                String letters = n.replaceAll("[0-9]", "").strip();
                String digits = n.replaceAll("[^0-9]", "").strip();
                if (letters.isEmpty() && digits.isEmpty()) {
                    continue;
                }
                result.add(letters + "-" + digits);
            }
            if (result.isEmpty()) {
                return singleParsingFailureSet();
            }
            return result;
        } else if (number.contains("/")) {
            String stripped = number.strip();
            String letters = stripped.replaceAll("[0-9]", "").strip();
            String digits = stripped.replaceAll("[^0-9]", "").strip();
            if (letters.isEmpty() && digits.isEmpty()) {
                return singleParsingFailureSet();
            }
            result.add(letters + "-" + digits);
            return result;
        }
        return singleParsingFailureSet();
    }

    @Override
    public Boolean isDoubleSided(String game, String name, String number) {
        String n = nf(number);
        String na = nf(name);
        switch (nf(game)) {
            case "Star Wars Unlimited":
                return na.contains("//");
            case "Lorcana TCG":
                return na.contains("//");
            case "Riftbound League of Legends Trading Card Game":
                return na.contains("//");
            case "Flesh & Blood TCG":
                if (n.contains("/")) {
                    return true;
                }
                return null;
            default:
                return false;
        }
    }

    @Override
    public String getPrintType(String printing) {
        if (printing == null || printing.isBlank()) {
            return "Unknown";
        }
        if (isNonFoil(printing)) {
            return "Normal";
        }
        String upperPrinting = printing.toUpperCase();
        if (upperPrinting.contains("FOIL")) {
            return "Foil";
        }
        if (upperPrinting.contains("NORMAL")) {
            return "Normal";
        }
        return "Unknown";
    }

    @Override
    public String normalizeFabFoil(String foil) {
        if (isNonFoil(foil)) {
            return "Normal";
        }
        return nf(foil);
    }

    private static boolean isNonFoil(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String upper = value.toUpperCase();
        return upper.contains("NON-FOIL") || upper.contains("NON FOIL");
    }

    @Override
    public String generateCodeNumber(TcgPPrice tcgPPrice) {
        if (tcgPPrice == null) {
            return PARSING_FAILURE;
        }
        String game = tcgPPrice.getGame();
        String number = tcgPPrice.getNumber();
        String setAbbrv = tcgPPrice.getSetAbbrv();
        String num = nf(number);
        String set = nf(setAbbrv);
        switch (nf(game)) {
            case "Flesh & Blood TCG":
                if (num.length() < 3) {
                    return PARSING_FAILURE;
                }
                return num.substring(0, 3) + "-" + num.substring(3, 6);
            default:
                if ("Sealed Products".equals(tcgPPrice.getType())) {
                    return set + '-' + nf(tcgPPrice.getProductName());
                }
                if (num.isBlank()) {
                    return PARSING_FAILURE;
                }
                return set + "-" + num;
        }
    }

    // ── 가격 소스 간 매칭 키 생성 ───────────────────────────────────────────

    /**
     * MTG: (세트코드 + 카드번호 + 프린팅타입) 로 만드는 매칭 키.
     */
    @Override
    public String buildCheckCodeMtgForTcgP(TcgPPrice tcgPrice) {
        if (tcgPrice == null) {
            return PARSING_FAILURE;
        }
        if ("Sealed Products".equals(tcgPrice.getType())) {
            return nf(tcgPrice.getSetAbbrv()) + "-" + nf(tcgPrice.getProductName());
        }

        String setAbbrv = nf(tcgPrice.getSetAbbrv());
        String setName = nf(tcgPrice.getSet());
        String productName = nf(tcgPrice.getProductName());
        String rarity = nf(tcgPrice.getRarity());
        String condition = nf(tcgPrice.getCondition());
        String tcgSet = nf(tcgPrice.getSet());

        String numb = nf(tcgPrice.getNumber()).replaceAll(" // ", "_");

        // Pattern pattern = Pattern.compile("([A-Z])");
        // Matcher matcher = pattern.matcher(tcgPrice.getProductName());
        // if (matcher.find()) {
        // numb = numb + matcher.group(1).toLowerCase();
        // }

        // 1) 세트 코드 계산
        String set = "";
        if (setAbbrv.length() <= 3) {
            set = setAbbrv;
        } else if (setAbbrv.length() == 5 && setAbbrv.startsWith("PP")) {
            set = setAbbrv.replaceFirst("PP", "P");
            numb = numb + "p";
        } else if (setAbbrv.equals("AFR&")) {
            set = "AFR&";
        } else if (setAbbrv.equals("FRF_UGIN")) {
            set = "FRF_UGIN";
        } else if (setAbbrv.equals("MPS2")) {
            set = "MPS2";
        } else if (setName.equals("Magic Premiere Shop")) {
            set = "MPS1";
        } else {
            set = setAbbrv.substring(0, 3);
        }

        // 2) 기본 속성 suffix
        String printing = nf(tcgPrice.getPrinting());
        // 토큰 구분 여부
        String token = rarity.contains("Token") ? "-token" : "";
        // 언어
        String language = condition.contains("-") ? ("-" + condition.split(" - ", 2)[1]) : "";
        if (setAbbrv.contains("WAR") && productName.contains("(JP Alternate Art)")) {
            language = "-JP Alternate Art";
        }
        // 아트 시리즈
        String artSeries = tcgSet.contains("(Art Series)")
                ? ("-" + tcgSet.split("\\(Art Series\\)", 2)[1])
                : "";
        // 스페셜
        String special = rarity.contains("Special") && (!setAbbrv.contains("TSR")) && (!setAbbrv.contains("TSB"))
                ? "(Special)"
                : "";
        // 시리얼 넘버
        String serialNumber = productName.contains("(Serial Numbered)") ? "-(Serial Numbered)-" : "";

        // 3) 예외/특수 케이스 suffix
        // 디스플레이 커맨더
        String displayCommander = productName.contains("(Display Commander)") ? "-(Display Commander)"
                : "";
        // 듀얼덱
        String displayName = "";
        if (tcgSet.contains("Duel Decks")) {
            displayName = ("-" + productName + "-");
        }
        // 미스 프린트
        String misPrint = productName.contains("(Misprint)") ? "-(Misprint)" : "";
        // 홈랜드 버전2
        String hmlVer2 = productName.contains(" [Version 2]") ? " -[Version 2]" : "";
        // MH1 Retro Frame
        String mhRetroFrame = productName.contains("Retro Frame") ? "-(Retro Frame)" : "";
        // MH1 Foil Etched
        String mhFoilEtched = productName.contains("Foil Etched") ? "-(Foil Etched)" : "";
        // MKM AB Print
        String mkmAbPrint = productName.contains("(b)") ? CROSS_SYMBOL : "";
        // OGW full art
        String ogwFullArt = productName.contains("Full Art")
                && (!setAbbrv.contains("OGW")) ? "-Full Art" : "";
        // One Concept Praetor
        String oneConceptPraetor = productName.contains("(Concept Praetor)") ? "-(Concept Praetor)" : "";
        // SOI 타미요의 저널
        String soiTamiyoJournal = productName.contains("Tamiyo's Journal (Entry ")
                ? "(" + productName.replace("Tamiyo's Journal (Entry ", "")
                : "";
        String occation = tcgSet.contains("Special Occasion") ? "-" + productName : "";
        String staJpPrint = "";
        if (setAbbrv.contains("STA") && productName.contains("(JP Alternate Art)")) {
            staJpPrint = "-(JP Print)";
        }

        // Surge Foil
        String Surge = (productName.contains("(Surge Foil)") && (!setAbbrv.contains("TMT"))
                && (!setAbbrv.contains("TMC"))
                && (!setAbbrv.contains("WHO"))) ? "-Surge Foil" : "";
        String unfNumber = tcgSet.contains("Unfinity") && productName.contains("(")
                && (!productName.contains("(Borderless)"))
                && (!productName.contains("(Showcase)"))
                        ? "(" + productName.split("\\(", 2)[1]
                        : "";
        String ustVer = tcgSet.contains("Unstable") && productName.contains("(")
                ? "-" + productName.split("\\(", 2)[1].replaceAll("\\)", "")
                : "";

        // 4) 최종 키 조립
        String key = special + serialNumber + set + "-" + numb + mkmAbPrint + "-" + printing + displayName + language
                + artSeries
                + token
                + displayCommander + misPrint + hmlVer2 + mhRetroFrame + mhFoilEtched + ogwFullArt
                + oneConceptPraetor + soiTamiyoJournal + occation + staJpPrint + Surge + unfNumber + ustVer;
        if (key.isBlank()) {
            return PARSING_FAILURE;
        }

        return key;
    }

    @Override
    public String buildCheckCodeMtgForMtgPrice(MtgPrice mtgPrice) {
        if (mtgPrice == null) {
            return PARSING_FAILURE;
        }
        String set = nf(mtgPrice.getSet());
        if (set.equals("UGIN")) {
            set = "FRF_" + set;
        }
        String name = nf(mtgPrice.getName());
        String codeRaw = nf(mtgPrice.getCode());
        String token = "";
        if (set.length() == 4 && set.startsWith("T")
                && (name.toUpperCase().contains("TOKEN")
                        || name.toUpperCase().contains("EMBLEM"))) {
            set = set.substring(1, 4);
            token = "-token";
        }
        // TCGPlayer 쪽 buildCheckCodeMtgForTcgP와 동일하게 이중면 번호 구분자 정규화
        String code = codeRaw.replaceAll(" // ", "_");
        String type = "normal".equals(mtgPrice.getType()) ? "Normal" : "Foil";
        // WAR JP
        String warJp = (set.contains("WAR") && codeRaw.contains("★")) ? "-JP Alternate Art" : "";
        // SLD Foil etched
        String sldFoilEtched = (set.contains("SLD") && codeRaw.contains("★")) ? "-Foil Etched" : "";
        // PLS Alternate Art Foil
        String plsAlternateArtFoil = (set.contains("PLS") && codeRaw.contains("★")) ? "-Alternate Art Foil"
                : "";
        String key = set + "-" + code + "-" + type + token + warJp + sldFoilEtched + plsAlternateArtFoil;
        if (key.isBlank()) {
            return PARSING_FAILURE;
        }
        return key;
    }

    @Override
    public String buildCheckCodeFabForFabPrice(FabPrice fabPrice) {
        if (fabPrice == null) {
            return PARSING_FAILURE;
        }

        String cardName = nf(fabPrice.getCardName());
        String setName = nf(fabPrice.getSetName());
        String rarity = nf(fabPrice.getRarity());

        String setNorm = nf(fabPrice.getSet()).replaceAll(FAB_GAME_NAME, "").trim();
        String setPrefix = setNorm.isEmpty() ? "" : setNorm.substring(0, 3);

        // String num = fabPrice.getCode() == null ? ""
        // : fabPrice.getCode().replaceAll("^.*?(\\d+)-.*$", "$1");
        String fabCode = nf(fabPrice.getCode());
        if (fabCode.isBlank()) {
            return PARSING_FAILURE;
        }
        String num = fabCode.contains("_CC") ? fabCode.split("_CC")[0] : fabCode.split("-")[0];
        if (setPrefix.equals("GEM")) {
            num = "0" + num;
        }

        String ccTag = cardName.contains("(CC Tag)") ? "CC-" : "";
        String foilNorm = normalizeFabFoil(fabPrice.getFoil());
        String edition = setName.contains("(1st Edition)")
                ? "1st Edition"
                : setName.contains("(Unlimited)") ? "Unlimited Edition" : "";

        // evr marvel edition 처리
        if ("Everfest (Marvel)".equals(setName)) {
            edition = "1st Edition";
        }
        // Marvel 처리
        String marvel = (rarity.contains("Marvel") || setName.contains("(Marvel)")) ? "-Marvel" : "";

        boolean rarityIsLegendaryMarvelOrFabled = rarity.contains("Legendary")
                || rarity.contains("Marvel")
                || rarity.contains("Fabled");

        String extendedArt = "";
        boolean extendedArtFromCard = cardName.contains("Extended Art")
                && !rarity.contains("Legendary")
                && !rarity.contains("Marvel")
                && !rarity.contains("Fabled");
        boolean extendedArtFromMarvelSet = setName.contains("(Marvel)") && !rarityIsLegendaryMarvelOrFabled;
        if (extendedArtFromCard || extendedArtFromMarvelSet) {
            extendedArt = "-Extended Art";
        }

        if (!rarityIsLegendaryMarvelOrFabled) {
            marvel = "";
        }

        // high seas 전용 처리
        if ("High Seas (Marvel)".equals(setName)) {
            extendedArt = "";
            marvel = "-Marvel";
        }
        // high seas 전용 처리2
        if ("High Seas".equals(setName) && rarity.contains("Marvel")) {
            extendedArt = "";
            marvel = "-Marvel";
        }

        String blitzAurora = setName.contains("Rosetta Blitz Deck - Aurora") ? "-AUR" : "";

        String key = setPrefix + "-" + num + "-" + ccTag + foilNorm
                + (edition.isEmpty() ? "" : "-" + edition)
                + marvel
                + extendedArt
                + blitzAurora;

        if (key.isBlank()) {
            return PARSING_FAILURE;
        }
        if (key.startsWith("PRM-")) {
            key = key.replaceFirst("PRM-", "");
        }
        key = key.replaceFirst("^([A-Za-z]{3})_", "$1-");
        // LGS- 키 정규화
        if (key.startsWith("LGS-")) {
            key = key.replaceAll("^(.*?(Foil|Normal)).*", "$1");
        }
        if (key.startsWith("HER-")) {
            key = key.replaceAll("^(.*?(Foil|Normal)).*", "$1");
        }
        if (key.startsWith("FAB-")) {
            key = key.replaceAll("^(.*?(Foil|Normal)).*", "$1");
        }
        if (key.startsWith("LSS-")) {
            key = key.replaceAll("^(.*?(Foil|Normal)).*", "$1");
        }

        return key;
    }

    /**
     * DB에서 사용하는 FAB 매칭 키와 동일 규칙:
     * CONCAT(LEFT(set_abbrv,3), SUBSTRING(TRIM(number),4,3),
     * TRIM(REPLACE(REPLACE(printing,'1st Edition',''),'Unlimited Edition','')),
     * CASE WHEN printing LIKE '%1st Edition%' THEN '1st Edition'
     * WHEN printing LIKE '%Unlimited%' THEN 'Unlimited' ELSE '' END)
     */
    @Override
    public String buildCheckCodeFabForTcgP(TcgPPrice tcgPrice) {
        if (tcgPrice == null) {
            return PARSING_FAILURE;
        }
        if ("Sealed Products".equals(tcgPrice.getType())) {
            String setPrefix = tcgPrice.getSetAbbrv() != null ? tcgPrice.getSetAbbrv() + "-" : "";
            return setPrefix + nf(tcgPrice.getProductName());
        }

        String productName = nf(tcgPrice.getProductName());
        String rawNumber = nf(tcgPrice.getNumber());
        String rarity = nf(tcgPrice.getRarity());

        String number = "";
        if (rawNumber.isBlank()) {
            number = productName;
        } else if (rawNumber.length() < 6) {
            number = rawNumber;
        } else if (rawNumber.length() == 6) {
            number = rawNumber.substring(0, 3) + "-" + rawNumber.substring(3, 6);
        } else if (rawNumber.length() > 6) {
            number = rawNumber;
        }

        if (rawNumber.contains("//") && !number.equalsIgnoreCase(productName)) {
            String[] parts = rawNumber.split("\\/\\/");
            List<String> nums = new ArrayList<>();
            for (String p : parts) {
                String n = nf(p).replaceAll("\\D", "");
                if (!n.isEmpty()) {
                    nums.add(n);
                }
            }
            number = nums.size() >= 2
                    ? rawNumber.substring(0, 3) + "-" + nums.get(0) + "_" + nums.get(1)
                    : number;
        }

        String printingRaw = tcgPrice.getPrinting() == null ? "" : tcgPrice.getPrinting();
        String printing = normalizeFabFoil(
                printingRaw.replace("1st Edition", "").replace("Unlimited Edition", "").trim());

        String edition;
        if (printingRaw.contains("1st Edition")) {
            edition = "1st Edition";
        } else if (printingRaw.contains("Unlimited")) {
            edition = "Unlimited Edition";
        } else {
            edition = "";
        }
        String marvel = productName.contains("(Marvel)") ? "Marvel" : "";
        String ccTag = productName.contains("(CC ") ? "CC-" : "";
        String extendedArt = productName.contains("(Extended Art)") ? "Extended Art" : "";
        extendedArt = productName.contains("(Extended Art)") && rarity.contains("Legendary")
                ? "Marvel"
                : extendedArt;
        String treasure = productName.contains("(Treasure)") ? "Marvel" : "";
        String blitzAurora = productName.contains("Blitz Deck: Rosetta - Aurora") ? "-AUR" : "";

        String key = number + "-" + ccTag + printing
                + (edition.isEmpty() ? "" : "-" + edition)
                + (marvel.isEmpty() ? "" : "-" + marvel)
                + (extendedArt.isEmpty() ? "" : "-" + extendedArt)
                + (treasure.isEmpty() ? "" : "-" + treasure)
                + blitzAurora;

        if (key.isBlank()) {
            return PARSING_FAILURE;
        }
        // LGS- 키 정규화
        if (key.startsWith("LGS-")) {
            key = key.replaceAll("^(.*?(Foil|Normal)).*", "$1");
        }
        if (key.startsWith("HER-")) {
            key = key.replaceAll("^(.*?(Foil|Normal)).*", "$1");
        }
        if (key.startsWith("FAB-")) {
            key = key.replaceAll("^(.*?(Foil|Normal)).*", "$1");
        }
        if (key.startsWith("LSS-")) {
            key = key.replaceAll("^(.*?(Foil|Normal)).*", "$1");
        }

        return key;
    }

    // private static String safeSubstring(String s, int begin, int end) {
    // if (s == null || begin < 0 || begin > s.length() || end < begin) {
    // return "";
    // }
    // int to = Math.min(end, s.length());
    // if (begin >= to) {
    // return "";
    // }
    // return s.substring(begin, to);
    // }
    @Override
    public String buildCheckCodeForSWU(TcgPPrice tcgPPrice) {
        return buildCheckCodeWithProductId(tcgPPrice);
    }

    @Override
    public String buildCheckCodeForLorcana(TcgPPrice tcgPPrice) {
        return buildCheckCodeWithProductId(tcgPPrice);
    }

    @Override
    public String buildCheckCodeForRift(TcgPPrice tcgPPrice) {
        return buildCheckCodeWithProductId(tcgPPrice);
    }

    private String buildCheckCodeWithProductId(TcgPPrice tcgPPrice) {
        if (tcgPPrice == null || tcgPPrice.getProductId() == null) {
            return PARSING_FAILURE;
        }
        String checkCode = nf(tcgPPrice.getCodeNumber()) + "-" + nf(tcgPPrice.getPrinting()) + "-"
                + tcgPPrice.getProductId();
        String condition = nf(tcgPPrice.getCondition());
        if (condition.contains("-")) {
            checkCode += "-" + condition.split("-", 2)[1];
        }
        return checkCode;
    }

}
