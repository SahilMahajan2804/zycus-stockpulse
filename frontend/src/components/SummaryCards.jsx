const metrics = [
  ['totalProducts', 'Total Products'],
  ['lowStockProducts', 'Low Stock Products'],
  ['pendingReviews', 'Pending Reviews'],
  ['demandSpikeProducts', 'Demand Spikes'],
]

export default function SummaryCards({ summary }) {
  return <section className="summary-grid">
    {metrics.map(([key, label]) => <article className="summary-card" key={key}>
      <span>{label}</span><strong>{summary?.[key] ?? '—'}</strong>
    </article>)}
  </section>
}
