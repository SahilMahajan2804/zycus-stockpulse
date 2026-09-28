import { useCallback, useEffect, useMemo, useState } from 'react'
import ProductTable from './components/ProductTable'
import ProductForm from './components/ProductForm'
import SuggestionCard from './components/SuggestionCard'
import SummaryCards from './components/SummaryCards'
import {
  acceptPricing,
  acceptReorder,
  createProduct,
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
  const [selectedProductId, setSelectedProductId] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [busyId, setBusyId] = useState('')
  const [showProductForm, setShowProductForm] = useState(false)

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

  useEffect(() => {
    if (!products.length) {
      setSelectedProductId('')
      return
    }
    if (!selectedProductId || !products.some(product => product.id === selectedProductId)) {
      setSelectedProductId(products[0].id)
    }
  }, [products, selectedProductId])

  const selectedProduct = useMemo(
    () => products.find(product => product.id === selectedProductId) || products[0] || null,
    [products, selectedProductId],
  )

  const selectedRecommendations = useMemo(
    () => recommendations.filter(item => item.productId === selectedProduct?.id),
    [recommendations, selectedProduct],
  )

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

  async function handleCreateProduct(product) {
    setBusyId('create-product')
    setError('')
    try {
      const created = await createProduct(product)
      setShowProductForm(false)
      await refreshAll()
      setSelectedProductId(created.id)
    } catch (err) {
      setError(err.message || 'Failed to create product')
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

  if (loading) return <main className="page-shell"><div className="loading-panel">Loading StockPulse…</div></main>

  return <main className="page-shell">
    <aside className="sidebar">
      <div className="brand-block">
        <p className="eyebrow">OPERATIONS</p>
        <h1>StockPulse</h1>
      </div>
      <nav className="nav">
        <button className="nav-item active" type="button">Dashboard</button>
        <button className="nav-item" type="button">Products</button>
        <button className="nav-item" type="button">Recommendations</button>
      </nav>
    </aside>

    <div className="content-panel">
      <header className="page-header">
        <div>
          <p className="eyebrow">MERCHANDISING WORKSPACE</p>
          <h2>Inventory recommendations</h2>
        </div>
        <div className="topbar-actions">
          <button type="button" onClick={() => setShowProductForm(current => !current)}>
            {showProductForm ? 'Close form' : 'Add product'}
          </button>
          <button type="button" className="ghost-button" onClick={refreshAll}>Refresh</button>
          <label className="strategy-picker">
            Strategy
            <select value={strategy.pricing === 'RULE' ? 'RULE' : 'AI'} onChange={handleStrategyChange}>
              <option value="AI">AI</option>
              <option value="RULE">RULE</option>
            </select>
          </label>
        </div>
      </header>

      {error && <p className="error" role="alert">{error}</p>}

      <SummaryCards summary={summary} />

      {showProductForm && <ProductForm
        onSubmit={handleCreateProduct}
        onCancel={() => setShowProductForm(false)}
        busy={busyId === 'create-product'}
      />}

      <ProductTable
        products={products}
        category={category}
        status={status}
        selectedProductId={selectedProductId}
        onFilter={updateFilter}
        busyId={busyId}
        onSale={handleSale}
        onSelect={setSelectedProductId}
      />

      {selectedProduct && <section className="section detail-panel">
        <div className="detail-header">
          <div>
            <p className="eyebrow">SELECTED PRODUCT</p>
            <h3>{selectedProduct.name}</h3>
            <small>{selectedProduct.id} · {selectedProduct.category}</small>
          </div>
          <span className={`status-badge ${String(selectedProduct.status).toLowerCase().replaceAll('_', '-')}`}>
            {selectedProduct.status}
          </span>
        </div>

        <div className="detail-stats">
          <div><span>Current Price</span><strong>₹{Number(selectedProduct.currentPrice).toFixed(2)}</strong></div>
          <div><span>Stock</span><strong>{selectedProduct.stockLevel}</strong></div>
          <div><span>Threshold</span><strong>{selectedProduct.reorderThreshold}</strong></div>
          <div><span>Demand Velocity</span><strong>{selectedProduct.demandVelocity}</strong></div>
        </div>

        <div className="recommendation-stack">
          {selectedRecommendations.length ? selectedRecommendations.map(item => (
            <SuggestionCard key={`${item.productId}-${item.triggerReason}`} recommendation={item} onDecision={handleDecision} busyId={busyId} />
          )) : <div className="empty-message-card">No pending recommendations for this product.</div>}
        </div>
      </section>}

      <section className="section">
        <div className="section-heading">
          <div>
            <h2>Pending Recommendations</h2>
            <p>Review each recommendation before applying a merchandising decision.</p>
          </div>
        </div>

        {recommendations.length ? (
          <div className="recommendation-list">
            {recommendations.map(item => (
              <SuggestionCard key={`${item.productId}-${item.triggerReason}`} recommendation={item} onDecision={handleDecision} busyId={busyId} />
            ))}
          </div>
        ) : (
          <p className="empty-message">No pending recommendations.</p>
        )}
      </section>
    </div>
  </main>
}
