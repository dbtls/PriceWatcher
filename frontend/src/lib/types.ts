/** 백엔드 ResponseDto 래퍼 */
export interface ApiResponse<T> {
  success: boolean;
  code: string;
  message: string;
  data: T | null;
  timestamp: string;
}

/** 로그인 응답 - LoginRes */
export interface LoginRes {
  accessToken: string;
  tokenType: string;
}

/** 프로필 - GET /users/me → ProfileRes */
export interface ProfileRes {
  id: number;
  email: string;
  nickname: string;
  role: string;
  status: string;
}

/** 상품 요약 - ProductSummaryRes */
export interface ProductSummaryRes {
  productId: number | null;
  brand: string | null;
  title: string;
  price: number;
  mallName: string | null;
  naverProductId: string | null;
  externalKey: string | null;
  url: string | null;
  imageUrl: string | null;
  categoryPath: string | null;
}

/** 상품 검색 응답 - ProductSearchRes (개별 상품 단위) */
export interface ProductSearchRes {
  internalResults: ProductSummaryRes[];
  externalResults: ProductSummaryRes[];
  degraded: boolean;
  page: number;
  size: number;
  totalCount: number;
  hasNext: boolean;
}

/** 상품 목록 응답 - ProductListRes (최신순) */
export interface ProductListRes {
  items: ProductSummaryRes[];
  page: number;
  size: number;
  totalCount: number;
  hasNext: boolean;
}

/** 상품 추천 응답 - GET /products/:id/recommendations */
export interface ProductRecommendationsRes {
  brandSimilarProducts: ProductSummaryRes[];
  similarProducts: ProductSummaryRes[];
}

/** 상품 선택 요청 - ProductSelectReq */
export interface ProductSelectReq {
  productId?: number | null;
  brand?: string | null;
  title: string;
  price: number;
  mallName?: string | null;
  naverProductId?: string | null;
  externalKey?: string | null;
  url?: string | null;
  imageUrl?: string | null;
  categoryPath?: string | null;
}

/** 상품 선택 응답 - ProductSelectRes */
export interface ProductSelectRes {
  product: ProductSummaryRes;
  created: boolean;
}

/** 마이페이지 요약 - GET /users/me/summary */
export interface MyPageSummaryRes {
  profile: ProfileRes;
  watchlistCount: number;
  watchlistGroupCount: number;
  unreadNotificationCount: number;
  targetReachedCount: number;
  recentDrops: MyPageRecentDropRes[];
}

/** 마이페이지 최근 가격 하락 - MyPageRecentDropRes */
export interface MyPageRecentDropRes {
  productId: number;
  title: string;
  imageUrl: string | null;
  currentPrice: number;
  previousPrice: number;
  dropAmount: number;
  dropRatePercent: number;
}

/** 워치리스트 항목 - WatchlistRes */
export interface WatchlistRes {
  watchlistId: number;
  productId: number;
  title: string;
  imageUrl: string | null;
  targetPrice: number;
}

/** 워치리스트 그룹 목록 항목 - WatchlistGroupRes */
export interface WatchlistGroupRes {
  groupId: number;
  name: string;
  itemCount: number;
  previewItems: WatchlistGroupPreviewItemRes[];
}

/** 워치리스트 그룹 미리보기 아이템 - WatchlistGroupPreviewItemRes */
export interface WatchlistGroupPreviewItemRes {
  productId: number;
  title: string;
  imageUrl: string | null;
}

/** 워치리스트 그룹 상세 - WatchlistGroupDetailRes */
export interface WatchlistGroupDetailRes {
  groupId: number;
  name: string;
  itemCount: number;
  items: WatchlistGroupItemRes[];
}

/** 워치리스트 그룹 상세 아이템 - WatchlistGroupItemRes */
export interface WatchlistGroupItemRes {
  productId: number;
  brand: string | null;
  title: string;
  mallName: string | null;
  imageUrl: string | null;
  currentPrice: number;
  targetPrice: number;
  lowestPrice: number;
  url: string | null;
  priceHistory: PriceHistoryItemRes[];
}

/** 그룹 생성 요청 - CreateWatchlistGroupReq */
export interface CreateWatchlistGroupReq {
  name: string;
}

/** 그룹 이름 변경 요청 - RenameWatchlistGroupReq */
export interface RenameWatchlistGroupReq {
  name: string;
}

/** 그룹에 상품 추가 요청 - AddWatchlistGroupItemReq */
export interface AddWatchlistGroupItemReq {
  productId: number;
}

/** 목표가 변경 요청 - UpdateTargetPriceReq */
export interface UpdateTargetPriceReq {
  targetPrice: number;
}

/** 가격 이력 항목 - PriceHistoryItemRes */
export interface PriceHistoryItemRes {
  capturedAt: string;
  price: number;
}

/** 알림 - NotificationRes */
export interface NotificationRes {
  id: number;
  type: string;
  message: string;
  isRead: boolean;
  productId: number | null;
}
