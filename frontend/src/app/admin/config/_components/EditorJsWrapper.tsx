"use client"

import { forwardRef, useEffect, useImperativeHandle, useRef } from "react"
import type EditorJS from "@editorjs/editorjs"
import type { OutputData } from "@editorjs/editorjs"

export type EditorJsWrapperHandle = {
  save: () => Promise<OutputData>
}

type EditorJsWrapperProps = {
  holderId: string
  initialData?: OutputData
}

export const EditorJsWrapper = forwardRef<EditorJsWrapperHandle, EditorJsWrapperProps>(
  function EditorJsWrapper({ holderId, initialData }, ref) {
    const editorRef = useRef<EditorJS | null>(null)
    const isInitialized = useRef(false)

    useImperativeHandle(ref, () => ({
      save: async () => {
        if (!editorRef.current) throw new Error("EditorJS not initialized")
        return editorRef.current.save()
      },
    }))

    useEffect(() => {
      if (isInitialized.current) return
      isInitialized.current = true

      let isCancelled = false
      let editor: EditorJS | null = null

      const init = async () => {
        const EditorJSModule = (await import("@editorjs/editorjs")).default
        const Header = (await import("@editorjs/header")).default
        const List = (await import("@editorjs/list")).default
        const ImageTool = (await import("@editorjs/image")).default

        editor = new EditorJSModule({
          holder: holderId,
          data: initialData,
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          tools: {
            header: {
              class: Header as any,
              inlineToolbar: true,
            },
            list: {
              class: List as any,
              inlineToolbar: true,
            },
            image: {
              class: ImageTool as any,
              config: {
                endpoints: {
                  byFile: "/api/admin/site-setting/upload/main-image",
                },
              },
            },
          },
        })

        await editor.isReady
        // cleanup이 먼저 실행된 경우 stale 인스턴스로 ref를 덮어쓰지 않음
        if (!isCancelled) {
          editorRef.current = editor
        }
      }

      init().catch(console.error)

      return () => {
        isCancelled = true
        editor?.destroy?.()
        editorRef.current = null
        isInitialized.current = false
      }
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [holderId])

    return <div id={holderId} className="border rounded-md min-h-[300px] p-3 prose max-w-none" />
  },
)
