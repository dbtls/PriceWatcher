import { api, unwrapData } from "@/lib/api";
import type { ApiResponse } from "@/lib/types";
import type { ProfileRes, MyPageSummaryRes } from "@/lib/types";

export const userApi = {
  /** GET /users/me */
  getProfile: () =>
    api
      .get<ApiResponse<ProfileRes>>("/users/me")
      .then((res) => unwrapData(res)),

  /** GET /users/me/summary - 프로필, 워치리스트/그룹/알림/목표가 도달 수, 최근 가격 하락 상위 5 */
  getMyPageSummary: () =>
    api
      .get<ApiResponse<MyPageSummaryRes>>("/users/me/summary")
      .then((res) => unwrapData(res)),
};
