import Link from "next/link";
import { Button } from "@/components/ui/button";
import { SidebarTrigger } from "@/components/ui/sidebar";
import AdminAlarmConnectionStatus from "./AdminAlarmConnectionStatus";
import AdminNotificationPrompt from "./AdminNotificationPrompt";

export default function AdminHeader() {
    return (
        <header className="border-b bg-blue-500">
            <div className="container mx-auto px-4 sm:px-6 lg:px-8 h-14 flex items-center justify-between gap-2 sm:gap-4">
                <SidebarTrigger />
                <div className="flex items-center gap-2 sm:gap-3">
                    <AdminAlarmConnectionStatus />
                    <AdminNotificationPrompt />
                    <Button asChild className="font-semibold text-base sm:text-lg shrink-0">
                        <Link href="/">
                            메인페이지로 복귀하기
                        </Link>
                    </Button>
                </div>
            </div>
        </header>
    );
}