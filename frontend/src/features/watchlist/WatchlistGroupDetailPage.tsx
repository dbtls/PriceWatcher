import { useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { watchlistApi } from "@/services/watchlistApi";
import { useToastStore } from "@/stores/toastStore";
import type { WatchlistGroupItemRes, WatchlistRes } from "@/lib/types";
import { Button } from "@/components/ui/Button";
import { Skeleton } from "@/components/ui/Skeleton";
import { PriceTrendChart } from "@/components/charts/PriceTrendChart";

const DAYS = 30;

function GroupItemRow({
  item,
  groupId,
  onRemoved,
}: {
  item: WatchlistGroupItemRes;
  groupId: number;
  onRemoved: () => void;
}) {
  const navigate = useNavigate();
  const addToast = useToastStore((s) => s.add);

  const removeMutation = useMutation({
    mutationFn: () => watchlistApi.removeGroupItem(groupId, item.productId),
    onSuccess: () => {
      addToast("그룹에서 제거되었습니다.", "success");
      onRemoved();
    },
    onError: (err) => addToast(String(err), "error"),
  });

  const currentPrice = typeof item.currentPrice === "number" ? item.currentPrice : Number(item.currentPrice);
  const targetPrice = typeof item.targetPrice === "number" ? item.targetPrice : Number(item.targetPrice);
  const lowestPrice = typeof item.lowestPrice === "number" ? item.lowestPrice : Number(item.lowestPrice);

  return (
    <article className="rounded-2xl border border-border bg-white dark:bg-[var(--surface)] overflow-hidden">
      <div className="flex flex-col md:flex-row gap-4 p-4">
        <div
          className="w-full md:w-36 aspect-square bg-gray-100 dark:bg-gray-800 rounded-xl shrink-0 cursor-pointer overflow-hidden"
          onClick={() => navigate(`/products/${item.productId}`)}
        >
          {item.imageUrl ? (
            <img src={item.imageUrl} alt="" className="w-full h-full object-contain p-2" />
          ) : (
            <div className="w-full h-full flex items-center justify-center text-text-muted">
              <span className="material-symbols-outlined text-4xl">image</span>
            </div>
          )}
        </div>
        <div className="flex-1 min-w-0">
          {item.brand && (
            <span className="text-xs text-text-muted font-medium">{item.brand}</span>
          )}
          <h3
            className="font-semibold text-text-main line-clamp-2 cursor-pointer hover:text-primary"
            onClick={() => navigate(`/products/${item.productId}`)}
          >
            {item.title}
          </h3>
          {item.mallName && (
            <p className="text-sm text-text-muted mt-0.5">{item.mallName}</p>
          )}
          <div className="mt-3 flex flex-wrap gap-4 text-sm">
            <span>
              <span className="text-text-muted">현재가</span>{" "}
              <span className="font-bold text-primary">{currentPrice.toLocaleString()}원</span>
            </span>
            <span>
              <span className="text-text-muted">목표가</span>{" "}
              <span className="font-medium">{targetPrice.toLocaleString()}원</span>
            </span>
            <span>
              <span className="text-text-muted">최저가</span>{" "}
              <span className="font-medium">{lowestPrice.toLocaleString()}원</span>
            </span>
          </div>
          {item.url && (
            <a
              href={item.url}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-1 mt-2 text-sm text-primary hover:underline"
            >
              쇼핑몰 보기
              <span className="material-symbols-outlined text-base">open_in_new</span>
            </a>
          )}
          <div className="mt-3 flex flex-wrap gap-2">
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
              그룹에서 제거
            </Button>
          </div>
        </div>
      </div>
      {item.priceHistory && item.priceHistory.length > 0 && (
        <div className="border-t border-border px-4 py-3 bg-gray-50/50 dark:bg-gray-800/30">
          <p className="text-xs text-text-muted mb-2">가격 이력 (최근 {item.priceHistory.length}건)</p>
          <div className="flex flex-wrap gap-2">
            {item.priceHistory.slice(0, 14).map((h, i) => (
              <span
                key={i}
                className="text-xs px-2 py-1 rounded bg-white dark:bg-[var(--surface)] border border-border"
              >
                {typeof h.capturedAt === "string" ? h.capturedAt.slice(0, 10) : String(h.capturedAt)}{" "}
                {typeof h.price === "number" ? h.price.toLocaleString() : Number(h.price).toLocaleString()}원
              </span>
            ))}
            {item.priceHistory.length > 14 && (
              <span className="text-xs text-text-muted self-center">+{item.priceHistory.length - 14}건</span>
            )}
          </div>
        </div>
      )}
    </article>
  );
}

function AddItemModal({
  groupId,
  watchlistItems,
  existingProductIds,
  onClose,
  onAdded,
}: {
  groupId: number;
  watchlistItems: WatchlistRes[];
  existingProductIds: Set<number>;
  onClose: () => void;
  onAdded: () => void;
}) {
  const addToast = useToastStore((s) => s.add);
  const candidates = watchlistItems.filter((w) => !existingProductIds.has(w.productId));

  const addMutation = useMutation({
    mutationFn: (productId: number) => watchlistApi.addGroupItem(groupId, productId),
    onSuccess: () => {
      addToast("그룹에 추가되었습니다.", "success");
      onAdded();
      onClose();
    },
    onError: (err) => addToast(String(err), "error"),
  });

  if (candidates.length === 0) {
    return (
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50" onClick={onClose} role="dialog" aria-modal="true">
        <div className="bg-white dark:bg-[var(--surface)] rounded-2xl border border-border shadow-xl max-w-md w-full p-6" onClick={(e) => e.stopPropagation()}>
          <h2 className="text-lg font-bold text-text-main mb-4">상품 추가</h2>
          <p className="text-text-muted">추가할 수 있는 관심 상품이 없습니다. 그룹에 없는 관심 상품만 추가할 수 있습니다.</p>
          <Button className="mt-4" onClick={onClose}>확인</Button>
        </div>
      </div>
    );
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50" onClick={onClose} role="dialog" aria-modal="true">
      <div className="bg-white dark:bg-[var(--surface)] rounded-2xl border border-border shadow-xl max-w-lg w-full max-h-[80vh] flex flex-col" onClick={(e) => e.stopPropagation()}>
        <div className="p-4 border-b border-border">
          <h2 className="text-lg font-bold text-text-main">그룹에 상품 추가</h2>
          <p className="text-sm text-text-muted mt-1">추가할 관심 상품을 선택하세요.</p>
        </div>
        <ul className="overflow-y-auto flex-1 p-2">
          {candidates.map((w) => (
            <li key={w.watchlistId}>
              <button
                type="button"
                onClick={() => addMutation.mutate(w.productId)}
                disabled={addMutation.isPending}
                className="w-full flex items-center gap-3 p-3 rounded-xl hover:bg-gray-100 dark:hover:bg-gray-800 text-left transition-colors"
              >
                {w.imageUrl ? (
                  <img src={w.imageUrl} alt="" className="w-12 h-12 rounded-lg object-contain bg-gray-100 dark:bg-gray-800" />
                ) : (
                  <div className="w-12 h-12 rounded-lg bg-gray-100 dark:bg-gray-800 flex items-center justify-center text-text-muted">
                    <span className="material-symbols-outlined">image</span>
                  </div>
                )}
                <span className="flex-1 font-medium text-text-main truncate">{w.title}</span>
                <span className="material-symbols-outlined text-text-muted">add</span>
              </button>
            </li>
          ))}
        </ul>
        <div className="p-4 border-t border-border">
          <Button variant="outline" className="w-full" onClick={onClose}>취소</Button>
        </div>
      </div>
    </div>
  );
}

export function WatchlistGroupDetailPage() {
  const { groupId: groupIdParam } = useParams<{ groupId: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.add);
  const [showRename, setShowRename] = useState(false);
  const [renameValue, setRenameValue] = useState("");
  const [showAddItem, setShowAddItem] = useState(false);

  const groupId = groupIdParam ? Number(groupIdParam) : NaN;
  const isValidId = Number.isInteger(groupId) && groupId > 0;

  const { data: detail, isLoading } = useQuery({
    queryKey: ["watchlist", "groups", groupId, DAYS],
    queryFn: () => watchlistApi.getGroupDetail(groupId, DAYS),
    enabled: isValidId,
  });

  const { data: watchlistItems } = useQuery({
    queryKey: ["watchlist"],
    queryFn: () => watchlistApi.getMine(),
    enabled: isValidId && showAddItem,
  });

  const renameMutation = useMutation({
    mutationFn: (name: string) => watchlistApi.renameGroup(groupId, { name }),
    onSuccess: () => {
      addToast("그룹 이름이 변경되었습니다.", "success");
      queryClient.invalidateQueries({ queryKey: ["watchlist", "groups"] });
      queryClient.invalidateQueries({ queryKey: ["watchlist", "groups", groupId] });
      setShowRename(false);
    },
    onError: (err) => addToast(String(err), "error"),
  });

  const deleteMutation = useMutation({
    mutationFn: () => watchlistApi.deleteGroup(groupId),
    onSuccess: () => {
      addToast("그룹이 삭제되었습니다.", "success");
      queryClient.invalidateQueries({ queryKey: ["watchlist", "groups"] });
      navigate("/watchlist#groups");
    },
    onError: (err) => addToast(String(err), "error"),
  });

  const handleRenameSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const name = renameValue.trim();
    if (!name) {
      addToast("그룹 이름을 입력하세요.", "error");
      return;
    }
    renameMutation.mutate(name);
  };

  if (!isValidId) {
    return (
      <div className="max-w-[1200px] mx-auto px-6 py-8">
        <p className="text-text-muted">잘못된 그룹입니다.</p>
        <Button variant="outline" className="mt-4" onClick={() => navigate("/watchlist")}>
          관심 목록으로
        </Button>
      </div>
    );
  }

  if (isLoading || !detail) {
    return (
      <div className="max-w-[1200px] mx-auto px-6 py-8">
        <Skeleton className="h-8 w-48 mb-4" />
        <Skeleton className="h-32 w-full" />
      </div>
    );
  }

  const existingProductIds = new Set(detail.items.map((i) => i.productId));
  const chartSeries = detail.items
    .filter((item) => item.priceHistory && item.priceHistory.length > 0)
    .map((item, index) => ({
      id: `group-item-${item.productId}`,
      name: item.mallName ? `${item.title} (${item.mallName})` : item.title,
      color: ["#2563eb", "#f97316", "#16a34a", "#dc2626", "#7c3aed", "#0891b2"][index % 6],
      points: item.priceHistory,
    }));

  return (
    <div className="max-w-[1200px] mx-auto px-6 py-8">
      <Button
        variant="ghost"
        size="sm"
        className="mb-4 -ml-2"
        onClick={() => navigate("/watchlist#groups")}
      >
        <span className="material-symbols-outlined mr-1">arrow_back</span>
        비교 그룹 목록
      </Button>

      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
        <div className="flex items-center gap-3">
          {!showRename ? (
            <>
              <h1 className="text-2xl font-bold text-text-main">{detail.name}</h1>
              <button
                type="button"
                onClick={() => {
                  setRenameValue(detail.name);
                  setShowRename(true);
                }}
                className="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 text-text-muted"
                title="이름 변경"
              >
                <span className="material-symbols-outlined text-lg">edit</span>
              </button>
            </>
          ) : (
            <form onSubmit={handleRenameSubmit} className="flex items-center gap-2">
              <input
                type="text"
                value={renameValue}
                onChange={(e) => setRenameValue(e.target.value)}
                className="px-3 py-2 rounded-xl border border-border bg-white dark:bg-[var(--surface)] text-text-main w-64"
                autoFocus
              />
              <Button type="submit" size="sm" loading={renameMutation.isPending}>
                저장
              </Button>
              <Button type="button" variant="outline" size="sm" onClick={() => setShowRename(false)}>
                취소
              </Button>
            </form>
          )}
        </div>
        <div className="flex gap-2">
          <Button variant="outline" size="sm" onClick={() => setShowAddItem(true)}>
            <span className="material-symbols-outlined text-lg mr-1">add</span>
            상품 추가
          </Button>
          <Button
            variant="danger"
            size="sm"
            onClick={() => {
              if (window.confirm("이 그룹을 삭제할까요?")) deleteMutation.mutate();
            }}
            loading={deleteMutation.isPending}
          >
            그룹 삭제
          </Button>
        </div>
      </div>

      <p className="text-text-muted mb-6">상품 {detail.itemCount}개 · 현재가·목표가·최저가·가격 이력 비교</p>

      {chartSeries.length > 0 && (
        <section className="mb-8">
          <h2 className="text-lg font-semibold text-text-main mb-4">그룹 가격 추이 비교</h2>
          <PriceTrendChart series={chartSeries} height={360} />
        </section>
      )}

      {detail.items.length === 0 ? (
        <div className="rounded-2xl border border-border bg-white dark:bg-[var(--surface)] p-12 text-center">
          <p className="text-text-muted mb-4">이 그룹에 상품이 없습니다.</p>
          <Button onClick={() => setShowAddItem(true)}>관심 상품 추가하기</Button>
        </div>
      ) : (
        <div className="flex flex-col gap-4">
          {detail.items.map((item) => (
            <GroupItemRow
              key={item.productId}
              item={item}
              groupId={groupId}
              onRemoved={() => {
                queryClient.invalidateQueries({ queryKey: ["watchlist", "groups", groupId] });
                queryClient.invalidateQueries({ queryKey: ["watchlist", "groups"] });
              }}
            />
          ))}
        </div>
      )}

      {showAddItem && watchlistItems && (
        <AddItemModal
          groupId={groupId}
          watchlistItems={watchlistItems}
          existingProductIds={existingProductIds}
          onClose={() => setShowAddItem(false)}
          onAdded={() => {
            queryClient.invalidateQueries({ queryKey: ["watchlist", "groups", groupId] });
            queryClient.invalidateQueries({ queryKey: ["watchlist", "groups"] });
          }}
        />
      )}
    </div>
  );
}
