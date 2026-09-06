import { useState, useEffect, useCallback } from "react"
import { api } from "@/lib/api.client"

export type RewardRuleDto = {
    id: number
    name: string
    rank: number
    rewardPercentage: number | null
    calRule: string | null
    isActive: boolean
    startAt: string | null
    endAt: string | null
    createdAt: string | null
    updatedAt: string | null
}

export type CalRuleForm = {
    game: string
    productType: string
    condition: string
    language: string
    set: string
    rarity: string
    printing: string
    cardName: string
    setNumber: string
}

export const EMPTY_CAL_RULE: CalRuleForm = {
    game: "",
    productType: "Cards",
    condition: "",
    language: "",
    set: "",
    rarity: "",
    printing: "",
    cardName: "",
    setNumber: "",
}

export type RewardRuleFormData = {
    name: string
    rewardPercentage: number | null
    calRule: CalRuleForm
    isActive: boolean
    startAt: string
    endAt: string
}

const BASE = "/api/admin/reward-rules"

export function useRewardRule() {
    const [rules, setRules] = useState<RewardRuleDto[]>([])

    const fetchRules = useCallback(() => {
        api.get<RewardRuleDto[]>(BASE).then(setRules).catch(console.error)
    }, [])

    useEffect(() => {
        fetchRules()
    }, [fetchRules])

    const createRule = async (form: RewardRuleFormData) => {
        await api.post(BASE, toPayload(form))
        fetchRules()
    }

    const updateRule = async (id: number, form: RewardRuleFormData) => {
        await api.put(`${BASE}/${id}`, toPayload(form))
        fetchRules()
    }

    const deleteRule = async (id: number) => {
        await api.delete(`${BASE}/${id}`)
        fetchRules()
    }

    const toggleActive = async (rule: RewardRuleDto) => {
        await api.put(`${BASE}/${rule.id}`, toPayload({
            name: rule.name,
            rewardPercentage: rule.rewardPercentage,
            calRule: parseCalRule(rule.calRule),
            isActive: !rule.isActive,
            startAt: rule.startAt ? rule.startAt.substring(0, 10) : "",
            endAt: rule.endAt ? rule.endAt.substring(0, 10) : "",
        }))
        fetchRules()
    }

    const moveUp = async (index: number) => {
        if (index === 0) return
        const newOrder = [...rules]
        ;[newOrder[index - 1], newOrder[index]] = [newOrder[index], newOrder[index - 1]]
        setRules(newOrder)
        await api.put(`${BASE}/reorder`, newOrder.map((r) => r.id))
        fetchRules()
    }

    const moveDown = async (index: number) => {
        if (index === rules.length - 1) return
        const newOrder = [...rules]
        ;[newOrder[index], newOrder[index + 1]] = [newOrder[index + 1], newOrder[index]]
        setRules(newOrder)
        await api.put(`${BASE}/reorder`, newOrder.map((r) => r.id))
        fetchRules()
    }

    return { rules, createRule, updateRule, deleteRule, toggleActive, moveUp, moveDown }
}

export function parseCalRule(json: string | null): CalRuleForm {
    if (!json) return { ...EMPTY_CAL_RULE }
    try {
        const parsed = JSON.parse(json) as Record<string, string>
        return {
            game: parsed.game ?? "",
            productType: parsed.productType ?? "",
            condition: parsed.condition ?? "",
            language: parsed.language ?? "",
            set: parsed.set ?? "",
            rarity: parsed.rarity ?? "",
            printing: parsed.printing ?? "",
            cardName: parsed.cardName ?? "",
            setNumber: parsed.setNumber ?? "",
        }
    } catch {
        return { ...EMPTY_CAL_RULE }
    }
}

function serializeCalRule(cal: CalRuleForm): string | null {
    const obj: Record<string, string> = {}
    for (const [k, v] of Object.entries(cal)) {
        if (v.trim() !== "") obj[k] = v.trim()
    }
    return Object.keys(obj).length > 0 ? JSON.stringify(obj) : null
}

function toPayload(form: RewardRuleFormData) {
    return {
        name: form.name,
        rewardPercentage: form.rewardPercentage,
        calRule: serializeCalRule(form.calRule),
        isActive: form.isActive ?? false,
        startAt: form.startAt ? `${form.startAt}T00:00:00` : null,
        endAt: form.endAt ? `${form.endAt}T00:00:00` : null,
    }
}
