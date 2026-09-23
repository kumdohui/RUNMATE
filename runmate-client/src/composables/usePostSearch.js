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
  const categories = ref([])
  const posts = ref([])
  const currentPage = ref(0)
  const totalPages = ref(0)
  const totalElements = ref(0)
  const loading = ref(false)
  const searched = ref(false)
  const message = ref('')
  const filters = reactive(initialFilters())

  const createParams = (page) => {
    const params = new URLSearchParams({
      page: String(page),
      sort: filters.sort,
    })
    const distance = distanceRanges[filters.distanceRange]
    if (filters.keyword.trim()) params.set('keyword', filters.keyword.trim())
    if (filters.categoryId) params.set('categoryId', filters.categoryId)
    if (filters.region) params.set('region', filters.region)
    if (filters.tag.trim()) params.set('tag', filters.tag.trim())
    if (distance?.min) params.set('minDistance', distance.min)
    if (distance?.max) params.set('maxDistance', distance.max)
    return params
  }

  const loadCategories = async () => {
    try {
      categories.value = await fetchCategories()
    } catch {
      categories.value = []
    }
  }

  const loadPosts = async (page = 0) => {
    loading.value = true
    message.value = ''
    try {
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

  const search = async () => {
    if (filters.keyword.trim() && filters.keyword.trim().length < 2) {
      message.value = '검색어는 두 글자 이상 입력해 주세요.'
      searched.value = false
      posts.value = []
      return
    }
    searched.value = true
    await loadPosts(0)
  }

  const reset = async () => {
    Object.assign(filters, initialFilters())
    searched.value = true
    await loadPosts(0)
  }

  const movePage = async (page) => {
    if (page < 0 || page >= totalPages.value || page === currentPage.value) return
    await loadPosts(page)
    window.scrollTo({ top: 0, behavior: 'smooth' })
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
    loadCategories,
    search,
    reset,
    movePage,
  }
}
