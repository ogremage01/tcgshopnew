package com.shop.checkout.service;

import com.shop.checkout.dto.CheckoutDraftCreateResponse;
import com.shop.checkout.dto.CheckoutDraftResponse;
import com.shop.checkout.dto.PatchCheckoutDraftRequest;
import com.shop.common.identity.UserIdentity;

public interface CheckoutDraftService {

    CheckoutDraftCreateResponse createDraft(UserIdentity identity);

    CheckoutDraftResponse getDraft(String publicId, UserIdentity identity);

    CheckoutDraftResponse patchDraft(String publicId, PatchCheckoutDraftRequest request, UserIdentity identity);
}
