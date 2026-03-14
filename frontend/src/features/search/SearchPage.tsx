import { useLocation, useNavigate } from "react-router-dom";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { productApi } from "@/services/productApi";
import { useAuthStore } from "@/stores/authStore";
import { useToastStore } from "@/stores/toastStore";
import type { ProductSummaryRes } from "@/lib/types";
import { Button } from "@/components/ui/Button";

/** 외부 검색 결과에서 상품을 선택해 DB에 저장한 뒤 상세로 이동 */
export function SearchPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.add);
  const isAuth = useAuthStore((s) => s.isAuthenticated());
  const product = location.state?.product as ProductSummaryRes | undefined;

  const selectMutation = useMutation({
    mutationFn: (p: ProductSummaryRes) =>
      productApi.select({
        productId: p.productId ?? undefined,
        brand: p.brand ?? undefined,
        title: p.title,
        price: typeof p.price === "number" ? p.price : Number(p.price),
        mallName: p.mallName ?? undefined,
        naverProductId: p.naverProductId ?? undefined,
        externalKey: p.externalKey ?? undefined,
        url: p.url ?? undefined,
        imageUrl: p.imageUrl ?? undefined,
        categoryPath: p.categoryPath ?? undefined,
      }),
    onSuccess: (res) => {
      addToast(res.created ? "상품이 등록되었습니다." : "이미 등록된 상품입니다.", "success");
      queryClient.invalidateQueries({ queryKey: ["products"] });
      if (res.product.productId)
        navigate(`/products/${res.product.productId}`, { replace: true });
    },
    onError: (err) => addToast(String(err), "error"),
  });

  if (!product) {
    return (
      <div className="max-w-[1200px] mx-auto px-6 py-8">
        <p className="text-text-muted">상품 정보가 없습니다.</p>
        <Button
          variant="outline"
          className="mt-4"
          onClick={() => navigate("/")}
        >
          홈으로
        </Button>
      </div>
    );
  }

  const handleSelect = () => {
    if (!isAuth) {
      addToast("로그인 후 이용해 주세요.", "info");
      navigate("/login", { state: { from: location } });
      return;
    }
    selectMutation.mutate(product);
  };

  const price =
    typeof product.price === "number" ? product.price : Number(product.price);

  return (
    <div className="max-w-[1200px] mx-auto px-6 py-8">
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
            <span className="text-sm text-text-muted font-medium">
              {product.brand}
            </span>
          )}
          <h1 className="text-xl font-bold text-text-main mt-1">{product.title}</h1>
          <p className="text-2xl font-bold text-primary mt-2">
            {price.toLocaleString()}원
          </p>
          {product.mallName && (
            <p className="text-text-muted text-sm mt-1">{product.mallName}</p>
          )}
          <div className="mt-6 flex gap-3">
            <Button
              onClick={handleSelect}
              loading={selectMutation.isPending}
              size="lg"
            >
              관심 목록에 추가
            </Button>
            <Button variant="outline" onClick={() => navigate("/")}>
              다시 검색
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
