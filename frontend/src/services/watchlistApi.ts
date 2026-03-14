import { api, unwrapData } from "@/lib/api";
import type { ApiResponse } from "@/lib/types";
import type {
  WatchlistRes,
  UpdateTargetPriceReq,
  WatchlistGroupRes,
  WatchlistGroupDetailRes,
  CreateWatchlistGroupReq,
  RenameWatchlistGroupReq,
} from "@/lib/types";

function checkSuccess(res: { data?: { success?: boolean; message?: string } }) {
  if (res.data?.success === false)
    throw new Error(String(res.data?.message ?? "요청에 실패했습니다."));
}

export const watchlistApi = {
  /** GET /watchlist */
  getMine: () =>
    api
      .get<ApiResponse<WatchlistRes[]>>("/watchlist")
      .then((res) => unwrapData(res)),

  /** POST /watchlist/:productId */
  add: (productId: number) =>
    api.post<ApiResponse<null>>(`/watchlist/${productId}`).then((res) => {
      checkSuccess(res);
      return undefined;
    }),

  /** DELETE /watchlist/:productId */
  remove: (productId: number) =>
    api.delete<ApiResponse<null>>(`/watchlist/${productId}`).then((res) => {
      checkSuccess(res);
      return undefined;
    }),

  /** PATCH /watchlist/:productId */
  updateTargetPrice: (productId: number, body: UpdateTargetPriceReq) =>
    api.patch<ApiResponse<null>>(`/watchlist/${productId}`, body).then((res) => {
      checkSuccess(res);
      return undefined;
    }),

  /** GET /watchlist/groups */
  getGroups: () =>
    api
      .get<ApiResponse<WatchlistGroupRes[]>>("/watchlist/groups")
      .then((res) => unwrapData(res)),

  /** POST /watchlist/groups */
  createGroup: (body: CreateWatchlistGroupReq) =>
    api
      .post<ApiResponse<WatchlistGroupRes>>("/watchlist/groups", body)
      .then((res) => unwrapData(res)),

  /** GET /watchlist/groups/:groupId?days=30 */
  getGroupDetail: (groupId: number, days = 30) =>
    api
      .get<ApiResponse<WatchlistGroupDetailRes>>(`/watchlist/groups/${groupId}`, {
        params: { days },
      })
      .then((res) => unwrapData(res)),

  /** PATCH /watchlist/groups/:groupId */
  renameGroup: (groupId: number, body: RenameWatchlistGroupReq) =>
    api
      .patch<ApiResponse<WatchlistGroupRes>>(`/watchlist/groups/${groupId}`, body)
      .then((res) => unwrapData(res)),

  /** DELETE /watchlist/groups/:groupId */
  deleteGroup: (groupId: number) =>
    api.delete<ApiResponse<null>>(`/watchlist/groups/${groupId}`).then((res) => {
      checkSuccess(res);
      return undefined;
    }),

  /** POST /watchlist/groups/:groupId/items */
  addGroupItem: (groupId: number, productId: number) =>
    api
      .post<ApiResponse<WatchlistGroupDetailRes>>(
        `/watchlist/groups/${groupId}/items`,
        { productId }
      )
      .then((res) => unwrapData(res)),

  /** DELETE /watchlist/groups/:groupId/items/:productId */
  removeGroupItem: (groupId: number, productId: number) =>
    api
      .delete<ApiResponse<WatchlistGroupDetailRes>>(
        `/watchlist/groups/${groupId}/items/${productId}`
      )
      .then((res) => unwrapData(res)),
};
