"use client";

// import { MessageCircleMore } from "lucide-react";
import Image from "next/image";
import { Button } from "@/components/ui/button";
import { resolveAssetUrl } from "@/lib/public-asset-url";
import WindowPortal from "@/components/WindowPortal";

const CONTACT_LINKS = [
  {
    label: "discord",
    href: "https://discord.gg/HxyytRWfz",
    src: "/uploads/assets/Discord-Symbol-Blurple.svg",
  },
  {
    label: "facebook",
    href: "https://www.facebook.com/profile.php?id=61590560613768",
    src: "/uploads/assets/Facebook_Logo_Primary.png",
  },
] as const;

export default function Contact() {
  return (
    <WindowPortal>
      <div className="fixed bottom-4 right-4 z-50 pointer-events-auto">
        <div className="flex flex-col items-end gap-2">
          {CONTACT_LINKS.map(({ label, href, src }) => (
            <Button
              key={label}
              type="button"
              variant="outline"
              size="icon"
              className="shadow-md"
              aria-label={label}
              onClick={() => window.open(href, "_blank")}
            >
              <Image
                src={resolveAssetUrl(src)}
                alt={label}
                width={20}
                height={20}
                className="size-5"
              />
            </Button>
          ))}

          {/* <Button
            type="button"
            variant="outline"
            size="icon"
            className="shadow-md"
            aria-label="contact"
          >
            <MessageCircleMore />
          </Button> */}
        </div>
      </div>
    </WindowPortal>
  );
}
