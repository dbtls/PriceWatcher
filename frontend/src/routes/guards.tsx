import type { ReactNode } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { useAuthStore } from "@/stores/authStore";

/** 로그인 필요: 미인증 시 /login 으로 이동 */
export function RequireAuth({ children }: { children: ReactNode }) {
  const accessToken = useAuthStore((s) => s.accessToken);
  const authReady = useAuthStore((s) => s.authReady);
  const isAuth = Boolean(accessToken);
  const location = useLocation();

  if (!authReady) {
    return null;
  }

  if (!isAuth) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return <>{children}</>;
}

/** 게스트 전용: 인증 시 / 로 이동 */
export function GuestOnly({ children }: { children: ReactNode }) {
  const authReady = useAuthStore((s) => s.authReady);
  const isAuth = useAuthStore((s) => s.isAuthenticated());

  if (!authReady) {
    return null;
  }

  if (isAuth) {
    return <Navigate to="/" replace />;
  }
  return <>{children}</>;
}
