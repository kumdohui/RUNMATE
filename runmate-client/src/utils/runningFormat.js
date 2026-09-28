export const formatDistance = (value) => {
  if (value == null) return null
  return `${Number(Number(value).toFixed(3))} km`
}

export const formatDuration = (totalSeconds) => {
  if (!totalSeconds) return null
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  return seconds ? `${minutes}분 ${String(seconds).padStart(2, '0')}초` : `${minutes}분`
}

export const formatPace = (distanceKm, durationSeconds) => {
  if (!distanceKm || !durationSeconds) return null
  const secondsPerKm = Math.round(durationSeconds / distanceKm)
  const minutes = Math.floor(secondsPerKm / 60)
  const seconds = secondsPerKm % 60
  return `${minutes}'${String(seconds).padStart(2, '0')}\" /km`
}
