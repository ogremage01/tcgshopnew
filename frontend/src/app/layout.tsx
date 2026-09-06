import { Noto_Sans_KR } from "next/font/google";
import { getLocale } from "next-intl/server";
import "./_styles/globals.css";
import { Toaster } from "sonner";
import DisableBFCache from "./_components/DisableBFCache";
import BodyPointerEventsReset from "./_components/BodyPointerEventsReset";

const notoSansKR = Noto_Sans_KR({
  weight: ["400", "700"],
  subsets: ["latin"],
});

export default async function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  const locale = await getLocale();

  return (
    <html lang={locale} suppressHydrationWarning>
      <body className={`${notoSansKR.className} antialiased`}>
        <DisableBFCache />
        <BodyPointerEventsReset />
        <div className="min-h-screen flex flex-col">{children}</div>
        <Toaster position="top-center" />
      </body>
    </html>
  );
}
