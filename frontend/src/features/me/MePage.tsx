import { useQuery } from "@tanstack/react-query";
import { userApi } from "@/services/userApi";
import { Skeleton } from "@/components/ui/Skeleton";
import { Link, useNavigate } from "react-router-dom";
import type { MyPageRecentDropRes } from "@/lib/types";

export function MePage() {
  const navigate = useNavigate();
  const { data: summary, isLoading } = useQuery({
    queryKey: ["users", "me", "summary"],
    queryFn: () => userApi.getMyPageSummary(),
  });

  if (isLoading) {
    return (
      <div className="max-w-[600px] mx-auto px-6 py-8">
        <Skeleton className="h-8 w-48 mb-4" />
        <Skeleton className="h-4 w-full mb-2" />
        <Skeleton className="h-4 w-3/4" />
      </div>
    );
  }

  if (!summary) {
    return (
      <div className="max-w-[600px] mx-auto px-6 py-8">
        <p className="text-text-muted">요약을 불러올 수 없습니다.</p>
      </div>
    );
  }

  const { profile, watchlistCount, watchlistGroupCount, unreadNotificationCount, targetReachedCount, recentDrops } = summary;

  return (
    <div className="max-w-[600px] mx-auto px-6 py-8">
      <h1 className="text-2xl font-bold text-text-main mb-6">내 정보</h1>

      <section className="bg-white dark:bg-[var(--surface)] rounded-2xl border border-border p-6 space-y-4 mb-6">
        <div>
          <span className="text-sm text-text-muted">이메일</span>
          <p className="font-medium text-text-main">{profile.email}</p>
        </div>
        <div>
          <span className="text-sm text-text-muted">닉네임</span>
          <p className="font-medium text-text-main">{profile.nickname}</p>
        </div>
        <div>
          <span className="text-sm text-text-muted">역할</span>
          <p className="font-medium text-text-main">{profile.role}</p>
        </div>
      </section>

      <section className="mb-6">
        <h2 className="text-lg font-semibold text-text-main mb-3">요약</h2>
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          <Link
            to="/watchlist"
            className="bg-white dark:bg-[var(--surface)] rounded-xl border border-border p-4 hover:border-primary transition-colors"
          >
            <p className="text-2xl font-bold text-primary">{watchlistCount}</p>
            <p className="text-sm text-text-muted">관심 목록</p>
          </Link>
          <Link
            to="/watchlist#groups"
            className="bg-white dark:bg-[var(--surface)] rounded-xl border border-border p-4 hover:border-primary transition-colors"
          >
            <p className="text-2xl font-bold text-primary">{watchlistGroupCount}</p>
            <p className="text-sm text-text-muted">비교 그룹</p>
          </Link>
          <Link
            to="/notifications"
            className="bg-white dark:bg-[var(--surface)] rounded-xl border border-border p-4 hover:border-primary transition-colors"
          >
            <p className="text-2xl font-bold text-primary">{unreadNotificationCount}</p>
            <p className="text-sm text-text-muted">읽지 않은 알림</p>
          </Link>
          <div className="bg-white dark:bg-[var(--surface)] rounded-xl border border-border p-4">
            <p className="text-2xl font-bold text-primary">{targetReachedCount}</p>
            <p className="text-sm text-text-muted">목표가 도달</p>
          </div>
        </div>
      </section>

      {recentDrops && recentDrops.length > 0 && (
        <section>
          <h2 className="text-lg font-semibold text-text-main mb-3">최근 가격 하락 상위 5</h2>
          <ul className="space-y-2">
            {recentDrops.map((drop: MyPageRecentDropRes) => (
              <li key={drop.productId}>
                <button
                  type="button"
                  onClick={() => navigate(`/products/${drop.productId}`)}
                  className="w-full flex items-center gap-3 p-3 rounded-xl border border-border bg-white dark:bg-[var(--surface)] hover:border-primary text-left transition-colors"
                >
                  {drop.imageUrl ? (
                    <img
                      src={drop.imageUrl}
                      alt=""
                      className="w-12 h-12 rounded-lg object-contain bg-gray-100 dark:bg-gray-800"
                    />
                  ) : (
                    <div className="w-12 h-12 rounded-lg bg-gray-100 dark:bg-gray-800 flex items-center justify-center text-text-muted">
                      <span className="material-symbols-outlined text-2xl">image</span>
                    </div>
                  )}
                  <div className="min-w-0 flex-1">
                    <p className="font-medium text-text-main truncate">{drop.title}</p>
                    <p className="text-sm text-text-muted">
                      {Number(drop.previousPrice).toLocaleString()}원 → {Number(drop.currentPrice).toLocaleString()}원
                      <span className="text-primary font-medium ml-1">(-{drop.dropRatePercent}%)</span>
                    </p>
                  </div>
                  <span className="material-symbols-outlined text-text-muted">chevron_right</span>
                </button>
              </li>
            ))}
          </ul>
        </section>
      )}
    </div>
  );
}
