package com.shop.card.metadata.support;

import java.util.List;
import java.util.Optional;

import com.shop.card.entity.UnionPrice;

public final class PrintingResolver {

        private static final List<PrintingOverrideRule> RULES = List.of(
                        CheckCodeSegmentContainsRule.of(1, "e", "Foil Etched", "H1R", "MH2"));

        private PrintingResolver() {
        }

        public static String resolve(UnionPrice unionPrice) {
                if (unionPrice == null) {
                        return null;
                }
                return resolve(new PrintingResolveContext(
                                unionPrice.getSetCode(),
                                unionPrice.getCheckCodeRefined(),
                                unionPrice.getPrinting(),
                                unionPrice.getPrintType()));
        }

        public static String resolve(PrintingResolveContext context) {
                if (context == null) {
                        return null;
                }
                for (PrintingOverrideRule rule : RULES) {
                        Optional<String> override = rule.apply(context);
                        if (override.isPresent()) {
                                return override.get();
                        }
                }
                if (context.printing() != null && !context.printing().isBlank()) {
                        return context.printing();
                }
                return context.printType();
        }
}
