"use client"

import { useEffect } from "react"

export default function PageShowLogger() {
    useEffect(() => {
        const navEntry = performance.getEntriesByType("navigation")[0] as PerformanceNavigationTiming | undefined
        //console.log("[trace] logger mounted, url:", window.location.href, "navigation.type:", navEntry?.type ?? "unknown")

        const persistedMarker = sessionStorage.getItem("__trace_pageshow_persisted_at")
        if (persistedMarker) {
            //console.log("[trace] restored from BFCache before reload at:", persistedMarker)
            sessionStorage.removeItem("__trace_pageshow_persisted_at")
        }

        const reloadMarker = sessionStorage.getItem("__trace_bfcache_fix_reload_at")
        if (reloadMarker) {
            //console.log("[trace] bfcache-fix triggered reload at:", reloadMarker)
            sessionStorage.removeItem("__trace_bfcache_fix_reload_at")
        }

        const handler = (e: PageTransitionEvent) => {
            if (e.persisted) {
                sessionStorage.setItem("__trace_pageshow_persisted_at", new Date().toISOString())
            }
            //console.log("[trace] pageshow fired, persisted:", e.persisted)
        }

        const pageHideHandler = (e: PageTransitionEvent) => {
            //console.log("[trace] pagehide fired, persisted:", e.persisted, "url:", window.location.href)
        }

        const popStateHandler = () => {
            //console.log("[trace] popstate fired, url:", window.location.href)
        }

        const visibilityHandler = () => {
            //console.log("[trace] visibilitychange fired, state:", document.visibilityState, "url:", window.location.href)
        }

        window.addEventListener("pageshow", handler)
        window.addEventListener("pagehide", pageHideHandler)
        window.addEventListener("popstate", popStateHandler)
        document.addEventListener("visibilitychange", visibilityHandler)

        return () => {
            window.removeEventListener("pageshow", handler)
            window.removeEventListener("pagehide", pageHideHandler)
            window.removeEventListener("popstate", popStateHandler)
            document.removeEventListener("visibilitychange", visibilityHandler)
        }
    }, [])

    return null
}