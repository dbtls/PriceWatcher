import { api, unwrapData } from "@/lib/api";
import type { ApiResponse } from "@/lib/types";
import type { PriceHistoryItemRes } from "@/lib/types";

export const priceApi = {
  /** GET /products/:id/price-history?days= */
  getPriceHistory: (productId: number, days = 30) =>
    api
      .get<ApiResponse<PriceHistoryItemRes[]>>(
        `/products/${productId}/price-history`,
        { params: { days } }
      )
      .then((res) => unwrapData(res)),
};
