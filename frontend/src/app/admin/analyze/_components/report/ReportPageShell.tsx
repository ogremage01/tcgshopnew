import { ReactNode } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import Link from "next/link";

interface Props {
  title: string;
  error: string | null;
  loaded: boolean;
  children: ReactNode;
  backTab?: "daily" | "weekly" | "monthly";
}

export function ReportPageShell({ title, error, loaded, children, backTab }: Props) {
  const listHref = backTab
    ? `/admin/analyze/sales-report?tab=${backTab}`
    : "/admin/analyze/sales-report";

  if (error) {
    return (
      <div className="w-[1280px] mx-auto">
        <Card>
          <CardContent className="py-8 text-center text-destructive">{error}</CardContent>
        </Card>
      </div>
    );
  }

  if (!loaded) {
    return (
      <div className="w-[1280px] mx-auto">
        <Card>
          <CardContent className="py-8 text-center">Loading...</CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="w-[1280px] mx-auto">
      <Card>
        <CardHeader className="flex flex-row justify-between">
          <CardTitle>{title}</CardTitle>
          <Button variant="default" asChild>
            <Link href={listHref}>목록 보기</Link>
          </Button>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">{children}</CardContent>
      </Card>
    </div>
  );
}
