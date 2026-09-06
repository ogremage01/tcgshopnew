import { NextRequest, NextResponse } from "next/server";

import { getPostalCodeApiConfig } from "@/lib/postal-code.server";

export async function GET(request: NextRequest) {
  const { searchParams } = request.nextUrl;
  const keyword = searchParams.get("keyword")?.trim();
  const locale = searchParams.get("locale") ?? "ko";
  const currentPage = searchParams.get("currentPage") ?? "1";
  const countPerPage = searchParams.get("countPerPage") ?? "10";

  if (!keyword) {
    return NextResponse.json({ message: "keyword is required" }, { status: 400 });
  }

  const { apiUrl, confmKey } = getPostalCodeApiConfig(locale);
  if (!confmKey) {
    return NextResponse.json(
      { message: "Postal code API is not configured" },
      { status: 503 },
    );
  }

  const url = new URL(apiUrl);
  url.searchParams.set("confmKey", confmKey);
  url.searchParams.set("resultType", "json");
  url.searchParams.set("keyword", keyword);
  url.searchParams.set("currentPage", currentPage);
  url.searchParams.set("countPerPage", countPerPage);

  const targetUrl = url.toString();
  if (process.env.NODE_ENV === "development") {
    console.log("[postal-code proxy]", { locale, confmKey, targetUrl });
  }

  const upstream = await fetch(targetUrl, { cache: "no-store" });
  const body = await upstream.text();
  const contentType = upstream.headers.get("content-type") ?? "";

  if (!contentType.includes("json") && body.trimStart().startsWith("<")) {
    return NextResponse.json(
      {
        message: "Postal code upstream returned non-JSON response",
        targetUrl,
      },
      { status: 502 },
    );
  }

  return new NextResponse(body, {
    status: upstream.status,
    headers: {
      "Content-Type": upstream.headers.get("content-type") ?? "application/json",
      ...(process.env.NODE_ENV === "development" && {
        "X-Debug-ConfmKey": confmKey,
        "X-Debug-Target-Url": targetUrl,
      }),
    },
  });
}
