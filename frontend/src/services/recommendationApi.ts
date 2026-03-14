import { api, unwrapData } from "@/lib/api";
import type { ApiResponse } from "@/lib/types";
import type { ProductRecommendationsRes } from "@/lib/types";

export const recommendationApi = {
  /** GET /products/:id/recommendations?limit= */
  getRecommendations: (productId: number, limit = 10) =>
    api
      .get<ApiResponse<ProductRecommendationsRes>>(
        `/products/${productId}/recommendations`,
        { params: { limit } }
      )
      .then((res) => unwrapData(res)),
};
