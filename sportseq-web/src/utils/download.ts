import request from '@/api/request'

/**
 * 带认证头的文件下载：后端返回二进制流。
 */
export async function downloadFile(url: string, filename: string): Promise<void> {
  const response = await request.get(url, { responseType: 'blob' })
  const blob = new Blob([response.data as BlobPart])
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = filename
  link.click()
  URL.revokeObjectURL(link.href)
}
