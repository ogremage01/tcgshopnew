import {
    Select,
    SelectTrigger,
    SelectValue,
    SelectContent,
    SelectItem,
} from "@/components/ui/select";
import { ADMIN_ORDER_STATUS_SELECT_OPTIONS } from "@/lib/order-status";

type AdminOrderStatusSelectProps = {
    value: string;
    onValueChange: (value: string) => void;
    contentClassName?: string;
    disabled?: boolean;
};

export function AdminOrderStatusSelect({
    value,
    onValueChange,
    contentClassName,
    disabled = false,
}: AdminOrderStatusSelectProps) {
    return (
        <Select value={value} onValueChange={onValueChange} disabled={disabled}>
            <SelectTrigger>
                <SelectValue placeholder="주문 상태" />
            </SelectTrigger>
            <SelectContent className={contentClassName}>
                {ADMIN_ORDER_STATUS_SELECT_OPTIONS.map(({ value: code, label }) => (
                    <SelectItem key={code} value={code}>
                        {label}
                    </SelectItem>
                ))}
            </SelectContent>
        </Select>
    );
}
