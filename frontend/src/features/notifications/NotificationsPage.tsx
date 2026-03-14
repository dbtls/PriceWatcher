import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { notificationApi } from "@/services/notificationApi";
import { useToastStore } from "@/stores/toastStore";
import { Skeleton } from "@/components/ui/Skeleton";
import { Link } from "react-router-dom";

export function NotificationsPage() {
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.add);

  const { data: notifications, isLoading } = useQuery({
    queryKey: ["notifications"],
    queryFn: () => notificationApi.getNotifications(),
  });

  const markRead = useMutation({
    mutationFn: (id: number) => notificationApi.markAsRead(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["notifications"] });
    },
    onError: (err) => addToast(String(err), "error"),
  });

  if (isLoading) {
    return (
      <div className="max-w-[600px] mx-auto px-6 py-8">
        <Skeleton className="h-8 w-32 mb-6" />
        <Skeleton className="h-16 w-full mb-2" />
        <Skeleton className="h-16 w-full mb-2" />
        <Skeleton className="h-16 w-full" />
      </div>
    );
  }

  if (!notifications || notifications.length === 0) {
    return (
      <div className="max-w-[600px] mx-auto px-6 py-8">
        <h1 className="text-2xl font-bold text-text-main mb-6">알림</h1>
        <p className="text-text-muted py-8">알림이 없습니다.</p>
      </div>
    );
  }

  return (
    <div className="max-w-[600px] mx-auto px-6 py-8">
      <h1 className="text-2xl font-bold text-text-main mb-6">알림</h1>
      <ul className="space-y-2">
        {notifications.map((n) => (
          <li
            key={n.id}
            className={`rounded-xl border p-4 ${
              n.isRead
                ? "bg-gray-50 dark:bg-gray-800/50 border-border"
                : "bg-primary-light/30 dark:bg-primary/10 border-primary/30"
            }`}
          >
            <div className="flex justify-between items-start gap-2">
              <div className="min-w-0 flex-1">
                <p className="font-medium text-text-main">{n.message}</p>
                {n.type && (
                  <p className="text-sm text-text-muted mt-1">{n.type}</p>
                )}
                {n.productId != null && (
                  <Link
                    to={`/products/${n.productId}`}
                    className="text-primary text-sm mt-2 inline-block hover:underline"
                  >
                    상품 보기 →
                  </Link>
                )}
              </div>
              {!n.isRead && (
                <button
                  type="button"
                  onClick={() => markRead.mutate(n.id)}
                  className="shrink-0 text-xs text-primary hover:underline"
                >
                  읽음
                </button>
              )}
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
}
