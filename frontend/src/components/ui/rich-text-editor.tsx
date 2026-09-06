"use client"

import * as React from "react"

import Placeholder from "@tiptap/extension-placeholder"
import { EditorContent, useEditor, type Editor } from "@tiptap/react"
import StarterKit from "@tiptap/starter-kit"
import {
  Bold as BoldIcon,
  Italic as ItalicIcon,
  List as ListIcon,
  ListOrdered as ListOrderedIcon,
  Redo as RedoIcon,
  Undo as UndoIcon,
} from "lucide-react"

import { Separator } from "@/components/ui/separator"
import { Toggle } from "@/components/ui/toggle"
import { cn } from "@/lib/utils"

export type RichTextEditorProps = {
  name?: string
  value?: string
  defaultValue?: string
  onChange?: (html: string) => void
  placeholder?: string
  className?: string
  editorClassName?: string
  disabled?: boolean
  maxLength?: number
}

function EditorToolbar({
  editor,
  disabled,
}: {
  editor: Editor | null
  disabled?: boolean
}) {
  if (!editor) return null

  return (
    <div className="flex flex-wrap items-center gap-1 border-b border-input bg-muted/30 p-1">
      <Toggle
        size="sm"
        pressed={editor.isActive("bold")}
        onPressedChange={() => editor.chain().focus().toggleBold().run()}
        disabled={disabled}
        aria-label="굵게"
      >
        <BoldIcon />
      </Toggle>
      <Toggle
        size="sm"
        pressed={editor.isActive("italic")}
        onPressedChange={() => editor.chain().focus().toggleItalic().run()}
        disabled={disabled}
        aria-label="기울임"
      >
        <ItalicIcon />
      </Toggle>
      <Separator orientation="vertical" className="mx-1 h-6" />
      <Toggle
        size="sm"
        pressed={editor.isActive("bulletList")}
        onPressedChange={() => editor.chain().focus().toggleBulletList().run()}
        disabled={disabled}
        aria-label="글머리 기호 목록"
      >
        <ListIcon />
      </Toggle>
      <Toggle
        size="sm"
        pressed={editor.isActive("orderedList")}
        onPressedChange={() => editor.chain().focus().toggleOrderedList().run()}
        disabled={disabled}
        aria-label="번호 매기기 목록"
      >
        <ListOrderedIcon />
      </Toggle>
      <Separator orientation="vertical" className="mx-1 h-6" />
      <Toggle
        size="sm"
        onPressedChange={() => editor.chain().focus().undo().run()}
        disabled={disabled || !editor.can().chain().focus().undo().run()}
        aria-label="실행 취소"
      >
        <UndoIcon />
      </Toggle>
      <Toggle
        size="sm"
        onPressedChange={() => editor.chain().focus().redo().run()}
        disabled={disabled || !editor.can().chain().focus().redo().run()}
        aria-label="다시 실행"
      >
        <RedoIcon />
      </Toggle>
    </div>
  )
}

export function RichTextEditor({
  name,
  value,
  defaultValue,
  onChange,
  placeholder,
  className,
  editorClassName,
  disabled,
  maxLength,
}: RichTextEditorProps) {
  const isControlled = value !== undefined
  const [internalHtml, setInternalHtml] = React.useState(defaultValue ?? "")
  const currentHtml = isControlled ? (value ?? "") : internalHtml
  const previousHtmlRef = React.useRef(currentHtml)

  const editor = useEditor({
    immediatelyRender: false,
    editable: !disabled,
    content: currentHtml,
    extensions: [
      StarterKit,
      Placeholder.configure({
        placeholder: placeholder ?? "",
        emptyEditorClass: "is-editor-empty",
      }),
    ],
    editorProps: {
      attributes: {
        class: cn(
          "rich-text-editor-content min-h-[140px] w-full px-3 py-2 text-sm outline-none",
          editorClassName,
        ),
      },
    },
    onUpdate({ editor }) {
      const html = editor.isEmpty ? "" : editor.getHTML()
      if (maxLength !== undefined && html.length > maxLength) {
        editor.commands.setContent(previousHtmlRef.current, { emitUpdate: false })
        return
      }
      previousHtmlRef.current = html
      if (!isControlled) setInternalHtml(html)
      onChange?.(html)
    },
  })

  React.useEffect(() => {
    previousHtmlRef.current = currentHtml
  }, [currentHtml])

  React.useEffect(() => {
    if (!editor || !isControlled) return
    const current = editor.getHTML()
    const next = value ?? ""
    if (next !== current) {
      editor.commands.setContent(next, { emitUpdate: false })
      previousHtmlRef.current = next
    }
  }, [editor, isControlled, value])

  React.useEffect(() => {
    if (!editor) return
    editor.setEditable(!disabled)
  }, [editor, disabled])

  return (
    <div
      className={cn(
        "flex w-full flex-col rounded-md border border-input bg-background",
        "focus-within:ring-2 focus-within:ring-ring focus-within:ring-offset-2 focus-within:ring-offset-background",
        disabled && "opacity-50",
        className,
      )}
    >
      <EditorToolbar editor={editor} disabled={disabled} />
      <EditorContent editor={editor} />
      {maxLength !== undefined ? (
        <p
          className={cn(
            "border-t border-input px-3 py-1 text-right text-xs text-muted-foreground",
            currentHtml.length >= maxLength && "text-destructive",
          )}
        >
          {currentHtml.length}/{maxLength}
        </p>
      ) : null}
      {name ? (
        <input type="hidden" name={name} value={currentHtml} readOnly />
      ) : null}
    </div>
  )
}

export default RichTextEditor
