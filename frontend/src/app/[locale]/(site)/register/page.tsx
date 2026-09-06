"use client";

import { z } from "zod";
import { useTranslations } from "next-intl";
import Link from "next/link";
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
import { EyeIcon, EyeOffIcon } from "lucide-react";
import {
  Dialog,
  DialogTrigger,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
  DialogClose,
} from "@/components/ui/dialog";
import { TermsContent } from "./_components/TermsContent";
import { PrivacyContent } from "./_components/PrivacyContent";
import useRegister from "./_hooks/registerHooks";

export default function RegisterPage() {
  const t = useTranslations();
  const {
    register,
    handleSubmit,
    errors,
    isSubmitting,
    passwordVisibility,
    passwordConfirmVisibility,
    togglePasswordVisibility,
    togglePasswordConfirmVisibility,
    onSubmit,
    showPasswordMatchHint,
    passwordMatch,
    REGISTER_REQUIRED
  } = useRegister();

  return (
    <div className="flex flex-col my-4">
      <form
        className="w-full h-full"
        onSubmit={handleSubmit(onSubmit)}
        noValidate
      >
        <FieldSet className="flex flex-col w-2/3 max-w-md gap-4 mx-auto">
          <FieldTitle className="text-center text-2xl font-bold">
            {t("register.title")}
          </FieldTitle>

          <Field>
            <FieldLabel htmlFor="name">
              {t("register.name")}
              {REGISTER_REQUIRED.has("name") && (
                <span className="text-red-500 text-sm ml-0.5">
                  {t("register.mustType")}
                </span>
              )}
            </FieldLabel>
            <FieldContent>
              <Input
                id="name"
                type="text"
                {...register("name")}
                aria-invalid={!!errors.name}
              />
            </FieldContent>
            {errors.name?.message && (
              <FieldError>{t(errors.name.message)}</FieldError>
            )}
          </Field>

          <Field>
            <FieldLabel htmlFor="email">
              {t("register.email")}
              {REGISTER_REQUIRED.has("email") && (
                <span className="text-red-500 text-sm ml-0.5">
                  {t("register.mustType")}
                </span>
              )}
            </FieldLabel>
            <FieldContent>
              <Input
                id="email"
                type="email"
                {...register("email")}
                aria-invalid={!!errors.email}
              />
            </FieldContent>
            {errors.email?.message && (
              <FieldError>{t(errors.email.message)}</FieldError>
            )}
          </Field>

          <Field>
            <FieldLabel htmlFor="password">
              {t("register.password")}
              {REGISTER_REQUIRED.has("password") && (
                <span className="text-red-500 text-sm ml-0.5">
                  {t("register.mustType")}
                </span>
              )}
            </FieldLabel>
            <FieldContent className="relative">
              <Input
                id="password"
                type={passwordVisibility ? "text" : "password"}
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
                  <EyeOffIcon className="size-4" />
                ) : (
                  <EyeIcon className="size-4" />
                )}
              </Button>
            </FieldContent>
            {errors.password?.message && (
              <FieldError>{t(errors.password.message)}</FieldError>
            )}
          </Field>

          <Field>
            <FieldLabel htmlFor="passwordConfirm">
              {t("register.passwordConfirm")}
              {REGISTER_REQUIRED.has("passwordConfirm") && (
                <span className="text-red-500 text-sm ml-0.5">
                  {t("register.mustType")}
                </span>
              )}
            </FieldLabel>
            <FieldContent className="relative">
              <Input
                id="passwordConfirm"
                type={passwordConfirmVisibility ? "text" : "password"}
                {...register("passwordConfirm")}
                aria-invalid={!!errors.passwordConfirm}
              />
              <Button
                variant="ghost"
                size="icon"
                type="button"
                className="absolute right-0 top-1/2 size-9 -translate-y-1/2"
                onClick={togglePasswordConfirmVisibility}
                aria-label={
                  passwordConfirmVisibility
                    ? t("login.hidePassword")
                    : t("login.showPassword")
                }
              >
                {passwordConfirmVisibility ? (
                  <EyeOffIcon className="size-4" />
                ) : (
                  <EyeIcon className="size-4" />
                )}
              </Button>
            </FieldContent>
            {showPasswordMatchHint ? (
              <p
                className={
                  passwordMatch
                    ? "text-sm text-green-600"
                    : "text-sm text-red-600"
                }
                role="status"
              >
                {t(
                  passwordMatch
                    ? "register.validation.passwordMatch"
                    : "register.validation.passwordNotMatch"
                )}
              </p>
            ) : errors.passwordConfirm?.message ? (
              <FieldError>
                {t(errors.passwordConfirm.message)}
              </FieldError>
            ) : null}
          </Field>
          <Field>
            <FieldLabel htmlFor="termsAgreed">
              {t("register.termsAgreed")}
            </FieldLabel>
            <FieldContent className="flex flex-row items-center gap-2">
              <Input
                id="termsAgreed"
                type="checkbox"
                {...register("termsAgreed")}
                aria-invalid={!!errors.termsAgreed}
                className="w-4 h-4"
                required={REGISTER_REQUIRED.has("termsAgreed")}
              />
              <Link href="/policy/use" target="_blank">
                <span className="text-sm text-center text-blue-500 hover:underline">{t("register.termsOfService")}</span>
              </Link>
            </FieldContent>
            {errors.termsAgreed?.message && (
              <FieldError>{t(errors.termsAgreed.message)}</FieldError>
            )}
          </Field>

          <Field>
            <FieldLabel htmlFor="privacyAgreed">
              {t("register.privacyAgreed")}
            </FieldLabel>
            <FieldContent className="flex flex-row items-center gap-2">
              <Input
                id="privacyAgreed"
                type="checkbox"
                {...register("privacyAgreed")}
                aria-invalid={!!errors.privacyAgreed}
                className="w-4 h-4"
                required={REGISTER_REQUIRED.has("privacyAgreed")}
              />
              <Link href="/policy/privacy" target="_blank">
                <span className="text-sm text-center text-blue-500 hover:underline">{t("register.privacyPolicy")}</span>
              </Link>
            </FieldContent>
            {errors.privacyAgreed?.message && (
              <FieldError>{t(errors.privacyAgreed.message)}</FieldError>
            )}
          </Field>

          <Button
            className="w-1/2 mx-auto"
            type="submit"
            disabled={isSubmitting}
          >
            {isSubmitting ? t("register.submitting") : t("register.button")}
          </Button>
        </FieldSet>
      </form>
      <Link
        className="text-sm text-center text-blue-500 hover:underline"
        href="/login"
      >
        {t("register.haveAccount")}
      </Link>
    </div>
  );
}

