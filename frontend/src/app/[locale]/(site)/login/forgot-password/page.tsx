"use client";

import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { useState } from "react";

export default function ForgotPasswordPage() {
  const [copied, setCopied] = useState(false);
    const email = "support@example.com";

  const handleCopy = () => {
    navigator.clipboard.writeText(email);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div>
      <Card className="w-full">
        <CardHeader>
          <CardTitle>Forgot Password</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            <span className="block">
              관리자에게 문의하여 비밀번호를 초기화하세요. 관리자 연락처:
              support@example.com
            </span>
            <div className="flex items-center gap-2">
              <span className="font-mono font-semibold">{email}</span>
              <Button onClick={handleCopy} variant="outline" size="sm">
                {copied ? "복사됨!" : "복사"}
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
