import { useState } from 'react'

const initialForm = {
  id: '',
  sku: '',
  name: '',
  category: 'ELECTRONICS',
  currentPrice: '',
  costPrice: '',
  supplierId: '',
  stockLevel: '',
  reorderThreshold: '',
}

export default function ProductForm({ onSubmit, onCancel, busy }) {
  const [form, setForm] = useState(initialForm)

  function updateField(event) {
    const { name, value } = event.target
    setForm(current => ({ ...current, [name]: value }))
  }

  function submit(event) {
    event.preventDefault()
    onSubmit({
      ...form,
      currentPrice: Number(form.currentPrice),
      costPrice: form.costPrice === '' ? null : Number(form.costPrice),
      stockLevel: Number(form.stockLevel),
      reorderThreshold: Number(form.reorderThreshold),
      supplierId: form.supplierId || null,
    })
  }

  return <section className="section product-form-section">
    <div className="section-heading">
      <div>
        <h2>New product</h2>
        <p>Add a product to inventory manually.</p>
      </div>
      <button type="button" className="secondary" onClick={onCancel}>Cancel</button>
    </div>

    <form className="product-form" onSubmit={submit}>
      <label>Product ID<input name="id" value={form.id} onChange={updateField} required /></label>
      <label>SKU<input name="sku" value={form.sku} onChange={updateField} required /></label>
      <label>Name<input name="name" value={form.name} onChange={updateField} required /></label>
      <label>Category<select name="category" value={form.category} onChange={updateField}>
        <option value="ELECTRONICS">Electronics</option>
        <option value="APPAREL">Apparel</option>
        <option value="HOME">Home</option>
      </select></label>
      <label>Current price<input name="currentPrice" type="number" min="0" step="0.01" value={form.currentPrice} onChange={updateField} required /></label>
      <label>Cost price<input name="costPrice" type="number" min="0" step="0.01" value={form.costPrice} onChange={updateField} /></label>
      <label>Stock level<input name="stockLevel" type="number" min="0" step="1" value={form.stockLevel} onChange={updateField} required /></label>
      <label>Reorder threshold<input name="reorderThreshold" type="number" min="0" step="1" value={form.reorderThreshold} onChange={updateField} required /></label>
      <label>Supplier ID<input name="supplierId" value={form.supplierId} onChange={updateField} /></label>
      <div className="product-form-actions">
        <button type="submit" disabled={busy}>{busy ? 'Creating…' : 'Create product'}</button>
      </div>
    </form>
  </section>
}
