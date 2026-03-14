import { useState, useEffect } from "react";
import { useNavigate, useLocation, useSearchParams } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { productApi } from "@/services/productApi";
import type { ProductSummaryRes } from "@/lib/types";
import { SkeletonCard } from "@/components/ui/Skeleton";
import { useToastStore } from "@/stores/toastStore";

/** 단일 상품 카드: 클릭 시 상품 상세(추천 포함) 또는 외부 확인 모달 */
function ProductCard({
  item,
  onExternalConfirm,
}: {
  item: ProductSummaryRes;
  onExternalConfirm?: (product: ProductSummaryRes) => void;
}) {
  const navigate = useNavigate();
  const hasId = item.productId != null;
  const price =
    typeof item.price === "number" ? item.price : Number(item.price);

  const handleClick = () => {
    if (hasId) {
      navigate(`/products/${item.productId}`);
      return;
    }
    if (onExternalConfirm) {
      onExternalConfirm(item);
    } else {
      navigate("/search", { state: { product: item } });
    }
  };

  return (
    <article
      className="flex flex-col bg-white dark:bg-[var(--surface)] rounded-2xl border border-border overflow-hidden shadow-sm hover:shadow-sky transition-all cursor-pointer"
      onClick={handleClick}
    >
      <div className="aspect-square bg-gray-100 dark:bg-gray-800 relative">
        {item.imageUrl ? (
          <img
            src={item.imageUrl}
            alt={item.title}
            className="w-full h-full object-contain p-2"
          />
        ) : (
          <div className="w-full h-full flex items-center justify-center text-text-muted">
            <span className="material-symbols-outlined text-4xl">image</span>
          </div>
        )}
      </div>
      <div className="p-4 flex flex-col gap-1">
        {item.brand && (
          <span className="text-xs text-text-muted font-medium">{item.brand}</span>
        )}
        <h3 className="font-semibold text-text-main line-clamp-2">{item.title}</h3>
        <p className="text-lg font-bold text-primary mt-1">
          {price.toLocaleString()}원
        </p>
        {item.mallName && (
          <span className="text-xs text-text-muted">{item.mallName}</span>
        )}
      </div>
    </article>
  );
}

