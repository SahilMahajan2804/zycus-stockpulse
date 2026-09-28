function SuggestionActions({ suggestion, type, onDecision, busyId }) {
  if (!suggestion) return null
  const decide = (status) => onDecision(type, suggestion.id, status)
  return <div className="decision-actions">
    <button type="button" disabled={busyId === suggestion.id} onClick={() => decide('ACCEPTED')}>Accept</button>
    <button type="button" className="secondary" disabled={busyId === suggestion.id} onClick={() => decide('REJECTED')}>Reject</button>
  </div>
}

export default function SuggestionCard({ recommendation, onDecision, busyId }) {
  const prices = recommendation.pricingSuggestions || (recommendation.pricingSuggestion ? [recommendation.pricingSuggestion] : [])
  const reorders = recommendation.reorderSuggestions || (recommendation.reorderSuggestion ? [recommendation.reorderSuggestion] : [])
  const sourceLabels = [...prices, ...reorders].map(item => item.source || 'RULE')

  return <article className="recommendation-card">
    <header className="recommendation-heading">
      <div>
        <h3>{recommendation.productName}</h3>
        <small>{recommendation.productId}</small>
      </div>
      <div className="trigger-group">
        <span className="trigger-badge">{recommendation.triggerReason}</span>
        {[...new Set(sourceLabels)].map(source => <span className="source-badge" key={source}>{source}</span>)}
      </div>
    </header>

    <div className="recommendation-details">
      <div className="recommendation-detail">
        <h4>Pricing</h4>
        {prices.length ? prices.map((price, index) => <div className="suggestion-option" key={price.id}>
          <div className="option-heading">
            <strong>Option {index + 1}</strong>
            <span className="source-badge">{price.source || 'RULE'}</span>
          </div>
          <p className="recommendation-value">₹{Number(price.currentPrice).toFixed(2)} <span>→</span> ₹{Number(price.recommendedPrice).toFixed(2)}</p>
          <div className="meta-row">
            <span>{price.direction}</span>
            <span>{Math.round(Number(price.confidence) * 100)}% confidence</span>
          </div>
          <p className="reasoning">{price.reasoning}</p>
          <SuggestionActions suggestion={price} type="pricing" onDecision={onDecision} busyId={busyId} />
        </div>) : <p className="muted">No pricing suggestion.</p>}
      </div>

      <div className="recommendation-detail">
        <h4>Reorder</h4>
        {reorders.length ? reorders.map((reorder, index) => <div className="suggestion-option" key={reorder.id}>
          <div className="option-heading">
            <strong>Option {index + 1}</strong>
            <span className="source-badge">{reorder.source || 'RULE'}</span>
          </div>
          <p className="recommendation-value">{reorder.currentStock} → {reorder.recommendedQuantity} units</p>
          <div className="meta-row">
            <span>Lead time {reorder.leadTimeDays}d</span>
            <span>{Math.round(Number(reorder.confidence) * 100)}% confidence</span>
          </div>
          <p className="reasoning">{reorder.reasoning}</p>
          <SuggestionActions suggestion={reorder} type="reorder" onDecision={onDecision} busyId={busyId} />
        </div>) : <p className="muted">No reorder suggestion.</p>}
      </div>
    </div>
  </article>
}
