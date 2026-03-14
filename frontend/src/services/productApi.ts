import { api, unwrapData } from "@/lib/api";
import type { ApiResponse } from "@/lib/types";
import type {
  ProductSearchRes,
  ProductListRes,
  ProductSummaryRes,
  ProductSelectReq,
  ProductSelectRes,
} from "@/lib/types";

export const productApi = {
  /** GET /products/search?q=&page=&size= (개별 상품 단위) */
  search: (q: string, page = 0, size = 20) =>
    api
      .get<ApiResponse<ProductSearchRes>>("/products/search", {
        params: { q, page, size },
      })
      .then((res) => unwrapData(res)),

  /** GET /products/search/external?q= */
  searchExternal: (q: string) =>
    api
      .get<ApiResponse<ProductSearchRes>>("/products/search/external", {
        params: { q },
      })
      .then((res) => unwrapData(res)),

  /** POST /products/select */
  select: (body: ProductSelectReq) =>
    api
      .post<ApiResponse<ProductSelectRes>>("/products/select", body)
      .then((res) => unwrapData(res)),

  /** GET /products/:id - 상품 단일 조회 */
  getProduct: (id: number) =>
    api
      .get<ApiResponse<ProductSummaryRes>>(`/products/${id}`)
      .then((res) => unwrapData(res)),

  /** GET /products/latest?page=&size= - 최신 등록 순 상품 목록 */
  getLatest: (page = 0, size = 20) =>
    api
      .get<ApiResponse<ProductListRes>>("/products/latest", {
        params: { page, size },
      })
      .then((res) => unwrapData(res)),
};
