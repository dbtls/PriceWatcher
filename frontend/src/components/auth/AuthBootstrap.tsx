import { useEffect } from "react";
import { refreshAccessToken } from "@/lib/api";
import { useAuthStore } from "@/stores/authStore";

export function AuthBootstrap() {
  const authReady = useAuthStore((s) => s.authReady);
  const setAuthReady = useAuthStore((s) => s.setAuthReady);

  useEffect(() => {
    let cancelled = false;

    async function bootstrapAuth() {
      try {
        await refreshAccessToken();
      } finally {
        if (!cancelled) {
          setAuthReady(true);
        }
      }
    }

    if (!authReady) {
      void bootstrapAuth();
    }

    return () => {
      cancelled = true;
    };
  }, [authReady, setAuthReady]);

  return null;
}
