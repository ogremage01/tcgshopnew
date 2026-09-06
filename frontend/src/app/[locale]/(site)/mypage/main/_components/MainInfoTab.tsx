import {
  Card,
  CardHeader,
  CardContent,
  CardDescription,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { useLocale, useTranslations } from "next-intl";
import type { UserResponseDto } from "@/types/user";
import { api, getApiErrorMessage } from "@/lib/api";
import OrderSimpleList from "./OrderSimpleList";
import { OrderSimpleDto } from "@/types/order";
import { useState, useEffect } from "react";
import { Dialog, DialogTrigger, DialogClose } from "@/components/ui/dialog";
import {
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Field, FieldContent, FieldLabel } from "@/components/ui/field";
import { EyeIcon, EyeOffIcon } from "lucide-react";

const TM = "mypage.main";

export default function MainInfo({
  user,
}: {
  user: UserResponseDto;
}) {
  const t = useTranslations();
  const locale = useLocale();
  const [latestFiveOrders, setLatestFiveOrders] = useState<OrderSimpleDto[]>(
    [],
  );
  const [currentPassword, setCurrentPassword] = useState("");
  const [password, setPassword] = useState("");
  const [passwordConfirm, setPasswordConfirm] = useState("");
  const [currentPasswordVisible, setCurrentPasswordVisible] = useState(false);
  const [passwordVisible, setPasswordVisible] = useState(false);
  const [passwordConfirmVisible, setPasswordConfirmVisible] = useState(false);
  useEffect(() => {
    api
      .get<OrderSimpleDto[]>("/api/user/orders/latest")
      .then((response) => {
        setLatestFiveOrders(response);
      })
      .catch((error) => {
        alert(getApiErrorMessage(error) ?? t("common.error"));
      });
  }, []);

  function handleNameChange(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const form = e.currentTarget;
    const formData = new FormData(form);
    const name = formData.get("name") as string;
    api
      .post("/api/user/change-name", {
        name: name,
      })
      .then(() => {
        alert(t(`${TM}.personalInfo.nameChangeSuccess`));
      })
      .catch((error) => {
        alert(getApiErrorMessage(error) ?? t("common.error"));
      });
  }
  function handlePasswordChange(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (password !== passwordConfirm) {
      alert(t(`${TM}.personalInfo.passwordConfirmMismatch`));
      return;
    }
    api
      .post("/api/user/change-password", {
        currentPassword: currentPassword,
        password: password,
        passwordConfirm: passwordConfirm,
      })
      .then(() => {
        alert(t(`${TM}.personalInfo.passwordChangeSuccess`));
        setCurrentPassword("");
        setPassword("");
        setPasswordConfirm("");
      })
      .catch((error) => {
        alert(getApiErrorMessage(error) ?? t("common.error"));
      });
  }
  const showPasswordMatchHint =
    password.length > 0 && passwordConfirm.length > 0;
  const passwordMatch = password === passwordConfirm;
  return (
    <div className="flex flex-col gap-4">
      <Card>
        <CardHeader>
          <span className="text-2xl font-bold">{t(`${TM}.personalInfo.title`)}</span>
        </CardHeader>
        <CardContent>
          <div className="flex flex-col gap-2">
            <div className="flex flex-row items-center gap-2">
              <span>{t(`${TM}.personalInfo.name`)}</span>
              <span>{user?.name}</span>
              <Dialog>
                <DialogTrigger asChild>
                  <Button variant="outline" size="sm">
                    {t(`${TM}.personalInfo.edit`)}
                  </Button>
                </DialogTrigger>
                <DialogContent>
                  <DialogHeader>
                    <DialogTitle>{t(`${TM}.personalInfo.editName`)}</DialogTitle>
                  </DialogHeader>
                  <form
                    id="name-change-form"
                    className="flex flex-col gap-2"
                    onSubmit={(e) => handleNameChange(e)}
                  >
                    <Input name="name" type="text" defaultValue={user?.name} />
                  </form>
                  <DialogFooter>
                    <DialogClose asChild>
                      <Button variant="outline" size="sm">
                        {t("common.cancel")}
                      </Button>
                    </DialogClose>
                    <Button
                      type="submit"
                      form="name-change-form"
                      variant="default"
                      size="sm"
                    >
                      {t(`${TM}.button.save`)}
                    </Button>
                  </DialogFooter>
                </DialogContent>
              </Dialog>
            </div>
            <div className="flex flex-row items-center gap-2">
              <span>{t(`${TM}.personalInfo.registeredEmail`)}</span>
              <span>{user?.email}</span>
            </div>
            <div className="flex flex-row gap-2">
              <span>{t(`${TM}.personalInfo.rewardPoint`)}</span>
              <span>Pts {user?.point.toLocaleString(locale)}</span>
            </div>
            <Dialog>
              <DialogTrigger asChild>
                <Button variant="default" size="sm" className="w-fit">
                  {t(`${TM}.personalInfo.editPassword`)}
                </Button>
              </DialogTrigger>
              <DialogContent>
                <DialogHeader>
                  <DialogTitle>{t(`${TM}.personalInfo.editPassword`)}</DialogTitle>
                </DialogHeader>
                <form
                  id="password-change-form"
                  className="flex flex-col gap-3"
                  onSubmit={(e) => handlePasswordChange(e)}
                >
                  <Field>
                    <FieldLabel htmlFor="currentPassword">
                      {t(`${TM}.personalInfo.currentPassword`)}
                    </FieldLabel>
                    <FieldContent className="relative">
                      <Input
                        id="currentPassword"
                        name="currentPassword"
                        type={currentPasswordVisible ? "text" : "password"}
                        value={currentPassword}
                        onChange={(e) => setCurrentPassword(e.target.value)}
                        required
                      />
                      <Button
                        variant="ghost"
                        size="icon"
                        type="button"
                        className="absolute right-0 top-1/2 size-9 -translate-y-1/2"
                        onClick={() =>
                          setCurrentPasswordVisible((prev) => !prev)
                        }
                        aria-label={
                          currentPasswordVisible
                            ? t("login.hidePassword")
                            : t("login.showPassword")
                        }
                      >
                        {currentPasswordVisible ? (
                          <EyeOffIcon className="size-4" />
                        ) : (
                          <EyeIcon className="size-4" />
                        )}
                      </Button>
                    </FieldContent>
                  </Field>
                  <Field>
                    <FieldLabel htmlFor="password">{t(`${TM}.personalInfo.newPassword`)}</FieldLabel>
                    <FieldContent className="relative">
                      <Input
                        id="password"
                        name="password"
                        type={passwordVisible ? "text" : "password"}
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        required
                      />
                      <Button
                        variant="ghost"
                        size="icon"
                        type="button"
                        className="absolute right-0 top-1/2 size-9 -translate-y-1/2"
                        onClick={() => setPasswordVisible((prev) => !prev)}
                        aria-label={
                          passwordVisible
                            ? t("login.hidePassword")
                            : t("login.showPassword")
                        }
                      >
                        {passwordVisible ? (
                          <EyeOffIcon className="size-4" />
                        ) : (
                          <EyeIcon className="size-4" />
                        )}
                      </Button>
                    </FieldContent>
                  </Field>
                  <Field>
                    <FieldLabel htmlFor="passwordConfirm">
                      {t(`${TM}.personalInfo.newPasswordConfirm`)}
                    </FieldLabel>
                    <FieldContent className="relative">
                      <Input
                        id="passwordConfirm"
                        name="passwordConfirm"
                        type={passwordConfirmVisible ? "text" : "password"}
                        value={passwordConfirm}
                        onChange={(e) => setPasswordConfirm(e.target.value)}
                        required
                      />
                      <Button
                        variant="ghost"
                        size="icon"
                        type="button"
                        className="absolute right-0 top-1/2 size-9 -translate-y-1/2"
                        onClick={() =>
                          setPasswordConfirmVisible((prev) => !prev)
                        }
                        aria-label={
                          passwordConfirmVisible
                            ? t("login.hidePassword")
                            : t("login.showPassword")
                        }
                      >
                        {passwordConfirmVisible ? (
                          <EyeOffIcon className="size-4" />
                        ) : (
                          <EyeIcon className="size-4" />
                        )}
                      </Button>
                    </FieldContent>
                    {showPasswordMatchHint && (
                      <p
                        className={
                          passwordMatch
                            ? "text-sm text-green-600"
                            : "text-sm text-red-600"
                        }
                        role="status"
                      >
                        {passwordMatch
                          ? t(`${TM}.personalInfo.passwordMatch`)
                          : t(`${TM}.personalInfo.passwordMismatch`)}
                      </p>
                    )}
                  </Field>
                </form>
                <DialogFooter>
                  <DialogClose asChild>
                    <Button variant="outline" size="sm">
                      {t("common.cancel")}
                    </Button>
                  </DialogClose>
                  <Button
                    type="submit"
                    form="password-change-form"
                    variant="default"
                    size="sm"
                  >
                    {t(`${TM}.button.save`)}
                  </Button>
                </DialogFooter>
              </DialogContent>
            </Dialog>
          </div>
        </CardContent>
      </Card>
      <Card>
        <CardHeader>
          <span className="text-2xl font-bold">{t(`${TM}.orderOverview.title`)}</span>
          <CardDescription>{t(`${TM}.orderOverview.description`)}</CardDescription>
        </CardHeader>
        <CardContent>
          <OrderSimpleList orders={latestFiveOrders} />
        </CardContent>
      </Card>
    </div>
  );
}
