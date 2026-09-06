"use client"

import { useState } from "react"
import { ArrowDown, ArrowUp, Pencil, Plus, Trash2 } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Checkbox } from "@/components/ui/checkbox"
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import {
    EMPTY_CAL_RULE,
    type RewardRuleDto,
    type RewardRuleFormData,
    parseCalRule,
    useRewardRule,
} from "@/app/admin/_hooks/useRewardRule"
import { RuleFormDialog } from "./_components/RuleFormDialog"

const EMPTY_FORM: RewardRuleFormData = {
    name: "",
    rewardPercentage: null,
    calRule: { ...EMPTY_CAL_RULE },
    isActive: true,
    startAt: "",
    endAt: "",
}

function toForm(rule: RewardRuleDto): RewardRuleFormData {
    return {
        name: rule.name,
        rewardPercentage: rule.rewardPercentage,
        calRule: parseCalRule(rule.calRule),
        isActive: rule.isActive,
        startAt: rule.startAt ? rule.startAt.substring(0, 10) : "",
        endAt: rule.endAt ? rule.endAt.substring(0, 10) : "",
    }
}

function CalRuleSummary({ calRule }: { calRule: string | null }) {
    if (!calRule) return <span className="text-muted-foreground text-sm">전체 적용</span>
    try {
        const entries = Object.entries(JSON.parse(calRule) as Record<string, string>)
        if (entries.length === 0) return <span className="text-muted-foreground text-sm">전체 적용</span>
        return (
            <div className="flex flex-wrap gap-1">
                {entries.map(([k, v]) => (
                    <span key={k} className="text-xs bg-muted px-1.5 py-0.5 rounded">{k}: {v}</span>
                ))}
            </div>
        )
    } catch {
        return <span className="text-xs text-destructive">JSON 오류</span>
    }
}

export default function RewardRulePage() {
    const { rules, createRule, updateRule, deleteRule, toggleActive, moveUp, moveDown } = useRewardRule()

    const [addOpen, setAddOpen] = useState(false)
    const [addForm, setAddForm] = useState<RewardRuleFormData>(EMPTY_FORM)

    const [editOpen, setEditOpen] = useState(false)
    const [editTarget, setEditTarget] = useState<RewardRuleDto | null>(null)
    const [editForm, setEditForm] = useState<RewardRuleFormData>(EMPTY_FORM)

    const [deleteTarget, setDeleteTarget] = useState<RewardRuleDto | null>(null)

    const handleAdd = async () => {
        if (!addForm.name.trim()) { alert("규칙명을 입력하세요."); return }
        await createRule(addForm)
        setAddOpen(false)
        setAddForm(EMPTY_FORM)
    }

    const openEdit = (rule: RewardRuleDto) => {
        setEditTarget(rule)
        setEditForm(toForm(rule))
        setEditOpen(true)
    }

    const handleEdit = async () => {
        if (!editTarget) return
        if (!editForm.name.trim()) { alert("규칙명을 입력하세요."); return }
        await updateRule(editTarget.id, editForm)
        setEditOpen(false)
    }

    const handleDelete = async () => {
        if (!deleteTarget) return
        await deleteRule(deleteTarget.id)
        setDeleteTarget(null)
    }

    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between">
                <h2 className="text-2xl font-bold">적립금 규칙 관리</h2>
                <Button onClick={() => { setAddForm(EMPTY_FORM); setAddOpen(true) }}>
                    <Plus className="mr-1 h-4 w-4" />규칙 추가
                </Button>
            </div>

            <Card>
                <CardHeader><CardTitle>적립 규칙 목록</CardTitle></CardHeader>
                <CardContent>
                    <Table>
                        <TableHeader>
                            <TableRow>
                                <TableHead className="w-14">순위</TableHead>
                                <TableHead>규칙명</TableHead>
                                <TableHead className="w-24">적립율</TableHead>
                                <TableHead>적용 조건</TableHead>
                                <TableHead className="w-44">기간</TableHead>
                                <TableHead className="w-16">활성화</TableHead>
                                <TableHead className="w-24">순서</TableHead>
                                <TableHead className="w-24">관리</TableHead>
                            </TableRow>
                        </TableHeader>
                        <TableBody>
                            {rules.length === 0 && (
                                <TableRow>
                                    <TableCell colSpan={8} className="text-center text-muted-foreground py-8">
                                        등록된 규칙이 없습니다.
                                    </TableCell>
                                </TableRow>
                            )}
                            {rules.map((rule, index) => (
                                <TableRow key={rule.id}>
                                    <TableCell className="font-mono text-sm">{rule.rank}</TableCell>
                                    <TableCell className="font-medium">{rule.name}</TableCell>
                                    <TableCell className="font-mono text-sm">
                                        {rule.rewardPercentage != null ? `${(rule.rewardPercentage * 100).toFixed(1)}%` : "-"}
                                    </TableCell>
                                    <TableCell><CalRuleSummary calRule={rule.calRule} /></TableCell>
                                    <TableCell className="text-sm">
                                        {rule.startAt || rule.endAt ? (
                                            <>{rule.startAt ? rule.startAt.substring(0, 10) : "∞"}{" ~ "}{rule.endAt ? rule.endAt.substring(0, 10) : "∞"}</>
                                        ) : (
                                            <span className="text-muted-foreground">무기한</span>
                                        )}
                                    </TableCell>
                                    <TableCell>
                                        <Checkbox checked={rule.isActive} onCheckedChange={() => toggleActive(rule)} />
                                    </TableCell>
                                    <TableCell>
                                        <div className="flex gap-1">
                                            <Button variant="outline" size="icon" disabled={index === 0} onClick={() => moveUp(index)}>
                                                <ArrowUp className="h-4 w-4" />
                                            </Button>
                                            <Button variant="outline" size="icon" disabled={index === rules.length - 1} onClick={() => moveDown(index)}>
                                                <ArrowDown className="h-4 w-4" />
                                            </Button>
                                        </div>
                                    </TableCell>
                                    <TableCell>
                                        <div className="flex gap-1">
                                            <Button variant="outline" size="icon" onClick={() => openEdit(rule)}>
                                                <Pencil className="h-4 w-4" />
                                            </Button>
                                            <Button variant="destructive" size="icon" onClick={() => setDeleteTarget(rule)}>
                                                <Trash2 className="h-4 w-4" />
                                            </Button>
                                        </div>
                                    </TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                </CardContent>
            </Card>

            <RuleFormDialog
                open={addOpen}
                onOpenChange={setAddOpen}
                title="적립금 규칙 추가"
                form={addForm}
                onChange={setAddForm}
                onConfirm={handleAdd}
                confirmLabel="추가"
            />

            <RuleFormDialog
                open={editOpen}
                onOpenChange={setEditOpen}
                title="적립금 규칙 수정"
                form={editForm}
                onChange={setEditForm}
                onConfirm={handleEdit}
                confirmLabel="저장"
            />

            <Dialog open={!!deleteTarget} onOpenChange={(open) => { if (!open) setDeleteTarget(null) }}>
                <DialogContent className="max-w-sm">
                    <DialogHeader>
                        <DialogTitle>규칙 삭제</DialogTitle>
                        <DialogDescription>이 규칙을 삭제하시겠습니까?</DialogDescription>
                    </DialogHeader>
                    <DialogFooter>
                        <Button variant="outline" onClick={() => setDeleteTarget(null)}>취소</Button>
                        <Button variant="destructive" onClick={handleDelete}>삭제</Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
        </div>
    )
}
