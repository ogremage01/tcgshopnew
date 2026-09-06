package com.shop.card.metadata.support;

import java.util.Optional;

public interface PrintingOverrideRule {

        Optional<String> apply(PrintingResolveContext context);
}
