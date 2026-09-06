import type { Alarm } from "@/types/alarm";

/** SSE data 필드(JSON 문자열)를 Alarm 객체로 변환합니다. */
export function parseAlarmData(raw: string): Alarm | null {
  try {
    return JSON.parse(raw) as Alarm;
  } catch {
    return null;
  }
}

/** Spring SSE 청크를 파싱합니다. (\r\n 대응) */
export function parseSseChunk(chunk: string): { eventName: string; data: string } {
  let eventName = "message";
  let data = "";

  const normalized = chunk.replace(/\r\n/g, "\n");
  for (const line of normalized.split("\n")) {
    if (line.startsWith("event:")) {
      eventName = line.slice(6).trim();
    }
    if (line.startsWith("data:")) {
      data += line.slice(5).trim();
    }
  }

  return { eventName, data };
}
