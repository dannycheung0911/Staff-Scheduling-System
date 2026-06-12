import http from './http'

export const scheduleApi = {
  upload: (file, onProgress) => {
    const form = new FormData()
    form.append('file', file)
    return http.post('/schedule/upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: onProgress
    })
  },
  listFiles: () => http.get('/schedule/files'),
  getRecords: (fileId) => http.get(`/schedule/records/${fileId}`),
  getCounts: (fileId) => http.get(`/schedule/counts/${fileId}`),
  getAlerts: (fileId) => http.get(`/schedule/alerts/${fileId}`),
  updateCell: (recordId, shiftCode) => http.put(`/schedule/record/${recordId}`, { shiftCode }),
  deleteFile: (fileId) => http.delete(`/schedule/file/${fileId}`)
}
