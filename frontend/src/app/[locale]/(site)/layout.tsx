import type { ReactNode } from "react";
import { Suspense } from "react";
import Header from "@/components/header/Header";
import Footer from "@/components/Footer";
import AuthSessionRestorer from "@/app/_components/AuthSessionRestorer";
import Cart from "@/components/cart/cart";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import Providers from "@/app/Providers";
import Contact from "@/app/_components/Contact";
export default function SiteLayout({
  children,
}: Readonly<{ children: ReactNode }>) {
  const queryClient = new QueryClient();
  return (
    <Providers>
      <Suspense
        fallback={
          <div className="h-14 border-b bg-blue-500 shrink-0" aria-hidden />
        }
      >
        <Header />
      </Suspense>
      <AuthSessionRestorer />
      <div className="flex flex-row gap-4">
        <main className="flex-1 container mx-auto px-4 sm:px-6 lg:px-8 flex flex-col gap-4">
          {children}
        </main>
      </div>
      <Contact />
      <Cart />
      <Footer />
    </Providers>
  );
}
