import { useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuthStore } from "@/stores/authStore";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { userApi } from "@/services/userApi";
import { notificationApi } from "@/services/notificationApi";
import { authApi } from "@/services/authApi";
import { useToastStore } from "@/stores/toastStore";

export function Header() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const accessToken = useAuthStore((s) => s.accessToken);
  const authReady = useAuthStore((s) => s.authReady);
  const isAuth = Boolean(accessToken);
  const setRole = useAuthStore((s) => s.setRole);
  const logout = useAuthStore((s) => s.logout);
  const addToast = useToastStore((s) => s.add);

  const { data: profile } = useQuery({
    queryKey: ["users", "me", accessToken],
    queryFn: () => userApi.getProfile(),
    enabled: authReady && Boolean(accessToken),
  });

  const { data: notifications } = useQuery({
    queryKey: ["notifications", accessToken],
    queryFn: () => notificationApi.getNotifications(),
    enabled: authReady && Boolean(accessToken),
  });

  const unreadCount =
    notifications?.filter((n) => !n.isRead).length ?? 0;

  useEffect(() => {
    if (profile?.role != null) setRole(profile.role);
  }, [profile?.role, setRole]);

  const handleLogout = async () => {
    try {
      await authApi.logout();
    } catch {
      addToast("로그아웃 처리 중 오류가 발생했습니다.", "error");
    }

    logout();
    queryClient.clear();
    navigate("/");
  };

  return (
    <header className="sticky top-0 z-50 w-full bg-[color:var(--surface)]/90 backdrop-blur-lg border-b border-border h-[72px] flex items-center">
      <div className="max-w-[1200px] mx-auto w-full px-6 flex items-center justify-between gap-8">
        <Link to="/" className="flex items-center gap-2 shrink-0">
          <div className="w-10 h-10 flex items-center justify-center bg-primary rounded-xl text-white shadow-lg shadow-blue-100 dark:shadow-blue-900/30">
            <span className="material-symbols-outlined fill-icon text-2xl">
              monitoring
            </span>
          </div>
          <h1 className="text-[22px] font-bold tracking-tight text-primary dark:text-primary hidden md:block">
            PriceWatcher
          </h1>
        </Link>

        <nav className="flex items-center gap-2">
          <Link
            to="/"
            className="px-3 py-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 text-text-main transition-colors text-sm font-medium"
          >
            검색
          </Link>
          {isAuth && (
            <Link
              to="/watchlist"
              className="px-3 py-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 text-text-main transition-colors text-sm font-medium"
            >
              관심 목록
            </Link>
          )}
        </nav>

        <div className="flex items-center gap-2 shrink-0">
          {isAuth ? (
            <>
              <Link
                to="/notifications"
                className="p-2.5 rounded-xl hover:bg-gray-100 dark:hover:bg-gray-800 text-text-main relative transition-colors"
                title="알림"
              >
                <span className="material-symbols-outlined">
                  notifications
                </span>
                {unreadCount > 0 && (
                  <span className="absolute top-2.5 right-2.5 w-2 h-2 bg-primary rounded-full border-2 border-white dark:border-[var(--surface)]" />
                )}
              </Link>
              <div className="h-6 w-px bg-border mx-2" />
              <div className="relative group">
                <button
                  type="button"
                  onClick={() => navigate("/me")}
                  className="flex items-center gap-2 pl-2 pr-1 py-1 rounded-full border border-border hover:bg-gray-100 dark:hover:bg-gray-800 transition-all"
                  aria-expanded="false"
                  aria-haspopup="true"
                >
                  <span className="text-sm font-semibold px-2 text-text-main">
                    마이페이지
                  </span>
                  <span className="w-8 h-8 rounded-full bg-primary/20 flex items-center justify-center overflow-hidden shrink-0">
                    <span className="material-symbols-outlined text-primary">
                      person
                    </span>
                  </span>
                </button>
                <div className="absolute right-0 top-full mt-1 py-2 w-48 bg-white dark:bg-[var(--surface)] rounded-xl border border-border shadow-lg opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all">
                  <Link
                    to="/me"
                    className="block px-4 py-2 text-sm text-text-main hover:bg-gray-100 dark:hover:bg-gray-800 rounded-lg mx-1 text-left"
                  >
                    내 정보
                  </Link>
                  <button
                    type="button"
                    onClick={handleLogout}
                    className="w-full text-left px-4 py-2 text-sm text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-lg mx-1"
                  >
                    로그아웃
                  </button>
                </div>
              </div>
            </>
          ) : (
            <>
              <Link
                to="/login"
                className="px-4 py-2 rounded-xl bg-primary text-white font-semibold text-sm hover:bg-primary-hover transition-colors"
              >
                로그인
              </Link>
              <Link
                to="/register"
                className="px-4 py-2 rounded-xl border-2 border-primary text-primary font-semibold text-sm hover:bg-primary-light transition-colors"
              >
                회원가입
              </Link>
            </>
          )}
        </div>
      </div>
    </header>
  );
}
