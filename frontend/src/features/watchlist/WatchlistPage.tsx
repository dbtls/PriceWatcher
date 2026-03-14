import { useNavigate } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { watchlistApi } from "@/services/watchlistApi";
import { priceApi } from "@/services/priceApi";
import { useToastStore } from "@/stores/toastStore";
import type { WatchlistRes } from "@/lib/types";
import { Button } from "@/components/ui/Button";
import { SkeletonCard } from "@/components/ui/Skeleton";
import { useState, useMemo } from "react";

const PRICE_HISTORY_DAYS = 30;

/** 가격 이력에서 현재가(최신), 30일 최저가 계산 */
function usePriceInfo(productId: number, enabled: boolean) {
  const { data: history } = useQuery({
    queryKey: ["price-history", productId, PRICE_HISTORY_DAYS],
    queryFn: () => priceApi.getPriceHistory(productId, PRICE_HISTORY_DAYS),
    enabled,
  });
  return useMemo(() => {
    if (!history || history.length === 0) return { currentPrice: null, lowestPrice: null };
    const prices = history.map((h) => (typeof h.price === "number" ? h.price : Number(h.price)));
    const sorted = [...history].sort(
      (a, b) => new Date(b.capturedAt).getTime() - new Date(a.capturedAt).getTime()
    );
    const currentPrice = sorted[0]
      ? typeof sorted[0].price === "number"
        ? sorted[0].price
        : Number(sorted[0].price)
      : null;
    const lowestPrice = prices.length ? Math.min(...prices) : null;
    return { currentPrice, lowestPrice };
  }, [history]);
}

function EditTargetPriceModal({
  item,
  onClose,
  onSaved,
}: {
  item: WatchlistRes;
  onClose: () => void;
  onSaved: () => void;
}) {
  const addToast = useToastStore((s) => s.add);
  const [value, setValue] = useState(String(item.targetPrice));

  const updateMutation = useMutation({
    mutationFn: (price: number) =>
      watchlistApi.updateTargetPrice(item.productId, { targetPrice: price }),
    onSuccess: () => {
      addToast("목표가가 변경되었습니다.", "success");
      onSaved();
      onClose();
    },
    onError: (err) => addToast(String(err), "error"),
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const num = Number(value);
    if (!Number.isFinite(num) || num < 0) {
      addToast("올바른 금액을 입력하세요.", "error");
      return;
    }
    updateMutation.mutate(num);
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50"
      onClick={onClose}
      role="dialog"
      aria-modal="true"
    >
      <div
        className="bg-white dark:bg-[var(--surface)] rounded-2xl border border-border shadow-xl max-w-sm w-full p-6"
        onClick={(e) => e.stopPropagation()}
      >
        <h2 className="text-lg font-bold text-text-main mb-2">목표가 변경</h2>
        <p className="text-sm text-text-muted truncate mb-4">{item.title}</p>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="target-price" className="block text-sm text-text-muted mb-1">
              목표가 (원)
            </label>
            <input
              id="target-price"
              type="number"
              min={0}
              value={value}
              onChange={(e) => setValue(e.target.value)}
              className="w-full px-4 py-2 rounded-xl border border-border bg-white dark:bg-[var(--surface)] text-text-main"
            />
          </div>
          <div className="flex gap-2 justify-end">
            <Button type="button" variant="outline" onClick={onClose}>
              취소
            </Button>
            <Button type="submit" loading={updateMutation.isPending}>
              저장
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}

function WatchlistCard({
  item,
  currentPrice,
  lowestPrice,
}: {
  item: WatchlistRes;
  currentPrice: number | null;
  lowestPrice: number | null;
}) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.add);
  const [showTargetModal, setShowTargetModal] = useState(false);

  const removeMutation = useMutation({
    mutationFn: () => watchlistApi.remove(item.productId),
    onSuccess: () => {
      addToast("관심 목록에서 제거되었습니다.", "success");
      queryClient.invalidateQueries({ queryKey: ["watchlist"] });
    },
    onError: (err) => addToast(String(err), "error"),
  });

  const targetPrice = typeof item.targetPrice === "number" ? item.targetPrice : Number(item.targetPrice);

  return (
    <>
      <article className="flex flex-col sm:flex-row gap-4 bg-white dark:bg-[var(--surface)] rounded-2xl border border-border p-4 shadow-sm">
        <div
          className="w-full sm:w-32 aspect-square bg-gray-100 dark:bg-gray-800 rounded-xl overflow-hidden shrink-0 cursor-pointer"
          onClick={() => navigate(`/products/${item.productId}`)}
        >
          {item.imageUrl ? (
            <img
              src={item.imageUrl}
              alt={item.title}
              className="w-full h-full object-contain p-2"
            />
          ) : (
            <div className="w-full h-full flex items-center justify-center text-text-muted">
              <span className="material-symbols-outlined">image</span>
            </div>
          )}
        </div>
        <div className="flex-1 min-w-0">
          <h3
            className="font-semibold text-text-main line-clamp-2 cursor-pointer hover:text-primary"
            onClick={() => navigate(`/products/${item.productId}`)}
          >
            {item.title}
          </h3>
          <div className="mt-3 grid grid-cols-1 sm:grid-cols-3 gap-2 text-sm">
            <div>
              <span className="text-text-muted">현재가</span>
              <p className="font-semibold text-primary">
                {currentPrice != null ? `${currentPrice.toLocaleString()}원` : "-"}
              </p>
            </div>
            <div>
              <span className="text-text-muted">30일 최저가</span>
              <p className="font-medium text-text-main">
                {lowestPrice != null ? `${lowestPrice.toLocaleString()}원` : "-"}
              </p>
            </div>
            <div>
              <span className="text-text-muted">목표가</span>
              <p className="font-medium text-text-main">{targetPrice.toLocaleString()}원</p>
            </div>
          </div>
        </div>
        <div className="flex sm:flex-col gap-2 shrink-0">
          <Button
            variant="outline"
            size="sm"
            onClick={() => setShowTargetModal(true)}
          >
            목표가 변경
          </Button>
          <Button
            variant="outline"
            size="sm"
            onClick={() => navigate(`/products/${item.productId}`)}
          >
            상세보기
          </Button>
          <Button
            variant="danger"
            size="sm"
            onClick={() => removeMutation.mutate()}
            loading={removeMutation.isPending}
          >
            제거
          </Button>
        </div>
      </article>

      {showTargetModal && (
        <EditTargetPriceModal
          item={item}
          onClose={() => setShowTargetModal(false)}
          onSaved={() => queryClient.invalidateQueries({ queryKey: ["watchlist"] })}
        />
      )}
    </>
  );
}

