import axios, { type AxiosError, type AxiosResponse } from "axios";
import { useAuthStore } from "@/stores/authStore";
import type { ApiResponse } from "@/lib/types";

const baseURL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

/** refresh 전용 클라이언트: Authorization 헤더 없이 쿠키만 보냄 (만료된 액세스 토큰 제외) */
const refreshClient = axios.create({
  baseURL,
  headers: { "Content-Type": "application/json" },
  withCredentials: true,
});

export const api = axios.create({
  baseURL,
  headers: { "Content-Type": "application/json" },
  withCredentials: true,
});

let refreshPromise: Promise<string | null> | null = null;

api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

function getAccessTokenFromRefreshResponse(body: unknown): string | null {
  if (body && typeof body === "object" && "data" in body) {
    const data = (body as { data?: unknown }).data;
    if (data && typeof data === "object" && "accessToken" in data) {
      const token = (data as { accessToken?: unknown }).accessToken;
      return typeof token === "string" ? token : null;
    }
  }
  if (body && typeof body === "object" && "accessToken" in body) {
    const token = (body as { accessToken?: unknown }).accessToken;
    return typeof token === "string" ? token : null;
  }
  return null;
}

export async function refreshAccessToken(options?: {
  redirectOnFailure?: boolean;
}): Promise<string | null> {
  const redirectOnFailure = options?.redirectOnFailure ?? false;

  if (!refreshPromise) {
    refreshPromise = refreshClient
      .post("/auth/refresh", {})
      .then(({ data }) => {
        const accessToken = getAccessTokenFromRefreshResponse(data);
        if (!accessToken) {
          useAuthStore.getState().logout();
          return null;
        }

        useAuthStore.getState().setTokens(accessToken);
        return accessToken;
      })
      .catch(() => {
        useAuthStore.getState().logout();
        if (redirectOnFailure) {
          window.location.href = "/login";
        }
        return null;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }

  return refreshPromise;
}

api.interceptors.response.use(
  (res) => res,
  async (err: AxiosError<{ message?: string }>) => {
    const original = err.config;
    const requestUrl = original?.url ?? "";

    if (
      err.response?.status === 401 &&
      original &&
      !(original as { _retry?: boolean })._retry &&
      !requestUrl.startsWith("/auth/")
    ) {
      (original as { _retry?: boolean })._retry = true;

      const accessToken = await refreshAccessToken({ redirectOnFailure: true });
      if (accessToken) {
        original.headers = original.headers ?? {};
        original.headers.Authorization = `Bearer ${accessToken}`;
        return api(original);
      }
    }

    return Promise.reject(err);
  }
);

export function getApiErrorMessage(err: unknown): string {
  if (axios.isAxiosError(err)) {
    const data = err.response?.data as
      | {
          message?: unknown;
          code?: string;
        }
      | string
      | undefined;

    if (typeof data === "string" && data.trim()) return data;
    if (data && typeof data === "object" && data.message != null)
      return String(data.message);
  }
  if (err instanceof Error) return err.message;
  return "오류가 발생했습니다.";
}

/** ApiResponse 래핑된 응답에서 data 추출 */
export function unwrapData<T>(res: AxiosResponse<ApiResponse<T>>): T {
  const data = res.data?.data;
  if (data === undefined || data === null) {
    throw new Error(res.data?.message ?? "No data");
  }
  return data;
}
