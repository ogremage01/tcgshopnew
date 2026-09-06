"use client"
import { Input } from "@/components/ui/input"
import { useState } from "react"
import { apiClient } from "@/lib/api"
import { Button } from "@/components/ui/button"
import { Search } from "lucide-react"
import { Field, FieldLabel, FieldDescription, FieldContent } from "@/components/ui/field"
export default function AppSearchBar({ url, queryParam, placeholder, onSearch, description }:
    { url: string, queryParam: string, placeholder: string, onSearch: (value: unknown) => void, description: string }) {
    const [value, setValue] = useState("")
    const handleSearch = () => {
        apiClient.get(`${url}?${queryParam}=${value}`).then(response => {
            onSearch(response.data)

        })
    }
    const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
        if (e.nativeEvent.isComposing || e.key !== "Enter") return
        e.preventDefault()
        if (value.length === 0) return
        handleSearch()
    }
    return <Field>
        <FieldLabel>{description}</FieldLabel>
        <FieldContent className="flex flex-row gap-2">
            <Input
                className="w-full"
                type="text"
                placeholder={placeholder}
                value={value}
                onChange={(e) => setValue(e.target.value)}
                onKeyDown={handleKeyDown}
            />
            <Button className="w-fit" onClick={handleSearch} disabled={value.length === 0}><Search /></Button>
        </FieldContent>
        <FieldDescription>
            {description}
        </FieldDescription>
    </Field>
}