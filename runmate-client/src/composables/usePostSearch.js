import { reactive, ref } from 'vue'
import { fetchCategories, fetchPosts } from '../api/postSearchApi'
import { getDefaultPostImage } from '../default-post-images'

const distanceRanges = {
  under3: { max: '3' },
  from3to5: { min: '3', max: '5' },
  from5to10: { min: '5', max: '10' },
  from10to15: { min: '10', max: '15' },
  from15to20: { min: '15', max: '20' },
  from20to25: { min: '20', max: '25' },
  from25to30: { min: '25', max: '30' },
  from30to35: { min: '30', max: '35' },
  from35to40: { min: '35', max: '40' },
  over40: { min: '40' },
}

const initialFilters = () => ({
  keyword: '',
  categoryId: '',
  region: '',
  distanceRange: '',
  tag: '',
  sort: 'latest',
})

export const usePostSearch = () => {
  const route = useRoute()
  const router = useRouter()
  const queryValue = (value) => (Array.isArray(value) ? value[0] : value || '')
  const categories = ref([])
  const posts = ref([])
  const currentPage = ref(0)
  const totalPages = ref(0)
  const totalElements = ref(0)
  const loading = ref(false)
  const searched = ref(false)
  const message = ref('')
  const filters = reactive({
    ...initialFilters(),
    keyword: queryValue(route.query.keyword),
    categoryId: queryValue(route.query.categoryId),
    region: queryValue(route.query.region),
    distanceRange: queryValue(route.query.distanceRange),
    tag: queryValue(route.query.tag),
    sort: queryValue(route.query.sort) || 'latest',
  })

  const createParams = (page) => {
    const params = new URLSearchParams({
      page: String(page),
      sort: filters.sort,
    })
    const distance = distanceRanges[filters.distanceRange]
    if (filters.keyword.trim()) params.set('keyword', filters.keyword.trim())
    if (filters.categoryId) params.set('categoryId', filters.categoryId)
    if (filters.region) params.set('region', filters.region)
    if (filters.distanceRange) params.set('distanceRange', filters.distanceRange)
    if (filters.tag.trim()) params.set('tag', filters.tag.trim())
    if (distance?.min) params.set('minDistance', distance.min)
    if (distance?.max) params.set('maxDistance', distance.max)
    return params
  }

  const saveSearchState = (page) =>
    router.replace({
      path: '/courses',
      query: Object.fromEntries(createParams(page).entries()),
    })

  const loadCategories = async () => {
    try {
      categories.value = await fetchCategories()
    } catch {
      categories.value = []
    }
  }

  const loadPosts = async (page = 0, saveState = true) => {
    loading.value = true
    message.value = ''
    try {
      if (saveState) await saveSearchState(page)
      const data = await fetchPosts(createParams(page))
      posts.value = (data.courses ?? []).map((post) => ({
        ...post,
        imageUrl: post.imageUrl || getDefaultPostImage(post.categoryName),
      }))
      currentPage.value = data.currentPage ?? 0
      totalPages.value = data.totalPages ?? 0
      totalElements.value = data.totalElements ?? 0
    } catch (error) {
      posts.value = []
      message.value = error.message
    } finally {
      loading.value = false
    }
  }

  const hasCondition = () =>
    Boolean(
      filters.keyword.trim() ||
        filters.categoryId ||
        filters.region ||
        filters.distanceRange ||
        filters.tag.trim(),
    )

  const clearResults = () => {
    posts.value = []
    currentPage.value = 0
    totalPages.value = 0
    totalElements.value = 0
  }

  const search = async () => {
    if (!hasCondition()) {
      message.value = ''
      searched.value = false
      clearResults()
      return
    }
    searched.value = true
    await loadPosts(0)
  }

  const reset = () => {
    Object.assign(filters, initialFilters())
    searched.value = false
    clearResults()
    message.value = ''
    router.replace({ path: '/courses' })
  }

  const movePage = async (page) => {
    if (page < 0 || page >= totalPages.value || page === currentPage.value) return
    await loadPosts(page)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const restore = async () => {
    await loadCategories()
    if (!hasCondition()) return
    searched.value = true
    const requestedPage = Number.parseInt(queryValue(route.query.page), 10)
    await loadPosts(Number.isFinite(requestedPage) && requestedPage >= 0 ? requestedPage : 0, false)
  }

  return {
    categories,
    posts,
    currentPage,
    totalPages,
    totalElements,
    loading,
    searched,
    message,
    filters,
    restore,
    search,
    reset,
    movePage,
  }
}
