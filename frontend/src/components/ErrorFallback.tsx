"use client";

import { Button } from "@/components/ui/button";
import { useRouter } from "next/navigation";
import { useEffect } from "react";

type ErrorFallbackProps = {
  title: string;
  description: string;
  homeLabel: string;
  homeHref: string;
};

/** Radix 등이 에러로 언마운트되며 pointer-events/overflow 정리가 안 남는 경우 복구 */
function restoreDocumentPointerAndScroll() {
  if (typeof document === "undefined") return;
  const { body, documentElement: html } = document;
  body.style.removeProperty("pointer-events");
  html.style.removeProperty("pointer-events");
  body.style.removeProperty("overflow");
  html.style.removeProperty("overflow");
}

export default function ErrorFallback({
  title,
  description,
  homeLabel,
  homeHref,
}: ErrorFallbackProps) {
  const router = useRouter();

  // useEffect(() => {
  //   const timer = window.setTimeout(() => {
  //     restoreDocumentPointerAndScroll();
  //     router.replace(homeHref);
  //   }, 1000);

  //   return () => window.clearTimeout(timer);
  // }, [router, homeHref]);

  return (
    <div className="flex h-screen flex-col items-center justify-center gap-4">
      <h1>{title}</h1>
      <p>{description}</p>
      <Button
        onClick={() => {
          restoreDocumentPointerAndScroll();
          router.push(homeHref);
        }}
      >
        {homeLabel}
      </Button>
    </div>
  );
}
