export function parseApiDate(value) {
  if (!value) return null

  const normalized = String(value).replace(' ', 'T')
  const date = new Date(normalized)
  return Number.isNaN(date.getTime()) ? null : date
}

export function formatActivityDate(value) {
  const date = parseApiDate(value)
  if (!date) return '时间待定'

  return `${date.getMonth() + 1} 月 ${date.getDate()} 日 · ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

export function formatActivityDateRange(start, end) {
  const startDate = parseApiDate(start)
  if (!startDate) return '时间待定'

  const endDate = parseApiDate(end)
  const dateText = `${startDate.getMonth() + 1} 月 ${startDate.getDate()} 日`
  const startTime = `${String(startDate.getHours()).padStart(2, '0')}:${String(startDate.getMinutes()).padStart(2, '0')}`
  const endTime = endDate
    ? `${String(endDate.getHours()).padStart(2, '0')}:${String(endDate.getMinutes()).padStart(2, '0')}`
    : ''

  return `${dateText} · ${startTime}${endTime ? ` - ${endTime}` : ''}`
}

export function formatActivityDay(value) {
  const date = parseApiDate(value)
  return date ? String(date.getDate()).padStart(2, '0') : '--'
}

export function formatActivityMonth(value) {
  const date = parseApiDate(value)
  return date ? date.toLocaleString('en-US', { month: 'short' }).toUpperCase() : '---'
}

export function formatActivityTime(value) {
  const date = parseApiDate(value)
  return date
    ? `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
    : '时间待定'
}
