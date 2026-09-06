"use client";

import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import { Separator } from "@/components/ui/separator";
import { Checkbox } from "@/components/ui/checkbox";
import { Button } from "@/components/ui/button";
import { api } from "@/lib/api.client";
import { OrderConfigDto } from "@/types/order";
import { useState, useEffect } from "react";

export default function OrderConfigPage() {
    const [shippingFee, setShippingFee] = useState<OrderConfigDto | null>(null);
    const [freeShippingThreshold, setFreeShippingThreshold] = useState<OrderConfigDto | null>(null);
    useEffect(() => {
        api.get<OrderConfigDto[]>("/api/admin/orders/config").then((res) => {
            setShippingFee(res.find((config) => config.configKey === "SHIPPING_FEE") ?? null);
            setFreeShippingThreshold(res.find((config) => config.configKey === "FREE_SHIPPING_THRESHOLD") ?? null);
        });
    }, []);

    const handleSubmitShippingFee = (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        const formData = new FormData(e.target as HTMLFormElement);
        api.post<OrderConfigDto>("/api/admin/orders/shipping-fee", {
            configKey: "SHIPPING_FEE",
            configValue: formData.get("shippingFee") as string,
            isEnabled: shippingFee?.isEnabled ?? false,
        }).then((res) => {
            alert("배송비 설정에 성공했습니다");
            setShippingFee(res);
        }).catch((err) => {
            console.error(err);
            alert("배송비 설정에 실패했습니다");
        });
    };
    const handleSubmitFreeShippingThreshold = (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        const formData = new FormData(e.target as HTMLFormElement);
        api.post<OrderConfigDto>("/api/admin/orders/free-shipping-threshold", {
            configKey: "FREE_SHIPPING_THRESHOLD",
            configValue: formData.get("freeShippingThreshold") as string,
            isEnabled: freeShippingThreshold?.isEnabled ?? false,
        }).then((res) => {
            alert("배송비 무료 기준 금액 설정에 성공했습니다");
            setFreeShippingThreshold(res);
        }).catch((err) => {
            console.error(err);
            alert("배송비 무료 기준 금액 설정에 실패했습니다");
        });
    };
    return (
        <div>
            <h1 className="text-2xl font-bold">주문 설정</h1>
            <p className="text-sm text-muted-foreground">
                주문 설정을 관리합니다.
            </p>
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                <Card>
                    <CardHeader>
                        <CardTitle>배송비 설정</CardTitle>
                        <CardDescription>기본 배송비를 설정합니다.</CardDescription>
                    </CardHeader>
                    <CardContent >
                        <Label>기본 배송비</Label>
                        <div>
                            <form className="flex flex-col gap-2" onSubmit={handleSubmitShippingFee}>
                                <div className="flex flex-row gap-2">
                                    <Input
                                        type="number"
                                        placeholder="배송비"
                                        value={shippingFee?.configValue ?? ""}
                                        name="shippingFee"
                                        onChange={(e) =>
                                            setShippingFee((prev) =>
                                                prev ? { ...prev, configValue: e.target.value } : prev
                                            )
                                        }
                                    />
                                    <Button type="submit">저장</Button>
                                </div>

                                <div className="flex flex-row gap-2 my-2">
                                    <Label htmlFor="isShippingFeeEnabled">배송비 활성화</Label>
                                    <Checkbox
                                        id="isShippingFeeEnabled"
                                        checked={shippingFee?.isEnabled ?? false}
                                        onCheckedChange={(checked) =>
                                            setShippingFee((prev) =>
                                                prev ? { ...prev, isEnabled: checked === true } : prev
                                            )
                                        }
                                    />
                                </div>
                            </form>
                        </div>
                        <Separator className="my-4" />
                        <Label>배송비 무료 기준 금액</Label>
                        <div>
                            <form className="flex flex-col gap-2" onSubmit={handleSubmitFreeShippingThreshold}>
                                <div className="flex flex-row gap-2">
                                    <Input
                                        type="number"
                                        placeholder="배송비 무료 기준 금액"
                                        value={freeShippingThreshold?.configValue ?? ""}
                                        name="freeShippingThreshold"
                                        onChange={(e) =>
                                            setFreeShippingThreshold((prev) =>
                                                prev ? { ...prev, configValue: e.target.value } : prev
                                            )
                                        }
                                    />
                                    <Button type="submit">저장</Button>
                                </div>
                                <div className="flex flex-row gap-2 my-2">
                                    <Label htmlFor="isFreeShippingThresholdEnabled">배송비 무료 기준 금액 활성화</Label>
                                    <Checkbox
                                        id="isFreeShippingThresholdEnabled"
                                        checked={freeShippingThreshold?.isEnabled ?? false}
                                        onCheckedChange={(checked) =>
                                            setFreeShippingThreshold((prev) =>
                                                prev ? { ...prev, isEnabled: checked === true } : prev
                                            )
                                        }
                                    />
                                </div>
                            </form>
                        </div>
                    </CardContent>
                </Card>
            </div>
        </div>
    );
}