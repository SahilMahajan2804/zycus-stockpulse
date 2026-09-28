import { useCallback, useEffect, useState } from 'react'
import ProductTable from './components/ProductTable'
import SuggestionCard from './components/SuggestionCard'
import SummaryCards from './components/SummaryCards'
import {
  acceptPricing,
  acceptReorder,
  getPendingRecommendations,
  getProducts,
  getStrategy,
  getSummary,
  placeOrder,
  rejectPricing,
  rejectReorder,
  switchStrategy,
} from './services/api'

export default function App() {
  const [products, setProducts] = useState([])
  const [summary, setSummary] = useState(null)
  const [recommendations, setRecommendations] = useState([])
  const [strategy, setStrategy] = useState({ pricing: 'AI', reorder: 'RULE' })
  const [category, setCategory] = useState('')
  const [status, setStatus] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [busyId, setBusyId] = useState('')

  const refreshAll = useCallback(async () => {
    try {
      const [productData, summaryData, pendingData] = await Promise.all([
        getProducts(category, status),
        getSummary(),
        getPendingRecommendations(),
      ])
      setProducts(productData.items || [])
      setSummary(summaryData)
      setRecommendations(pendingData.items || [])
      setError('')
    } catch (err) {
      setError(err.message || 'Failed to load StockPulse data')
    } finally {
      setLoading(false)
    }
  }, [category, status])

  const refreshPending = useCallback(async () => {
    try {
      const data = await getPendingRecommendations()
      setRecommendations(data.items || [])
    } catch (err) {
      setError(err.message || 'Failed to load recommendations')
    }
  }, [])

  useEffect(() => {
    setLoading(true)
    refreshAll()
    getStrategy().then(setStrategy).catch(err => setError(err.message || 'Failed to load strategy'))
  }, [refreshAll])

  useEffect(() => {
    const interval = window.setInterval(refreshPending, 3000)
    return () => window.clearInterval(interval)
  }, [refreshPending])

  function updateFilter(name, value) {
    if (name === 'category') setCategory(value)
    else setStatus(value)
  }

  async function handleSale(product, quantity) {
    setBusyId(product.id)
    setError('')
    try {
      await placeOrder(product.id, quantity)
      await refreshAll()
    } catch (err) {
      setError(err.message || 'Failed to place order')
    } finally {
      setBusyId('')
    }
  }

  async function handleDecision(type, id, decision) {
    setBusyId(id)
    setError('')
    try {
      if (type === 'pricing') {
        if (decision === 'ACCEPTED') await acceptPricing(id)
        else await rejectPricing(id)
      } else if (decision === 'ACCEPTED') await acceptReorder(id)
      else await rejectReorder(id)
      await refreshAll()
    } catch (err) {
      setError(err.message || 'Failed to update recommendation')
    } finally {
      setBusyId('')
    }
  }

  async function handleStrategyChange(event) {
    const selected = event.target.value
    setError('')
    try {
      const current = await switchStrategy(selected)
      setStrategy(current)
    } catch (err) {
      setError(err.message || 'Failed to change strategy')
      try { setStrategy(await getStrategy()) } catch { /* keep last value */ }
    }
  }

  if (loading) return <main className="page"><p>Loading...</p></main>

  return <main className="page">
    <header className="page-header">
      <div><h1>STOCKPULSE</h1><p>Inventory recommendations for human review.</p></div>
    </header>
    {error && <p className="error" role="alert">{error}</p>}

    <SummaryCards summary={summary} />

    <ProductTable
      products={products}
      category={category}
      status={status}
      onFilter={updateFilter}
      busyId={busyId}
      onSale={handleSale}
    />

    <section className="section">
      <div className="section-heading"><div><h2>Pending Recommendations</h2><p>Review before applying any suggested changes.</p></div></div>
      {recommendations.length ? <div className="recommendation-list">
        {recommendations.map(item => <SuggestionCard key={`${item.productId}-${item.triggerReason}`} recommendation={item} onDecision={handleDecision} busyId={busyId} />)}
      </div> : <p className="empty-message">No pending recommendations.</p>}
    </section>

    <section className="section strategy-section">
      <div><h2>Active Strategy</h2><p>Pricing: <strong>{strategy.pricing}</strong> · Reorder: <strong>{strategy.reorder}</strong></p></div>
      <label>Change strategy<select value={strategy.pricing === 'RULE' ? 'RULE' : 'AI'} onChange={handleStrategyChange}>
        <option value="AI">AI</option><option value="RULE">RULE</option>
      </select></label>
    </section>
  </main>
}
