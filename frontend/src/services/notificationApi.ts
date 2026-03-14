import { api, unwrapData } from "@/lib/api";
import type { ApiResponse } from "@/lib/types";
import type { NotificationRes } from "@/lib/types";

export const notificationApi = {
  /** GET /notifications */
  getNotifications: () =>
    api
      .get<ApiResponse<NotificationRes[]>>("/notifications")
      .then((res) => unwrapData(res)),

  /** PATCH /notifications/:id/read - 성공 시 data null */
  markAsRead: (id: number) =>
    api.patch<ApiResponse<null>>(`/notifications/${id}/read`).then((res) => {
      if (res.data?.success === false)
        throw new Error(String(res.data?.message ?? "요청에 실패했습니다."));
      return undefined;
    }),
};
