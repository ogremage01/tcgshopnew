import { z } from "zod";
import { useTranslations } from "next-intl";
import { useRouter } from "@/i18n/navigation";
import { useState } from "react";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { api, getApiErrorMessage } from "@/lib/api";
import { translateApiErrorForUser } from "@/lib/i18n-api-error";

/** 회원가입 요청 스키마. optinal=선택 입력 사항 */
const registerSchema = z
    .object({
        name: z.string().min(1, "register.validation.nameRequired"),
        email: z
            .string()
            .min(1, "register.validation.emailRequired")
            .email("register.validation.emailInvalid"),
        password: z.string().min(1, "register.validation.passwordRequired"),
        passwordConfirm: z
            .string()
            .min(1, "register.validation.passwordConfirmRequired"),
        termsAgreed: z
            .boolean()
            .refine((v) => v === true, { message: "register.validation.agreementRequired" }),
        privacyAgreed: z
            .boolean()
            .refine((v) => v === true, { message: "register.validation.agreementRequired" }),
    })
    .refine((data) => data.password === data.passwordConfirm, {
        message: "register.validation.passwordNotMatch",
        path: ["passwordConfirm"],
    });

/** 회원가입 폼 값 타입 */
type RegisterFormValues = z.infer<typeof registerSchema>;

/** 필수 입력 필드 (라벨 * 표시용). 검증은 registerSchema에서 함 */
const REGISTER_REQUIRED = new Set<keyof RegisterFormValues>([
    "name",
    "email",
    "password",
    "passwordConfirm",
]);

export default function useRegister() {
    const t = useTranslations();
    const router = useRouter();
    const [passwordVisibility, setPasswordVisibility] = useState(false);
    const [passwordConfirmVisibility, setPasswordConfirmVisibility] = useState(false);

    const togglePasswordVisibility = () => {
        setPasswordVisibility(!passwordVisibility);
    };
    const togglePasswordConfirmVisibility = () => {
        setPasswordConfirmVisibility(!passwordConfirmVisibility);
    };

    const {
        register,
        handleSubmit,
        control,
        formState: { errors, isSubmitting },
    } = useForm<RegisterFormValues>({
        defaultValues: {
            name: "",
            email: "",
            password: "",
            passwordConfirm: "",
            termsAgreed: false,
            privacyAgreed: false,
        },
        resolver: zodResolver(registerSchema),
    });

    const password = useWatch({ control, name: "password" }) ?? "";
    const passwordConfirm = useWatch({ control, name: "passwordConfirm" }) ?? "";
    const showPasswordMatchHint = password.length > 0 && passwordConfirm.length > 0;
    const passwordMatch = password === passwordConfirm;

    const onSubmit = async (data: RegisterFormValues) => {
        try {
            await api.post("/api/auth/register", data);
            alert(t("register.success"));
            router.push("/login");
        } catch (err) {
            alert(translateApiErrorForUser(t, getApiErrorMessage(err), "register.error"));
        }
    };
    return {
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
        registerSchema,
        REGISTER_REQUIRED,
    };
}