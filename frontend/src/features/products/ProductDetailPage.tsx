import { useParams, useNavigate } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { productApi } from "@/services/productApi";
import { recommendationApi } from "@/services/recommendationApi";
import { priceApi } from "@/services/priceApi";
import { watchlistApi } from "@/services/watchlistApi";
import { useAuthStore } from "@/stores/authStore";
import { useToastStore } from "@/stores/toastStore";
import type { ProductSummaryRes } from "@/lib/types";
import { Button } from "@/components/ui/Button";
import { Skeleton } from "@/components/ui/Skeleton";
import { PriceTrendChart } from "@/components/charts/PriceTrendChart";
import dayjs from "dayjs";

const DAYS = 30;

function ProductCard({ p }: { p: ProductSummaryRes }) {
  const navigate = useNavigate();
  const hasId = p.productId != null;
  const price = typeof p.price === "number" ? p.price : Number(p.price);

  if (!hasId) return null;

  return (
    <article
      className="flex flex-col bg-white dark:bg-[var(--surface)] rounded-xl border border-border overflow-hidden shadow-sm hover:shadow-sky transition-all cursor-pointer flex-shrink-0 w-[160px] sm:w-[180px]"
      onClick={() => navigate(`/products/${p.productId}`)}
    >
      <div className="aspect-square bg-gray-100 dark:bg-gray-800">
        {p.imageUrl ? (
          <img
            src={p.imageUrl}
            alt={p.title}
            className="w-full h-full object-contain p-2"
          />
        ) : (
          <div className="w-full h-full flex items-center justify-center text-text-muted">
            <span className="material-symbols-outlined text-3xl">image</span>
          </div>
        )}
      </div>
      <div className="p-2 min-w-0">
        {p.brand && (
          <span className="text-xs text-text-muted truncate block">{p.brand}</span>
        )}
        <h3 className="font-medium text-text-main text-sm line-clamp-2">{p.title}</h3>
        <p className="text-primary font-semibold text-sm mt-0.5">
          {price.toLocaleString()}원
        </p>
      </div>
    </article>
  );
}

function RecommendationSection({
  title,
  items,
  isLoading,
}: {
  title: string;
  items: ProductSummaryRes[];
  isLoading: boolean;
}) {
  if (isLoading) {
    return (
      <section className="mt-10">
        <h2 className="text-lg font-semibold text-text-main mb-4">{title}</h2>
        <div className="flex gap-3 overflow-x-auto pb-2">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-48 w-[160px] flex-shrink-0 rounded-xl" />
          ))}
        </div>
      </section>
    );
  }
  if (!items || items.length === 0) return null;

  return (
    <section className="mt-10">
      <h2 className="text-lg font-semibold text-text-main mb-4">{title}</h2>
      <div className="flex gap-3 overflow-x-auto pb-2 custom-scrollbar">
        {items.map((p, i) => (
          <ProductCard key={p.productId ?? p.externalKey ?? i} p={p} />
        ))}
      </div>
    </section>
  );
}