/** 외부 검색 상품 확인 모달: "이 제품이 맞습니까?" → 예 시 DB 저장, 같은 페이지 유지로 여러 개 연속 등록 가능 */
export function ConfirmProductModal({
  product,
  onCancel,
}: {
  product: ProductSummaryRes;
  searchQuery?: string;
  onCancel: () => void;
}) {
  const addToast = useToastStore((s) => s.add);
  const queryClient = useQueryClient();

  const selectMutation = useMutation({
    mutationFn: () => {
      const price =
        typeof product.price === "number" ? product.price : Number(product.price);
      const title = (product.title || "").trim() || "제목 없음";
      const brand = (product.brand || product.mallName || "알 수 없음").trim();
      const url =
        (product.url || "").trim() ||
        (product.naverProductId
          ? `https://search.shopping.naver.com/catalog/${product.naverProductId}`
          : `https://search.shopping.naver.com/search?query=${encodeURIComponent(title)}`);
      const naverProductId = (product.naverProductId || "").trim() || null;
      const externalKey =
        (product.externalKey || "").trim() ||
        (naverProductId ? null : `ext-${encodeURIComponent(title).slice(0, 200)}`);
      if (!naverProductId && !externalKey) {
        throw new Error("상품 식별 정보가 없어 등록할 수 없습니다.");
      }
      return productApi.select({
        productId: product.productId ?? undefined,
        brand: brand || "알 수 없음",
        title,
        price: Number.isFinite(price) && price >= 0 ? price : 0,
        mallName: product.mallName ?? undefined,
        naverProductId: naverProductId || undefined,
        externalKey: externalKey || undefined,
        url,
        imageUrl: product.imageUrl ?? undefined,
        categoryPath: product.categoryPath ?? undefined,
      });
    },
    onSuccess: () => {
      addToast("상품이 등록되었습니다. 다른 상품도 등록할 수 있습니다.", "success");
      queryClient.invalidateQueries({ queryKey: ["products"] });
      onCancel();
    },
    onError: (err) => {
      addToast(String(err), "error");
    },
  });

  const price =
    typeof product.price === "number" ? product.price : Number(product.price);

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50"
      onClick={onCancel}
      role="dialog"
      aria-modal="true"
      aria-labelledby="confirm-product-title"
    >
      <div
        className="bg-white dark:bg-[var(--surface)] rounded-2xl border border-border shadow-xl max-w-md w-full overflow-hidden"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="p-6">
          <h2 id="confirm-product-title" className="text-lg font-bold text-text-main mb-4">
            이 제품이 맞습니까?
          </h2>
          <div className="flex gap-4 mb-6">
            <div className="w-24 h-24 shrink-0 rounded-xl bg-gray-100 dark:bg-gray-800 overflow-hidden">
              {product.imageUrl ? (
                <img
                  src={product.imageUrl}
                  alt={product.title}
                  className="w-full h-full object-contain"
                />
              ) : (
                <div className="w-full h-full flex items-center justify-center text-text-muted">
                  <span className="material-symbols-outlined">image</span>
                </div>
              )}
            </div>
            <div className="min-w-0 flex-1">
              {product.brand && (
                <span className="text-xs text-text-muted">{product.brand}</span>
              )}
              <p className="font-semibold text-text-main line-clamp-2 mt-0.5">
                {product.title}
              </p>
              <p className="text-primary font-bold mt-1">
                {price.toLocaleString()}원
              </p>
            </div>
          </div>
          <div className="flex gap-3 justify-end">
            <button
              type="button"
              onClick={() => selectMutation.mutate()}
              disabled={selectMutation.isPending}
              className="px-4 py-2.5 rounded-xl bg-primary text-white font-semibold hover:bg-primary-hover transition-colors disabled:opacity-50"
            >
              {selectMutation.isPending ? "저장 중…" : "예"}
            </button>
            <button
              type="button"
              onClick={onCancel}
              className="px-4 py-2.5 rounded-xl border-2 border-border text-text-main font-medium hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors"
            >
              아니오
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

export function HomePage() {
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();
  const qFromUrl = searchParams.get("q") ?? "";
  const [query, setQuery] = useState(qFromUrl);
  const [submitted, setSubmitted] = useState(qFromUrl.trim());
  const [page, setPage] = useState(0);
  const [browsePage, setBrowsePage] = useState(0);
  const [externalSearchRequested, setExternalSearchRequested] = useState(false);
  const [confirmProduct, setConfirmProduct] = useState<ProductSummaryRes | null>(null);

  const PAGE_SIZE = 20;
  const BROWSE_PAGE_SIZE = 10;

  // URL ?q= 검색어와 동기화 (뒤로가기 시 검색 유지)
  useEffect(() => {
    const q = searchParams.get("q") ?? "";
    const trimmed = q.trim();
    if (trimmed !== submitted) {
      setQuery(trimmed);
      setSubmitted(trimmed);
    }
  }, [searchParams]);

  // 상품 등록 후 돌아왔을 때 직전 검색어로 검색 실행
  useEffect(() => {
    const q = (location.state as { searchQuery?: string } | null)?.searchQuery;
    if (q && typeof q === "string") {
      setQuery(q);
      setSubmitted(q);
      setExternalSearchRequested(false);
      setSearchParams({ q }, { replace: true });
      navigate(".", { replace: true, state: {} });
    }
  }, [location.state, navigate, setSearchParams]);

  const { data, isLoading, isFetching } = useQuery({
    queryKey: ["products", "search", submitted, page, PAGE_SIZE],
    queryFn: () => productApi.search(submitted, page, PAGE_SIZE),
    enabled: submitted.length > 0,
  });

  const { data: latestData, isLoading: latestLoading, isFetching: latestFetching } = useQuery({
    queryKey: ["products", "latest", browsePage, BROWSE_PAGE_SIZE],
    queryFn: () => productApi.getLatest(browsePage, BROWSE_PAGE_SIZE),
    enabled: submitted.length === 0,
  });

  const {
    data: externalData,
    isLoading: externalLoading,
    isFetching: externalFetching,
  } = useQuery({
    queryKey: ["products", "searchExternal", submitted],
    queryFn: () => productApi.searchExternal(submitted),
    enabled: submitted.length > 0 && externalSearchRequested,
  });

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = query.trim();
    setSubmitted(trimmed);
    setPage(0);
    setExternalSearchRequested(false);
    setSearchParams(trimmed ? { q: trimmed } : {}, { replace: true });
  };

  const internalItems = data?.internalResults ?? [];
  const externalItemsFromData = data?.externalResults ?? [];
  const hasResults = internalItems.length > 0 || externalItemsFromData.length > 0;
  const showExternalButton = submitted && data && !isLoading && !isFetching;
  const externalSearchItems = externalData?.externalResults ?? [];

  return (
    <div className="max-w-[1200px] mx-auto px-6 py-8">
      <section className="mb-10">
        <h1 className="text-2xl md:text-3xl font-bold text-text-main mb-2">
          가격을 한눈에, PriceWatcher
        </h1>
        <p className="text-text-muted mb-6">
          상품을 검색하고 관심 목록에 추가해 목표가 도달 시 알림을 받아보세요.
        </p>

        <form onSubmit={handleSearch} className="flex gap-2 max-w-xl">
          <input
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="상품명 또는 브랜드로 검색"
            className="flex-1 px-4 py-3 rounded-xl border-2 border-border focus:border-primary focus:outline-none bg-white dark:bg-[var(--surface)] text-text-main"
          />
          <button
            type="submit"
            className="px-6 py-3 rounded-xl bg-primary text-white font-semibold hover:bg-primary-hover transition-colors flex items-center gap-2"
          >
            <span className="material-symbols-outlined">search</span>
            검색
          </button>
        </form>
      </section>

      {!submitted && (
        <section className="mb-10">
          <h2 className="text-lg font-semibold text-text-main mb-4">최근 등록 상품</h2>
          {latestLoading || latestFetching ? (
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4">
              {Array.from({ length: 10 }).map((_, i) => (
                <SkeletonCard key={i} />
              ))}
            </div>
          ) : latestData ? (
            latestData.items.length === 0 ? (
              <p className="py-8 text-text-muted">등록된 상품이 없습니다.</p>
            ) : (
              <>
                <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4">
                  {latestData.items.map((item, i) => (
                    <ProductCard
                      key={item.productId ?? item.externalKey ?? i}
                      item={item}
                    />
                  ))}
                </div>
                {latestData.totalCount > BROWSE_PAGE_SIZE && (
                  <div className="mt-6 flex flex-wrap items-center justify-between gap-4">
                    <p className="text-sm text-text-muted">
                      {browsePage * BROWSE_PAGE_SIZE + 1}–
                      {Math.min((browsePage + 1) * BROWSE_PAGE_SIZE, latestData.totalCount)} / 총{" "}
                      {latestData.totalCount}개
                    </p>
                    <div className="flex items-center gap-2">
                      <button
                        type="button"
                        onClick={() => setBrowsePage((p) => Math.max(0, p - 1))}
                        disabled={browsePage === 0 || latestFetching}
                        className="px-4 py-2 rounded-xl border border-border bg-white dark:bg-[var(--surface)] text-text-main font-medium hover:bg-gray-100 dark:hover:bg-gray-800 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                      >
                        이전
                      </button>
                      <button
                        type="button"
                        onClick={() => setBrowsePage((p) => p + 1)}
                        disabled={!latestData.hasNext || latestFetching}
                        className="px-4 py-2 rounded-xl border border-border bg-white dark:bg-[var(--surface)] text-text-main font-medium hover:bg-gray-100 dark:hover:bg-gray-800 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                      >
                        다음
                      </button>
                    </div>
                  </div>
                )}
              </>
            )
          ) : null}
        </section>
      )}

      {submitted && (
        <section>
          <h2 className="text-lg font-semibold text-text-main mb-4">
            검색 결과
            {data && (
              <span className="text-text-muted font-normal ml-2">
                (총 {data.totalCount}건)
              </span>
            )}
          </h2>

          {isLoading || isFetching ? (
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-4">
              {Array.from({ length: 8 }).map((_, i) => (
                <SkeletonCard key={i} />
              ))}
            </div>
          ) : data ? (
            <>
              {!hasResults ? (
                <div className="py-8">
                  <p className="text-text-muted mb-4">
                    검색 결과가 없습니다.
                  </p>
                  <button
                    type="button"
                    onClick={() => setExternalSearchRequested(true)}
                    className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl border-2 border-primary text-primary font-semibold hover:bg-primary-light transition-colors"
                  >
                    <span className="material-symbols-outlined">
                      public
                    </span>
                    외부(네이버 쇼핑)에서 검색하기
                  </button>
                </div>
              ) : (
                <>
                  <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-4">
                    {[...internalItems, ...externalItemsFromData].map(
                      (item, i) => (
                        <ProductCard
                          key={item.productId ?? item.externalKey ?? i}
                          item={item}
                        />
                      )
                    )}
                  </div>
                  {data && data.totalCount > PAGE_SIZE && (
                    <div className="mt-6 flex flex-wrap items-center justify-between gap-4">
                      <p className="text-sm text-text-muted">
                        {page * PAGE_SIZE + 1}–
                        {Math.min((page + 1) * PAGE_SIZE, data.totalCount)} / 총{" "}
                        {data.totalCount}건
                      </p>
                      <div className="flex items-center gap-2">
                        <button
                          type="button"
                          onClick={() => setPage((p) => Math.max(0, p - 1))}
                          disabled={page === 0 || isFetching}
                          className="px-4 py-2 rounded-xl border border-border bg-white dark:bg-[var(--surface)] text-text-main font-medium hover:bg-gray-100 dark:hover:bg-gray-800 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                        >
                          이전
                        </button>
                        <button
                          type="button"
                          onClick={() => setPage((p) => p + 1)}
                          disabled={!data.hasNext || isFetching}
                          className="px-4 py-2 rounded-xl border border-border bg-white dark:bg-[var(--surface)] text-text-main font-medium hover:bg-gray-100 dark:hover:bg-gray-800 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                        >
                          다음
                        </button>
                      </div>
                    </div>
                  )}
                  {showExternalButton && (
                    <div className="mt-8 pt-6 border-t border-border">
                      <p className="text-text-muted text-sm mb-3">
                        원하는 상품이 없나요?
                      </p>
                      <button
                        type="button"
                        onClick={() => setExternalSearchRequested(true)}
                        className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl border-2 border-primary text-primary font-semibold hover:bg-primary-light transition-colors"
                      >
                        <span className="material-symbols-outlined">
                          public
                        </span>
                        외부(네이버 쇼핑)에서 더 검색하기
                      </button>
                    </div>
                  )}
                </>
              )}
            </>
          ) : null}

          {externalSearchRequested && (
            <div className="mt-10 pt-8 border-t border-border">
              <h3 className="text-lg font-semibold text-text-main mb-4">
                네이버 쇼핑 검색 결과
                {externalData && (
                  <span className="text-text-muted font-normal ml-2">
                    ({externalSearchItems.length}건)
                  </span>
                )}
              </h3>
              {externalLoading || externalFetching ? (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {Array.from({ length: 6 }).map((_, i) => (
                    <SkeletonCard key={i} />
                  ))}
                </div>
              ) : externalData ? (
                externalSearchItems.length === 0 ? (
                  <p className="text-text-muted py-6">
                    외부 검색 결과가 없습니다.
                  </p>
                ) : (
                  <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-4">
                    {externalSearchItems.map((item, i) => (
                      <ProductCard
                        key={item.productId ?? item.externalKey ?? i}
                        item={item}
                        onExternalConfirm={(product) => setConfirmProduct(product)}
                      />
                    ))}
                  </div>
                )
              ) : null}
            </div>
          )}
        </section>
      )}

      {confirmProduct && submitted && (
        <ConfirmProductModal
          product={confirmProduct}
          onCancel={() => setConfirmProduct(null)}
        />
      )}
    </div>
  );
}
