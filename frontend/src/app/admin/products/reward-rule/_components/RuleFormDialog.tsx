"use client"

import { Button } from "@/components/ui/button"
import { Checkbox } from "@/components/ui/checkbox"
import {
    Dialog,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Separator } from "@/components/ui/separator"
import { type CalRuleForm, type RewardRuleFormData } from "@/app/admin/_hooks/useRewardRule"
import { GAME_ENUM } from "@/config/gameEnum"
import { useCardProductLanguages } from "@/hooks/use-card-product-languages"

const GAME_OPTIONS = GAME_ENUM.map((g) => ({
    value: g.game,
    label: g.gameAbbr,
}))
const PRODUCT_TYPE_OPTIONS = ["Cards", "SealedProducts", "ManualProducts"]
const CONDITION_OPTIONS = ["NM", "EX", "VG", "G"]
const PRINTING_OPTIONS = ["Foil", "Normal"]

const ALL_VALUE = "__ALL__"

type SelectOption = string | { value: string; label: string }

function SelectField({
    label,
    value,
    options,
    onChange,
}: {
    label: string
    value: string
    options: SelectOption[]
    onChange: (v: string) => void
}) {
    return (
        <div className="flex flex-col gap-1">
            <Label>{label}</Label>
            <Select
                value={value === "" ? ALL_VALUE : value}
                onValueChange={(v) => onChange(v === ALL_VALUE ? "" : v)}
            >
                <SelectTrigger>
                    <SelectValue placeholder="전체 적용" />
                </SelectTrigger>
                <SelectContent>
                    <SelectItem value={ALL_VALUE}>전체 적용</SelectItem>
                    {options.map((o) => {
                        const val = typeof o === "string" ? o : o.value
                        const lbl = typeof o === "string" ? o : o.label
                        return <SelectItem key={val} value={val}>{lbl}</SelectItem>
                    })}
                </SelectContent>
            </Select>
        </div>
    )
}

function RuleFormFields({
    form,
    onChange,
}: {
    form: RewardRuleFormData
    onChange: (f: RewardRuleFormData) => void
}) {
    const { languages } = useCardProductLanguages()
    const setCal = (patch: Partial<CalRuleForm>) =>
        onChange({ ...form, calRule: { ...form.calRule, ...patch } })

    return (
        <div className="flex flex-col gap-4">
            <div className="flex flex-col gap-1">
                <Label>규칙명 *</Label>
                <Input
                    value={form.name}
                    onChange={(e) => onChange({ ...form, name: e.target.value })}
                    placeholder="규칙 이름"
                />
            </div>
            <div className="flex flex-col gap-1">
                <Label>적립율 (예: 0.05 = 5%)</Label>
                <Input
                    type="number"
                    step="0.001"
                    min="0"
                    max="1"
                    value={form.rewardPercentage ?? ""}
                    onChange={(e) =>
                        onChange({ ...form, rewardPercentage: e.target.value === "" ? null : Number(e.target.value) })
                    }
                    placeholder="0.05"
                />
            </div>

            <Separator />
            <p className="text-sm font-medium">
                적용 조건{" "}
                <span className="text-muted-foreground font-normal">(비워두면 전체 적용)</span>
            </p>

            <div className="grid grid-cols-2 gap-3">
                <SelectField label="게임" value={form.calRule.game} options={GAME_OPTIONS} onChange={(v) => setCal({ game: v })} />
                <div className="flex flex-col gap-1">
                    <Label>상품 유형</Label>
                    <Input value="Cards" disabled className="bg-muted text-muted-foreground" />
                </div>
                <SelectField label="컨디션" value={form.calRule.condition} options={CONDITION_OPTIONS} onChange={(v) => setCal({ condition: v })} />
                <SelectField
                    label="언어"
                    value={form.calRule.language}
                    options={languages.map((language) => ({ value: language.code, label: language.displayName }))}
                    onChange={(v) => setCal({ language: v })}
                />
                <div className="flex flex-col gap-1">
                    <Label>세트 코드</Label>
                    <Input value={form.calRule.set} onChange={(e) => setCal({ set: e.target.value })} placeholder="HVY / MH3 …" />
                </div>
                <div className="flex flex-col gap-1">
                    <Label>레어도</Label>
                    <Input value={form.calRule.rarity} onChange={(e) => setCal({ rarity: e.target.value })} placeholder="Legend / Rare …" />
                </div>
                <SelectField label="인쇄 유형" value={form.calRule.printing} options={PRINTING_OPTIONS} onChange={(v) => setCal({ printing: v })} />
                <div className="flex flex-col gap-1">
                    <Label>세트 번호</Label>
                    <Input value={form.calRule.setNumber} onChange={(e) => setCal({ setNumber: e.target.value })} placeholder="001" />
                </div>
            </div>
            <div className="flex flex-col gap-1">
                <Label>카드 이름</Label>
                <Input value={form.calRule.cardName} onChange={(e) => setCal({ cardName: e.target.value })} placeholder="Invoke Dominia …" />
            </div>

            <Separator />

            <div className="grid grid-cols-2 gap-3">
                <div className="flex flex-col gap-1">
                    <Label>시작일</Label>
                    <Input type="date" value={form.startAt} onChange={(e) => onChange({ ...form, startAt: e.target.value })} />
                </div>
                <div className="flex flex-col gap-1">
                    <Label>종료일</Label>
                    <Input type="date" value={form.endAt} onChange={(e) => onChange({ ...form, endAt: e.target.value })} />
                </div>
            </div>
            <div className="flex items-center gap-2">
                <Checkbox
                    id="isActive"
                    checked={form.isActive}
                    onCheckedChange={(v) => onChange({ ...form, isActive: v === true })}
                />
                <Label htmlFor="isActive">활성화</Label>
            </div>
        </div>
    )
}

type Props = {
    open: boolean
    onOpenChange: (open: boolean) => void
    title: string
    form: RewardRuleFormData
    onChange: (f: RewardRuleFormData) => void
    onConfirm: () => void
    confirmLabel: string
}

export function RuleFormDialog({ open, onOpenChange, title, form, onChange, onConfirm, confirmLabel }: Props) {
    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="max-w-lg max-h-[90vh] overflow-y-auto" aria-describedby={undefined}>
                <DialogHeader>
                    <DialogTitle>{title}</DialogTitle>
                </DialogHeader>
                <RuleFormFields form={form} onChange={onChange} />
                <DialogFooter>
                    <Button variant="outline" onClick={() => onOpenChange(false)}>취소</Button>
                    <Button onClick={onConfirm}>{confirmLabel}</Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    )
}
