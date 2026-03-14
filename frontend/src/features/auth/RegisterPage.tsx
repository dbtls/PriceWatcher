import { useNavigate, Link } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation } from "@tanstack/react-query";
import { authApi } from "@/services/authApi";
import { getApiErrorMessage } from "@/lib/api";
import { useToastStore } from "@/stores/toastStore";
import { Button } from "@/components/ui/Button";

const schema = z
  .object({
    email: z.string().email("올바른 이메일을 입력하세요"),
    password: z.string().min(8, "비밀번호는 8자 이상이어야 합니다"),
    passwordConfirm: z.string(),
    nickname: z.string().min(1, "닉네임을 입력하세요"),
  })
  .refine((data) => data.password === data.passwordConfirm, {
    message: "비밀번호가 일치하지 않습니다",
    path: ["passwordConfirm"],
  });

type FormData = z.infer<typeof schema>;

export function RegisterPage() {
  const navigate = useNavigate();
  const addToast = useToastStore((s) => s.add);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { email: "", password: "", passwordConfirm: "", nickname: "" },
  });

  const registerMutation = useMutation({
    mutationFn: (data: FormData) =>
      authApi.register({
        email: data.email,
        password: data.password,
        nickname: data.nickname,
      }),
    onSuccess: () => {
      addToast("회원가입이 완료되었습니다. 로그인해 주세요.", "success");
      navigate("/login", { replace: true });
    },
    onError: (err) => addToast(getApiErrorMessage(err), "error"),
  });

  return (
    <div className="max-w-[400px] mx-auto px-6 py-12">
      <h1 className="text-2xl font-bold text-text-main mb-6">회원가입</h1>
      <form
        onSubmit={handleSubmit((data) => registerMutation.mutate(data))}
        className="flex flex-col gap-4"
      >
        <div>
          <label htmlFor="email" className="block text-sm font-medium text-text-main mb-1">
            이메일
          </label>
          <input
            id="email"
            type="email"
            {...register("email")}
            className="w-full px-4 py-3 rounded-xl border-2 border-border focus:border-primary focus:outline-none bg-white dark:bg-[var(--surface)] text-text-main"
            placeholder="email@example.com"
          />
          {errors.email && (
            <p className="mt-1 text-sm text-red-600">{errors.email.message}</p>
          )}
        </div>
        <div>
          <label htmlFor="nickname" className="block text-sm font-medium text-text-main mb-1">
            닉네임
          </label>
          <input
            id="nickname"
            type="text"
            {...register("nickname")}
            className="w-full px-4 py-3 rounded-xl border-2 border-border focus:border-primary focus:outline-none bg-white dark:bg-[var(--surface)] text-text-main"
            placeholder="닉네임"
          />
          {errors.nickname && (
            <p className="mt-1 text-sm text-red-600">{errors.nickname.message}</p>
          )}
        </div>
        <div>
          <label htmlFor="password" className="block text-sm font-medium text-text-main mb-1">
            비밀번호
          </label>
          <input
            id="password"
            type="password"
            {...register("password")}
            className="w-full px-4 py-3 rounded-xl border-2 border-border focus:border-primary focus:outline-none bg-white dark:bg-[var(--surface)] text-text-main"
            placeholder="8자 이상"
          />
          {errors.password && (
            <p className="mt-1 text-sm text-red-600">{errors.password.message}</p>
          )}
        </div>
        <div>
          <label htmlFor="passwordConfirm" className="block text-sm font-medium text-text-main mb-1">
            비밀번호 확인
          </label>
          <input
            id="passwordConfirm"
            type="password"
            {...register("passwordConfirm")}
            className="w-full px-4 py-3 rounded-xl border-2 border-border focus:border-primary focus:outline-none bg-white dark:bg-[var(--surface)] text-text-main"
            placeholder="비밀번호 다시 입력"
          />
          {errors.passwordConfirm && (
            <p className="mt-1 text-sm text-red-600">{errors.passwordConfirm.message}</p>
          )}
        </div>
        <Button type="submit" className="w-full" loading={registerMutation.isPending}>
          회원가입
        </Button>
      </form>
      <p className="mt-4 text-center text-sm text-text-muted">
        이미 계정이 있으신가요?{" "}
        <Link to="/login" className="text-primary font-medium hover:underline">
          로그인
        </Link>
      </p>
    </div>
  );
}
