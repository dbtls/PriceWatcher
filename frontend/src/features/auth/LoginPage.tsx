import { useNavigate, useLocation } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation } from "@tanstack/react-query";
import { authApi } from "@/services/authApi";
import { useAuthStore } from "@/stores/authStore";
import { getApiErrorMessage } from "@/lib/api";
import { useToastStore } from "@/stores/toastStore";
import { Button } from "@/components/ui/Button";
import { Link } from "react-router-dom";

const schema = z.object({
  email: z.string().email("올바른 이메일을 입력하세요"),
  password: z.string().min(1, "비밀번호를 입력하세요"),
});

type FormData = z.infer<typeof schema>;

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const addToast = useToastStore((s) => s.add);
  const setTokens = useAuthStore((s) => s.setTokens);
  const from = (location.state as { from?: { pathname: string } })?.from?.pathname ?? "/";

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { email: "", password: "" },
  });

  const login = useMutation({
    mutationFn: (data: FormData) => authApi.login(data),
    onSuccess: (res) => {
      setTokens(res.data.accessToken);
      addToast("로그인되었습니다.", "success");
      navigate(from, { replace: true });
    },
    onError: (err) => addToast(getApiErrorMessage(err), "error"),
  });

  return (
    <div className="max-w-[400px] mx-auto px-6 py-12">
      <h1 className="text-2xl font-bold text-text-main mb-6">로그인</h1>
      <form
        onSubmit={handleSubmit((data) => login.mutate(data))}
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
          <label htmlFor="password" className="block text-sm font-medium text-text-main mb-1">
            비밀번호
          </label>
          <input
            id="password"
            type="password"
            {...register("password")}
            className="w-full px-4 py-3 rounded-xl border-2 border-border focus:border-primary focus:outline-none bg-white dark:bg-[var(--surface)] text-text-main"
            placeholder="비밀번호"
          />
          {errors.password && (
            <p className="mt-1 text-sm text-red-600">{errors.password.message}</p>
          )}
        </div>
        <Button type="submit" className="w-full" loading={login.isPending}>
          로그인
        </Button>
      </form>
      <p className="mt-4 text-center text-sm text-text-muted">
        계정이 없으신가요?{" "}
        <Link to="/register" className="text-primary font-medium hover:underline">
          회원가입
        </Link>
      </p>
    </div>
  );
}
