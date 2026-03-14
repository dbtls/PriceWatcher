import { api, refreshAccessToken, unwrapData } from "@/lib/api";
import type { ApiResponse } from "@/lib/types";
import type { LoginRes } from "@/lib/types";

export interface LoginReq {
  email: string;
  password: string;
}

export interface RegisterReq {
  email: string;
  password: string;
  nickname: string;
}

export const authApi = {
  register: (body: RegisterReq) =>
    api.post<ApiResponse<null>>("/auth/register", body),

  login: (body: LoginReq) =>
    api.post<ApiResponse<LoginRes>>("/auth/login", body).then((res) => {
      const data = unwrapData(res);
      return { data };
    }),

  refresh: () =>
    refreshAccessToken().then((accessToken) => {
      if (!accessToken) {
        throw new Error("토큰 재발급에 실패했습니다.");
      }
      return { data: { accessToken, tokenType: "Bearer" } };
    }),

  logout: () => api.post<ApiResponse<null>>("/auth/logout"),
};
