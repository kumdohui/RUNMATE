export const normalizePhoneNumber = (value) => {
  const phoneNumber = value.trim()
  if (!/^01[016789](?:\d{7,8}|-\d{3,4}-\d{4})$/.test(phoneNumber)) {
    return null
  }
  return phoneNumber.replaceAll('-', '')
}

export const openTerms = (router, type) => {
  const url = router.resolve(`/terms/${type}/1.0`).href
  window.open(url, '_blank')
}
