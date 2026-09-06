"use client"

import { forwardRef, useImperativeHandle, useRef } from "react"
import { useEditor, EditorContent } from "@tiptap/react"
import { StarterKit } from "@tiptap/starter-kit"
import ImageExtension from "@tiptap/extension-image"
import {
  Bold,
  Italic,
  Heading2,
  Heading3,
  List,
  ListOrdered,
  ImageIcon,
  Strikethrough,
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { api } from "@/lib/api"

export type TiptapEditorHandle = {
  getHTML: () => string
}

type TiptapEditorProps = {
  initialContent?: string
}

export const TiptapEditor = forwardRef<TiptapEditorHandle, TiptapEditorProps>(
  function TiptapEditor({ initialContent }, ref) {
    const fileInputRef = useRef<HTMLInputElement>(null)

    const editor = useEditor({
      extensions: [
        StarterKit,
        ImageExtension.configure({ inline: false }),
      ],
      content: initialContent ?? "",
      immediatelyRender: false,
      editorProps: {
        attributes: {
          class:
            "min-h-[300px] p-3 focus:outline-none prose prose-sm max-w-none dark:prose-invert",
        },
      },
    })

    useImperativeHandle(ref, () => ({
      getHTML: () => editor?.getHTML() ?? "",
    }))

    const handleImageUpload = async (file: File) => {
      const formData = new FormData()
      formData.append("image", file)
      try {
        const res = await api.postFormData<{ success: number; file: { url: string } }>(
          "/api/admin/site-setting/upload/main-image",
          formData,
        )
        if (res.success === 1 && editor) {
          editor.chain().focus().setImage({ src: res.file.url }).run()
        }
      } catch {
        alert("이미지 업로드에 실패했습니다")
      }
    }

    const ToolbarButton = ({
      onClick,
      active,
      children,
    }: {
      onClick: () => void
      active?: boolean
      children: React.ReactNode
    }) => (
      <Button
        type="button"
        variant={active ? "secondary" : "ghost"}
        size="icon"
        className="h-7 w-7"
        onClick={onClick}
      >
        {children}
      </Button>
    )

    const Divider = () => <div className="w-px h-5 bg-border mx-0.5 self-center" />

    return (
      <div className="border rounded-md overflow-hidden">
        <div className="flex flex-wrap items-center gap-0.5 border-b bg-muted/40 p-1">
          <ToolbarButton
            active={editor?.isActive("bold")}
            onClick={() => editor?.chain().focus().toggleBold().run()}
          >
            <Bold className="h-3.5 w-3.5" />
          </ToolbarButton>
          <ToolbarButton
            active={editor?.isActive("italic")}
            onClick={() => editor?.chain().focus().toggleItalic().run()}
          >
            <Italic className="h-3.5 w-3.5" />
          </ToolbarButton>
          <ToolbarButton
            active={editor?.isActive("strike")}
            onClick={() => editor?.chain().focus().toggleStrike().run()}
          >
            <Strikethrough className="h-3.5 w-3.5" />
          </ToolbarButton>

          <Divider />

          <ToolbarButton
            active={editor?.isActive("heading", { level: 2 })}
            onClick={() => editor?.chain().focus().toggleHeading({ level: 2 }).run()}
          >
            <Heading2 className="h-3.5 w-3.5" />
          </ToolbarButton>
          <ToolbarButton
            active={editor?.isActive("heading", { level: 3 })}
            onClick={() => editor?.chain().focus().toggleHeading({ level: 3 }).run()}
          >
            <Heading3 className="h-3.5 w-3.5" />
          </ToolbarButton>

          <Divider />

          <ToolbarButton
            active={editor?.isActive("bulletList")}
            onClick={() => editor?.chain().focus().toggleBulletList().run()}
          >
            <List className="h-3.5 w-3.5" />
          </ToolbarButton>
          <ToolbarButton
            active={editor?.isActive("orderedList")}
            onClick={() => editor?.chain().focus().toggleOrderedList().run()}
          >
            <ListOrdered className="h-3.5 w-3.5" />
          </ToolbarButton>

          <Divider />

          <ToolbarButton onClick={() => fileInputRef.current?.click()}>
            <ImageIcon className="h-3.5 w-3.5" />
          </ToolbarButton>
          <input
            ref={fileInputRef}
            type="file"
            accept=".webp,.png,.jpg,.jpeg"
            className="hidden"
            onChange={(e) => {
              const file = e.target.files?.[0]
              if (file) handleImageUpload(file)
              e.target.value = ""
            }}
          />
        </div>

        <EditorContent editor={editor} />
      </div>
    )
  },
)
