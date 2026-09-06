export const formatDateTime = (value: string | Date | undefined | null) => {
    if (value == null || value === "") return "";
    const date = value instanceof Date ? value : new Date(value);
    if (isNaN(date.getTime())) return "";

    const parts = new Intl.DateTimeFormat("ko-KR", {
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
        second: "2-digit",
        hour12: false,
    }).formatToParts(date);

    const m = Object.fromEntries(parts.map(p => [p.type, p.value]));

    return `${m.year}-${m.month}-${m.day} ${m.hour}:${m.minute}:${m.second}`;
};