import { create } from "zustand";
import { persist } from "zustand/middleware";

interface AuthState {
  accessToken: string | null;
  role: string | null;
  authReady: boolean;
  setTokens: (access: string, role?: string | null) => void;
  setRole: (role: string | null) => void;
  setAuthReady: (ready: boolean) => void;
  logout: () => void;
  isAuthenticated: () => boolean;
  isAdmin: () => boolean;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      accessToken: null,
      role: null,
      authReady: false,
      setTokens: (access, role = null) =>
        set({ accessToken: access, role: role ?? null, authReady: true }),
      setRole: (role) => set({ role }),
      setAuthReady: (ready) => set({ authReady: ready }),
      logout: () => set({ accessToken: null, role: null, authReady: true }),
      isAuthenticated: () => !!get().accessToken,
      isAdmin: () => get().role === "ADMIN",
    }),
    {
      name: "pricewatcher-auth",
      partialize: (state) => ({
        accessToken: state.accessToken,
        role: state.role,
      }),
    }
  )
);
