function SuggestionActions({ suggestion, type, onDecision, busyId }) {
  if (!suggestion) return null
  const decide = (status) => onDecision(type, suggestion.id, status)
  return <div className="decision-actions">
    <button disabled={busyId === suggestion.id} onClick={() => decide('ACCEPTED')}>Accept</button>
    <button className="secondary" disabled={busyId === suggestion.id} onClick={() => decide('REJECTED')}>Reject</button>
  </div>
}

export default function SuggestionCard({ recommendation, onDecision, busyId }) {
  const price = recommendation.pricingSuggestion
  const reorder = recommendation.reorderSuggestion
  return <article className="recommendation-card">
    <header className="recommendation-heading">
      <div><h3>{recommendation.productName}</h3><small>{recommendation.productId}</small></div>
      <span className="trigger-badge">{recommendation.triggerReason}</span>
    </header>
    <div className="recommendation-details">
      <div className="recommendation-detail">
        <h4>Pricing</h4>
        {price ? <>
          <p className="recommendation-value">₹{Number(price.currentPrice).toFixed(2)} <span>→</span> ₹{Number(price.recommendedPrice).toFixed(2)}</p>
          <p><b>Confidence:</b> {Math.round(Number(price.confidence) * 100)}% <span className="source">{price.source}</span></p>
          <p className="reasoning">{price.reasoning}</p>
          <SuggestionActions suggestion={price} type="pricing" onDecision={onDecision} busyId={busyId} />
        </> : <p className="muted">No pricing suggestion.</p>}
      </div>
      <div className="recommendation-detail">
        <h4>Reorder</h4>
        {reorder ? <>
          <p className="recommendation-value">{reorder.recommendedQuantity} units</p>
          <p><b>Confidence:</b> {Math.round(Number(reorder.confidence) * 100)}% <span className="source">{reorder.source}</span></p>
          <p className="reasoning">{reorder.reasoning}</p>
          <SuggestionActions suggestion={reorder} type="reorder" onDecision={onDecision} busyId={busyId} />
        </> : <p className="muted">No reorder suggestion.</p>}
      </div>
    </div>
  </article>
}
