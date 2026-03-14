import { BrowserRouter, Routes, Route, Navigate, Outlet } from "react-router-dom";
import { Layout } from "@/components/layout/Layout";
import { RequireAuth, GuestOnly } from "@/routes/guards";
import { HomePage } from "@/features/home/HomePage";
import { SearchPage } from "@/features/search/SearchPage";
import { ProductDetailPage } from "@/features/products/ProductDetailPage";
import { WatchlistPage } from "@/features/watchlist/WatchlistPage";
import { WatchlistGroupDetailPage } from "@/features/watchlist/WatchlistGroupDetailPage";
import { LoginPage } from "@/features/auth/LoginPage";
import { RegisterPage } from "@/features/auth/RegisterPage";
import { MePage } from "@/features/me/MePage";
import { NotificationsPage } from "@/features/notifications/NotificationsPage";

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route index element={<HomePage />} />
          <Route path="search" element={<SearchPage />} />
          <Route path="products/:id" element={<ProductDetailPage />} />
          <Route path="watchlist" element={<RequireAuth><Outlet /></RequireAuth>}>
            <Route index element={<WatchlistPage />} />
            <Route path="groups/:groupId" element={<WatchlistGroupDetailPage />} />
          </Route>
          <Route
            path="me"
            element={
              <RequireAuth>
                <MePage />
              </RequireAuth>
            }
          />
          <Route
            path="notifications"
            element={
              <RequireAuth>
                <NotificationsPage />
              </RequireAuth>
            }
          />
          <Route
            path="login"
            element={
              <GuestOnly>
                <LoginPage />
              </GuestOnly>
            }
          />
          <Route
            path="register"
            element={
              <GuestOnly>
                <RegisterPage />
              </GuestOnly>
            }
          />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