function WatchlistCardWithPrice({ item }: { item: WatchlistRes }) {
  const priceInfo = usePriceInfo(item.productId, true);
  return (
    <WatchlistCard
      item={item}
      currentPrice={priceInfo.currentPrice}
      lowestPrice={priceInfo.lowestPrice}
    />
  );
}

function CreateGroupModal({
  onClose,
  onCreated,
}: {
  onClose: () => void;
  onCreated: () => void;
}) {
  const addToast = useToastStore((s) => s.add);
  const [name, setName] = useState("");

  const createMutation = useMutation({
    mutationFn: () => watchlistApi.createGroup({ name: name.trim() }),
    onSuccess: () => {
      addToast("비교 그룹이 생성되었습니다.", "success");
      onCreated();
      onClose();
    },
    onError: (err) => addToast(String(err), "error"),
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) {
      addToast("그룹 이름을 입력하세요.", "error");
      return;
    }
    createMutation.mutate();
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50"
      onClick={onClose}
      role="dialog"
      aria-modal="true"
    >
      <div
        className="bg-white dark:bg-[var(--surface)] rounded-2xl border border-border shadow-xl max-w-md w-full p-6"
        onClick={(e) => e.stopPropagation()}
      >
        <h2 className="text-lg font-bold text-text-main mb-4">비교 그룹 만들기</h2>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="group-name" className="block text-sm text-text-muted mb-1">
              그룹 이름
            </label>
            <input
              id="group-name"
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="예: 검정 후드집업 비교"
              className="w-full px-4 py-2 rounded-xl border border-border bg-white dark:bg-[var(--surface)] text-text-main"
            />
          </div>
          <div className="flex gap-2 justify-end">
            <Button type="button" variant="outline" onClick={onClose}>
              취소
            </Button>
            <Button type="submit" loading={createMutation.isPending}>
              만들기
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}

export function WatchlistPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [showCreateGroup, setShowCreateGroup] = useState(false);

  const { data: items, isLoading } = useQuery({
    queryKey: ["watchlist"],
    queryFn: () => watchlistApi.getMine(),
  });

  const { data: groups, isLoading: groupsLoading } = useQuery({
    queryKey: ["watchlist", "groups"],
    queryFn: () => watchlistApi.getGroups(),
  });

  return (
    <div className="max-w-[1200px] mx-auto px-6 py-8">
      <h1 className="text-2xl font-bold text-text-main mb-6">관심 목록</h1>

      <section id="groups" className="scroll-mt-6 mb-10">
        <div className="flex items-center justify-between mb-3">
          <h2 className="text-lg font-semibold text-text-main">비교 그룹</h2>
          <Button size="sm" onClick={() => setShowCreateGroup(true)}>
            <span className="material-symbols-outlined text-lg mr-1">add</span>
            그룹 만들기
          </Button>
        </div>
        {groupsLoading ? (
          <div className="grid gap-3 sm:grid-cols-2 md:grid-cols-3">
            {Array.from({ length: 3 }).map((_, i) => (
              <SkeletonCard key={i} />
            ))}
          </div>
        ) : !groups || groups.length === 0 ? (
          <p className="text-text-muted py-6">
            비교 그룹이 없습니다. 그룹을 만들고 관심 상품을 넣어 가격을 비교해 보세요.
          </p>
        ) : (
          <div className="grid gap-3 sm:grid-cols-2 md:grid-cols-3">
            {groups.map((g) => (
              <button
                key={g.groupId}
                type="button"
                onClick={() => navigate(`/watchlist/groups/${g.groupId}`)}
                className="flex flex-col rounded-2xl border border-border bg-white dark:bg-[var(--surface)] p-4 text-left hover:border-primary transition-colors"
              >
                <p className="font-semibold text-text-main mb-1">{g.name}</p>
                <p className="text-sm text-text-muted mb-3">상품 {g.itemCount}개</p>
                <div className="flex gap-1 overflow-hidden">
                  {g.previewItems.slice(0, 4).map((preview) => (
                    <div
                      key={preview.productId}
                      className="w-10 h-10 rounded-lg bg-gray-100 dark:bg-gray-800 shrink-0 overflow-hidden"
                    >
                      {preview.imageUrl ? (
                        <img
                          src={preview.imageUrl}
                          alt=""
                          className="w-full h-full object-contain"
                        />
                      ) : (
                        <div className="w-full h-full flex items-center justify-center text-text-muted">
                          <span className="material-symbols-outlined text-lg">image</span>
                        </div>
                      )}
                    </div>
                  ))}
                  {g.itemCount > 4 && (
                    <span className="text-xs text-text-muted self-center">+{g.itemCount - 4}</span>
                  )}
                </div>
              </button>
            ))}
          </div>
        )}
      </section>

      <section className="mb-10">
        <h2 className="text-lg font-semibold text-text-main mb-3">내 관심 상품</h2>
        {isLoading ? (
          <div className="grid gap-4 sm:grid-cols-1">
            {Array.from({ length: 3 }).map((_, i) => (
              <SkeletonCard key={i} />
            ))}
          </div>
        ) : !items || items.length === 0 ? (
          <p className="text-text-muted py-6">
            관심 목록이 비어 있습니다. 상품 검색 후 관심 목록에 추가해 보세요.
          </p>
        ) : (
          <div className="flex flex-col gap-4">
            {items.map((item) => (
              <WatchlistCardWithPrice key={item.watchlistId} item={item} />
            ))}
          </div>
        )}
      </section>

      {showCreateGroup && (
        <CreateGroupModal
          onClose={() => setShowCreateGroup(false)}
          onCreated={() => queryClient.invalidateQueries({ queryKey: ["watchlist", "groups"] })}
        />
      )}
    </div>
  );
}
