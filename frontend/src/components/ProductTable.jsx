function StatusBadge({ status }) {
  return <span className={`status-badge ${String(status).toLowerCase().replaceAll('_', '-')}`}>
    {String(status).replaceAll('_', ' ')}
  </span>
}

export default function ProductTable({ products, category, status, selectedProductId, onFilter, busyId, onSale, onSelect }) {
  return <section className="section">
    <div className="section-heading">
      <div><h2>Products</h2><p>Inventory and demand at a glance.</p></div>
      <div className="filters">
        <label>Category<select value={category} onChange={e => onFilter('category', e.target.value)}>
          <option value="">All</option><option value="ELECTRONICS">ELECTRONICS</option>
          <option value="APPAREL">APPAREL</option><option value="HOME">HOME</option>
        </select></label>
        <label>Status<select value={status} onChange={e => onFilter('status', e.target.value)}>
          <option value="">All</option><option value="ACTIVE">ACTIVE</option>
          <option value="PRICE_REVIEW_PENDING">PRICE_REVIEW_PENDING</option>
          <option value="OUT_OF_STOCK">OUT_OF_STOCK</option>
        </select></label>
      </div>
    </div>
    <div className="table-wrap">
      <table>
        <thead><tr><th>Name</th><th>Category</th><th>Price</th><th>Stock</th><th>Threshold</th><th>Demand</th><th>Status</th><th>Actions</th></tr></thead>
        <tbody>{products.map(product => <tr
          key={product.id}
          className={selectedProductId === product.id ? 'selected-row' : ''}
          onClick={() => onSelect(product.id)}
        >
          <td><strong>{product.name}</strong><small>{product.id}</small></td>
          <td>{product.category}</td>
          <td>₹{Number(product.currentPrice).toFixed(2)}</td>
          <td>{product.stockLevel}</td>
          <td>{product.reorderThreshold}</td>
          <td>{product.demandVelocity}</td>
          <td><StatusBadge status={product.status} /></td>
          <td><div className="actions" onClick={e => e.stopPropagation()}>
            <button type="button" disabled={busyId === product.id || product.stockLevel < 1} onClick={() => onSale(product, 1)}>{busyId === product.id ? 'Selling…' : 'Sale'}</button>
            <button type="button" className="secondary" disabled={busyId === product.id || product.stockLevel < 5} onClick={() => onSale(product, 5)}>Sell 5</button>
          </div></td>
        </tr>)}
        {!products.length && <tr><td colSpan="8" className="empty">No products found.</td></tr>}</tbody>
      </table>
    </div>
  </section>
}
