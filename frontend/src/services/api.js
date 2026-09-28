const API_BASE = '/api'

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      ...(options.body ? { 'Content-Type': 'application/json' } : {}),
      ...options.headers,
    },
  })
  const data = await response.json().catch(() => ({}))
  if (!response.ok) throw new Error(data.message || `Request failed (${response.status})`)
  return data
}

const decide = (path, status) => request(path, {
  method: 'PATCH',
  body: JSON.stringify({ status }),
})

export const getProducts = (category = '', status = '') => {
  const params = new URLSearchParams()
  if (category) params.set('category', category)
  if (status) params.set('status', status)
  const query = params.toString()
  return request(`/products${query ? `?${query}` : ''}`)
}

export const getSummary = () => request('/dashboard/summary')
export const getPendingRecommendations = () => request('/recommendations/pending')
export const getStrategy = () => request('/admin/strategy')
export const switchStrategy = (strategy) => request('/admin/strategy', {
  method: 'PUT',
  body: JSON.stringify({ strategy }),
})
export const placeOrder = (productId, quantity) => request(`/products/${encodeURIComponent(productId)}/orders`, {
  method: 'POST',
  body: JSON.stringify({ quantity }),
})
export const acceptPricing = (id) => decide(`/pricing-suggestions/${id}`, 'ACCEPTED')
export const rejectPricing = (id) => decide(`/pricing-suggestions/${id}`, 'REJECTED')
export const acceptReorder = (id) => decide(`/reorder-suggestions/${id}`, 'ACCEPTED')
export const rejectReorder = (id) => decide(`/reorder-suggestions/${id}`, 'REJECTED')
