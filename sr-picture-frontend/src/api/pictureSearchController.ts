// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** searchByAI POST /api/picture/search/ai */
export async function searchByAiUsingPost(
  body: API.AISearchRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListPictureVO_>('/api/picture/search/ai', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
