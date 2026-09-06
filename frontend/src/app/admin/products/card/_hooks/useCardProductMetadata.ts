"use client";

import { useState, useEffect } from "react";
import { api } from "@/lib/api";
import type { GradePricingPolicyDto, StorageDto } from "@/types/product";

export function useCardProductMetadata() {
    const [storageList, setStorageList] = useState<StorageDto[]>([]);
    const [gradePolicyList, setGradePolicyList] = useState<GradePricingPolicyDto[]>([]);

    useEffect(() => {
        api.get<StorageDto[]>("/api/admin/product/metadata/storage/list")
            .then(setStorageList)
            .catch(console.error);
        api.get<GradePricingPolicyDto[]>("/api/admin/product/metadata/grade-price")
            .then(setGradePolicyList)
            .catch(console.error);
    }, []);

    return { storageList, gradePolicyList };
}