export function ProductDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.add);
  const isAuth = useAuthStore((s) => s.isAuthenticated());

  const productId = id ? Number(id) : NaN;
  const isValidId = Number.isInteger(productId) && productId > 0;

  const { data: product, isLoading: productLoading } = useQuery({
    queryKey: ["products", productId],
    queryFn: () => productApi.getProduct(productId),
    enabled: isValidId,
  });

  const { data: recommendations, isLoading: recLoading } = useQuery({
    queryKey: ["products", productId, "recommendations"],
    queryFn: () => recommendationApi.getRecommendations(productId, 10),
    enabled: isValidId && !!product,
  });

  const { data: priceHistory, isLoading: priceHistoryLoading } = useQuery({
    queryKey: ["products", productId, "price-history", DAYS],
    queryFn: () => priceApi.getPriceHistory(productId, DAYS),
    enabled: isValidId,
  });

  const addWatchlist = useMutation({
    mutationFn: () => watchlistApi.add(productId),
    onSuccess: () => {
      addToast("관심 목록에 추가되었습니다.", "success");
      queryClient.invalidateQueries({ queryKey: ["watchlist"] });
    },
    onError: (err) => addToast(String(err), "error"),
  });

  if (!isValidId) {
    return (
      <div className="max-w-[1200px] mx-auto px-6 py-8">
        <p className="text-text-muted">잘못된 상품입니다.</p>
        <Button variant="outline" className="mt-4" onClick={() => navigate("/")}>
          홈으로
        </Button>
      </div>
    );
  }

  if (productLoading || !product) {
    return (
      <div className="max-w-[1200px] mx-auto px-6 py-8">
        <Skeleton className="h-8 w-24 mb-6" />
        <Skeleton className="h-64 w-full rounded-2xl mb-6" />
        <Skeleton className="h-8 w-3/4 mb-2" />
        <Skeleton className="h-6 w-1/2" />
      </div>
    );
  }

  const price = typeof product.price === "number" ? product.price : Number(product.price);

  const handleAddWatchlist = () => {
    if (!isAuth) {
      addToast("로그인 후 이용해 주세요.", "info");
      navigate("/login", { state: { from: { pathname: `/products/${productId}` } } });
      return;
    }
    addWatchlist.mutate();
  };

  return (
    <div className="max-w-[1200px] mx-auto px-6 py-8">
      <Button
        variant="ghost"
        size="sm"
        className="mb-4 -ml-2"
        onClick={() => navigate(-1)}
      >
        <span className="material-symbols-outlined mr-1">arrow_back</span>
        뒤로
      </Button>

      <div className="flex flex-col md:flex-row gap-8">
        <div className="w-full md:w-80 shrink-0">
          <div className="aspect-square bg-gray-100 dark:bg-gray-800 rounded-2xl overflow-hidden">
            {product.imageUrl ? (
              <img
                src={product.imageUrl}
                alt={product.title}
                className="w-full h-full object-contain p-4"
              />
            ) : (
              <div className="w-full h-full flex items-center justify-center text-text-muted">
                <span className="material-symbols-outlined text-6xl">image</span>
              </div>
            )}
          </div>
        </div>
        <div className="flex-1">
          {product.brand && (
            <span className="text-sm text-text-muted font-medium">{product.brand}</span>
          )}
          <h1 className="text-xl font-bold text-text-main mt-1">{product.title}</h1>
          <p className="text-2xl font-bold text-primary mt-2">
            {price.toLocaleString()}원
          </p>
          {product.mallName && (
            <p className="text-text-muted text-sm mt-1">{product.mallName}</p>
          )}
          {product.url && (
            <a
              href={product.url}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-1 text-primary text-sm mt-2 hover:underline"
            >
              쇼핑몰에서 보기
              <span className="material-symbols-outlined text-base">open_in_new</span>
            </a>
          )}
          <div className="mt-6">
            <Button
              onClick={handleAddWatchlist}
              loading={addWatchlist.isPending}
              size="lg"
            >
              관심 목록에 추가
            </Button>
          </div>
        </div>
      </div>

      {/* 최근 30일 가격 이력 */}
      <section className="mt-10">
        <h2 className="text-lg font-semibold text-text-main mb-4">최근 30일 가격 이력</h2>
        {priceHistoryLoading ? (
          <div className="grid grid-cols-3 sm:grid-cols-5 md:grid-cols-6 lg:grid-cols-10 gap-2">
            {Array.from({ length: 30 }).map((_, i) => (
              <Skeleton key={i} className="h-16 rounded-xl" />
            ))}
          </div>
        ) : priceHistory && priceHistory.length > 0 ? (
          <div className="space-y-4">
            <PriceTrendChart
              series={[
                {
                  id: `product-${productId}`,
                  name: product.title,
                  color: "#2563eb",
                  points: priceHistory,
                },
              ]}
            />
            <div className="grid grid-cols-3 sm:grid-cols-5 md:grid-cols-6 lg:grid-cols-10 gap-2">
              {[...priceHistory]
                .sort(
                  (a, b) =>
                    new Date(b.capturedAt).getTime() - new Date(a.capturedAt).getTime()
                )
                .map((item, i) => (
                  <div
                    key={`${item.capturedAt}-${i}`}
                    className="bg-white dark:bg-[var(--surface)] rounded-xl border border-border p-3 text-center"
                  >
                    <p className="text-xs text-text-muted">
                      {dayjs(item.capturedAt).format("MM/DD")}
                    </p>
                    <p className="text-sm font-semibold text-primary mt-1">
                      {Number(item.price).toLocaleString()}원
                    </p>
                  </div>
                ))}
            </div>
          </div>
        ) : (
          <p className="text-text-muted py-6 text-sm">기록된 가격 이력이 없습니다.</p>
        )}
      </section>

      {/* 같은 브랜드 유사 상품 */}
      <RecommendationSection
        title="같은 브랜드 유사 상품"
        items={recommendations?.brandSimilarProducts ?? []}
        isLoading={recLoading}
      />

      {/* 비슷한 상품 (브랜드 무관) */}
      <RecommendationSection
        title="비슷한 상품"
        items={recommendations?.similarProducts ?? []}
        isLoading={recLoading}
      />
    </div>
  );
}
