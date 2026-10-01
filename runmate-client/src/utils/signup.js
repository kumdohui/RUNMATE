export const normalizePhoneNumber = (value) => {
  const phoneNumber = value.trim()
  if (!/^01[016789](?:\d{7,8}|-\d{3,4}-\d{4})$/.test(phoneNumber)) {
    return null
  }
  return phoneNumber.replaceAll('-', '')
}

export const fetchCurrentTerms = async () => {
  const response = await fetch('/api/terms/current')
  const body = await response.json().catch(() => [])
  if (!response.ok) throw new Error('약관 정보를 불러오지 못했습니다.')
  return Array.isArray(body) ? body : []
}

export const selectedTermConsents = (terms, selections) =>
  terms
    .filter((term) => selections[term.termCode])
    .map((term) => ({
      termId: term.termId,
      contentHash: term.contentHash,
    }))

export const openTerms = (router, type, terms = []) => {
  const code = type.toUpperCase()
  const term = terms.find((item) => item.termCode === code)
  const url = term?.contentUrl || router.resolve(`/terms/${type}/${term?.version || '1.0'}`).href
  window.open(url, '_blank')
}
