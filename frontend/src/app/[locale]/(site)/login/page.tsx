"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import {
  Field,
  FieldContent,
  FieldError,
  FieldLabel,
  FieldSet,
  FieldTitle,
} from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { useTranslations } from "next-intl";
import { useRouter } from "@/i18n/navigation";
import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { api, getApiErrorMessage } from "@/lib/api";
import { translateApiErrorForUser } from "@/lib/i18n-api-error";
import { useAuthStore } from "@/stores/auth-store";
import type { AuthResponseDto } from "@/types/user";
import { userFromAuthDto } from "@/types/user";
import { EyeIcon, EyeOffIcon } from "lucide-react";
import Link from "next/link";

const loginSchema = z.object({
  email: z
    .string()
    .min(1, "login.validation.emailRequired")
    .email("login.validation.emailInvalid"),
  password: z.string().min(1, "login.validation.passwordRequired"),
});

type LoginFormValues = z.infer<typeof loginSchema>;

export default function LoginPage() {
  const router = useRouter();
  const t = useTranslations();
  const setAuth = useAuthStore((s) => s.setAuth);
  const queryClient = useQueryClient();
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [passwordVisibility, setPasswordVisibility] = useState(false);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: "", password: "" },
  });

  const togglePasswordVisibility = () => {
    setPasswordVisibility(!passwordVisibility);
  };

  const onSubmit = async (values: LoginFormValues) => {
    setSubmitError(null);
    try {
      const result = await api.post<AuthResponseDto & { message?: string }>("/api/auth/login", values);
      if (result?.token && result?.user) {
        setAuth(result.token, userFromAuthDto(result.user));
        await queryClient.invalidateQueries({ queryKey: ["cart"] });
        router.push("/");
      } else {
        setSubmitError(result?.message ?? t("login.error"));
      }
    } catch (err) {
      setSubmitError(translateApiErrorForUser(t, getApiErrorMessage(err), "login.error"));
    }
  };

  return (
    <div className="flex flex-col my-4">
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <FieldSet className="flex flex-col w-2/3 max-w-md gap-4 mx-auto">
          <FieldTitle className="text-center text-2xl font-bold">
            {t("login.title")}
          </FieldTitle>

          {submitError && (
            <p className="text-sm text-destructive text-center" role="alert">
              {submitError}
            </p>
          )}

          <Field>
            <FieldLabel htmlFor="email">{t("login.email")}</FieldLabel>
            <FieldContent>
              <Input
                id="email"
                type="email"
                autoComplete="email"
                {...register("email")}
                aria-invalid={!!errors.email}
              />
            </FieldContent>
            {errors.email?.message && (
              <FieldError>{t(errors.email.message as string)}</FieldError>
            )}
          </Field>

          <Field>
            <FieldLabel htmlFor="password">{t("login.password")}</FieldLabel>
            <FieldContent className="relative">
              <Input
                id="password"
                type={passwordVisibility ? "text" : "password"}
                autoComplete="current-password"
                className="pr-10"
                {...register("password")}
                aria-invalid={!!errors.password}
              />
              <Button
                variant="ghost"
                size="icon"
                type="button"
                className="absolute right-0 top-1/2 size-9 -translate-y-1/2"
                onClick={togglePasswordVisibility}
                aria-label={
                  passwordVisibility ? t("login.hidePassword") : t("login.showPassword")
                }
              >
                {passwordVisibility ? (
                  <EyeIcon className="size-4" />
                ) : (
                  <EyeOffIcon className="size-4" />
                )}
              </Button>
            </FieldContent>
            {errors.password?.message && (
              <FieldError>
                {t(errors.password.message as string)}
              </FieldError>
            )}
          </Field>

          <Button className="w-1/2 mx-auto" type="submit" disabled={isSubmitting}>
            {isSubmitting ? t("login.submitting") : t("login.button")}
          </Button>
          <Button
            className="w-1/2 mx-auto"
            variant="secondary"
            type="button"
            onClick={() => router.push("/register")}
          >
            {t("login.registerButton")}
          </Button>
        </FieldSet>
      </form>

      <Link href="/login/forgot-password" className="text-sm text-center text-blue-500 hover:underline">
        {t("login.forgotPassword")}
      </Link>
    </div>
  );
}

