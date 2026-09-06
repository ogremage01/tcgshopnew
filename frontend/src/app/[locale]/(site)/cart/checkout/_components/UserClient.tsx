"use client";

import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { Field, FieldLabel, FieldContent } from "@/components/ui/field";
import { Card } from "@/components/ui/card";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { Label } from "@/components/ui/label";
import { useState } from "react";

const userSchema = z.object({
    name: z.string().min(1),
    email: z.string().email(),
    phone: z.string().min(1),
    zipCode: z.string().min(1),
    address: z.string().min(1),
    addressDetail: z.string().min(1),
})
export default function UserClient() {
    const [selected, setSelected] = useState<"store" | "delivery">("delivery");
    const changeSelected = (value: "store" | "delivery") => {
        setSelected(value);
    }
    return (
        <div>
            <Card className="w-full gap-4 p-4">
                <RadioGroup
                    className="flex flex-row gap-2 my-4"
                    value={selected}
                    onValueChange={(value: "store" | "delivery") => changeSelected(value)}
                >
                    <div className="flex flex-row gap-2">
                        <RadioGroupItem value="store" />
                        <Label>
                            매장 수령
                        </Label>
                    </div>
                    <div className="flex flex-row gap-2">
                        <RadioGroupItem value="delivery" />
                        <Label>
                            배송 수령
                        </Label>
                    </div>
                </RadioGroup>

                <Field>
                    <FieldLabel>
                        수령 정보
                    </FieldLabel>
                    <FieldContent>
                        <Input
                            type="text"
                            placeholder="수령인 이름"
                            className="w-full"
                        />
                        <Input
                            type="text"
                            placeholder="이메일"
                            className="w-full"
                        />
                        <Input
                            type="text"
                            placeholder="수령인 전화번호"
                            className="w-full"
                        />
                        {selected === "delivery" && (
                            <>
                                <Input
                                    type="text"
                                    placeholder="우편번호"
                                    className="w-full"
                                />
                                <Input
                                    type="text"
                                    placeholder="주소"
                                    className="w-full"
                                />
                                <Input
                                    type="text"
                                    placeholder="상세주소"
                                    className="w-full"
                                />
                            </>
                        )}
                    </FieldContent>
                </Field>

            </Card>
        </div>
    )
}