"use client";

import { AppSidebar } from "./_components/AppSidebar";
import AdminHeader from "./_components/Header";
import { SidebarInset, SidebarProvider } from "@/components/ui/sidebar";
import { Toaster } from "sonner";
import ReactQueryProvider from "./_providers/ReactQueryProvider";
import AdminAccessGate from "./_components/AdminAccessGate";
import { useAdminAlarmSse } from "./_hooks/useAdminAlarmSse";

export default function AdminLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  useAdminAlarmSse();

  return (
    <AdminAccessGate>
      <ReactQueryProvider>
        <SidebarProvider>
          <AppSidebar />
          <SidebarInset>
            <AdminHeader />
            <main className="flex-1 container mx-auto px-4 sm:px-6 lg:px-8 py-4 sm:py-6 bg-gray-100">
              {children}
              <Toaster position="top-center" />
            </main>
          </SidebarInset>
        </SidebarProvider>
      </ReactQueryProvider>
    </AdminAccessGate>
  );
}
