import { saveAs } from "file-saver"

/**
 * 格式化文件大小
 * @param size 
 * @returns 
 */
export const formatSize = (size?: number) => {
    if (!size) return '未知'
    if (size < 1024) return size + 'B'
    if (size < 1024 * 1024) return (size / 1024).toFixed(2) + 'KB'
    return (size / (1024 * 1024)).toFixed(2) + 'MB'
}

/**
 * 下载图片
 * @param url 
 * @param filename 
 * @returns 
 */
export function downloadImage(url?: string, filename?: string) {
    if (!url) {
        return
    }
    saveAs(url, filename)
}