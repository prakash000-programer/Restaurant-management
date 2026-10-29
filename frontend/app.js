/* ===================================================================
   Grand Bistro — Restaurant Management System
   app.js  |  In-memory SPA logic
   Admin password: admin123
=================================================================== */

'use strict';

// ─────────────────────────────────────────────────────────────────
// 1. IN-MEMORY DATA STORE
// ─────────────────────────────────────────────────────────────────

let tableIdCounter     = 1;
let menuItemIdCounter  = 1;
let orderIdCounter     = 1;
let orderItemIdCounter = 1;
let billIdCounter      = 1;
let paymentIdCounter   = 1;

/** @type {Array<{id:number, capacity:number, status:'FREE'|'OCCUPIED'}>} */
let tables = [];

/** @type {Array<{id:number, name:string, price:number, category:string, available:boolean}>} */
let menuItems = [];

/** @type {Array<{id:number, tableId:number, status:string, createdAt:Date, items:Array}>} */
let orders = [];

/** @type {Array<{id:number, orderId:number, tableId:number, subTotal:number, discountPercentage:number, discountAmount:number, totalAmount:number, status:string, createdAt:Date}>} */
let bills = [];

/** @type {Array<{id:number, billId:number, orderId:number, tableId:number, amountDue:number, amountPaid:number, method:string, status:string, paidAt:Date}>} */
let payments = [];

/** Active order being built (Place Order section). */
let currentOrder = {
  tableId: null,
  items: []   // {menuItemId, name, price, quantity}
};

// Temp variables for quantity & payment modals
let _pendingMenuItem   = null;   // item being added to cart
let _pendingQty        = 1;
let _updatingCartIndex = -1;     // cart item index being updated
let _updatingCartQty   = 1;
let _selectedPaymentMethod = 'CASH';
let _activePaymentOrderId  = null;
let _activeReceiptPaymentId = null;
let _currentPaymentFilter  = 'ALL';

// Admin action queue
let _adminAction = null;  // function to call after admin verification

const ADMIN_PASSWORD = 'admin123';

// ─────────────────────────────────────────────────────────────────
// 2. SEED DATA
// ─────────────────────────────────────────────────────────────────

function seedData() {
  // Tables
  addTableData(2);
  addTableData(4);
  addTableData(6);
  addTableData(8);
  tables[1].status = 'OCCUPIED';   // Table 2 starts occupied

  // Menu items
  addMenuItemData('Spring Rolls',     45.00, 'STARTER');
  addMenuItemData('Chicken Soup',     60.00, 'STARTER');
  addMenuItemData('Grilled Chicken', 180.00, 'MAIN_COURSE');
  addMenuItemData('Pasta Alfredo',   150.00, 'MAIN_COURSE');
  addMenuItemData('Veg Burger',      120.00, 'MAIN_COURSE');
  addMenuItemData('Choc Mousse',      80.00, 'DESSERT');
  addMenuItemData('Ice Cream',        50.00, 'DESSERT');
  addMenuItemData('Lime Soda',        40.00, 'BEVERAGE');
  addMenuItemData('Coffee',           35.00, 'BEVERAGE');

  // A few sample past orders
  const o1 = createOrderData(1);
  o1.items.push({ menuItemId: 1, name: 'Spring Rolls',   price: 45.00, quantity: 2 });
  o1.items.push({ menuItemId: 3, name: 'Grilled Chicken', price: 180.00, quantity: 1 });
  o1.status = 'COMPLETED';
  o1.createdAt = new Date(Date.now() - 3600000 * 2);

  const o2 = createOrderData(2);
  o2.items.push({ menuItemId: 5, name: 'Veg Burger', price: 120.00, quantity: 3 });
  o2.items.push({ menuItemId: 9, name: 'Coffee',     price:  35.00, quantity: 2 });
  o2.status = 'PREPARING';
  o2.createdAt = new Date(Date.now() - 3600000);

  // Seed sample completed bill & payment for order 1
  const b1 = addBillData(o1.id, o1.tableId, getOrderTotal(o1), 5.0, o1.createdAt);
  b1.status = 'PAID';
  addPaymentData(b1, b1.totalAmount, 'CREDIT_CARD', 'COMPLETED', o1.createdAt);
}

// ─────────────────────────────────────────────────────────────────
// 3. DATA HELPERS
// ─────────────────────────────────────────────────────────────────

function addTableData(capacity) {
  const t = { id: tableIdCounter++, capacity, status: 'FREE' };
  tables.push(t);
  return t;
}

function addMenuItemData(name, price, category) {
  const m = { id: menuItemIdCounter++, name, price, category, available: true };
  menuItems.push(m);
  return m;
}

function createOrderData(tableId) {
  const o = {
    id: orderIdCounter++,
    tableId,
    status: 'PENDING',
    createdAt: new Date(),
    items: []
  };
  orders.push(o);
  return o;
}

function addBillData(orderId, tableId, subTotal, discountPercentage = 0, createdAt = new Date()) {
  const discountAmount = (subTotal * discountPercentage) / 100;
  const totalAmount = Math.max(0, subTotal - discountAmount);
  const b = {
    id: billIdCounter++,
    orderId,
    tableId,
    subTotal,
    discountPercentage,
    discountAmount,
    totalAmount,
    status: 'UNPAID',
    createdAt: new Date(createdAt)
  };
  bills.push(b);
  return b;
}

function addPaymentData(billOrOrderId, tableIdOrAmountPaid, amountDueOrMethod, amountPaidOrStatus, methodOrPaidAt, status = 'COMPLETED', paidAt = new Date()) {
  let billId = null;
  let orderId;
  let tableId;
  let amountDue;
  let amountPaid;
  let method;
  let pStatus = status;
  let pPaidAt = paidAt;

  if (typeof billOrOrderId === 'object' && billOrOrderId !== null) {
    const bill = billOrOrderId;
    billId = bill.id;
    orderId = bill.orderId;
    tableId = bill.tableId;
    amountDue = bill.totalAmount;
    amountPaid = tableIdOrAmountPaid;
    method = amountDueOrMethod;
    if (amountPaidOrStatus) pStatus = amountPaidOrStatus;
    if (methodOrPaidAt) pPaidAt = methodOrPaidAt;
  } else {
    orderId = billOrOrderId;
    tableId = tableIdOrAmountPaid;
    amountDue = amountDueOrMethod;
    amountPaid = amountPaidOrStatus;
    method = methodOrPaidAt;
  }

  const p = {
    id: paymentIdCounter++,
    billId,
    orderId,
    tableId,
    amountDue,
    amountPaid,
    method,
    status: pStatus,
    paidAt: new Date(pPaidAt)
  };
  payments.push(p);
  return p;
}

function getOrderTotal(order) {
  return order.items.reduce((sum, i) => sum + i.price * i.quantity, 0);
}

function getCartTotal() {
  return currentOrder.items.reduce((sum, i) => sum + i.price * i.quantity, 0);
}

// ─────────────────────────────────────────────────────────────────
// 4. NAVIGATION
// ─────────────────────────────────────────────────────────────────

const sectionMeta = {
  tables:   { name: 'Tables',            sub: 'Overview' },
  menu:     { name: 'Menu',              sub: 'All Items' },
  order:    { name: 'Place Order',       sub: 'New Order' },
  history:  { name: 'Order History',     sub: 'Top 10' },
  payments: { name: 'Payment & Billing', sub: 'Revenue & Invoicing' }
};

function switchSection(key) {
  document.querySelectorAll('.section').forEach(s => s.classList.remove('active'));
  document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));

  document.getElementById('section-' + key).classList.add('active');
  document.getElementById('nav-' + key).classList.add('active');

  const meta = sectionMeta[key];
  document.getElementById('header-section-name').textContent = meta.name;
  document.getElementById('header-section-sub').textContent  = meta.sub;

  // Refresh content on section switch
  if (key === 'tables')   renderTables();
  if (key === 'menu')     renderMenu('ALL');
  if (key === 'order')    renderOrderSection();
  if (key === 'history')  renderHistory();
  if (key === 'payments') renderPaymentsSection();
}

// ─────────────────────────────────────────────────────────────────
// 5. TABLE MANAGEMENT
// ─────────────────────────────────────────────────────────────────

function renderTables() {
  const grid  = document.getElementById('tables-grid');
  const stats = document.getElementById('table-stats');

  const free     = tables.filter(t => t.status === 'FREE').length;
  const occupied = tables.filter(t => t.status === 'OCCUPIED').length;

  stats.innerHTML = `
    <div class="stat-card amber">
      <div class="stat-value">${tables.length}</div>
      <div class="stat-label">Total Tables</div>
    </div>
    <div class="stat-card green">
      <div class="stat-value">${free}</div>
      <div class="stat-label">Free</div>
    </div>
    <div class="stat-card red">
      <div class="stat-value">${occupied}</div>
      <div class="stat-label">Occupied</div>
    </div>
  `;

  if (tables.length === 0) {
    grid.innerHTML = '<p style="color:var(--text-muted);padding:20px">No tables yet. Add one!</p>';
    return;
  }

  grid.innerHTML = tables.map(t => {
    const isFree = t.status === 'FREE';
    const activeOrder = orders.find(o => o.tableId === t.id && o.status !== 'COMPLETED' && o.status !== 'CANCELLED' && !payments.some(p => p.orderId === o.id && p.status === 'COMPLETED'));
    return `
      <div class="table-card ${isFree ? 'free' : 'occupied'}">
        <div class="table-card-header">
          <div class="table-card-num">T${String(t.id).padStart(2, '0')}</div>
          <span class="table-badge ${isFree ? 'badge-free' : 'badge-occupied'}">${t.status}</span>
        </div>
        <div class="table-capacity">${t.capacity} seats</div>
        ${activeOrder ? `<div style="font-size:0.78rem;color:var(--accent);font-weight:600;margin:6px 0;">Order #${activeOrder.id} &bull; $${getOrderTotal(activeOrder).toFixed(2)}</div>` : ''}
        <div class="table-card-actions">
          <button class="btn-icon" title="Edit" onclick="openEditTable(${t.id})">&#9998;</button>
          <button class="btn-icon danger" title="Delete" onclick="deleteTable(${t.id})">&#128465;</button>
          ${activeOrder ? `<button class="btn btn-sm btn-primary" style="padding:4px 8px;font-size:0.75rem;" onclick="openPaymentModal(${activeOrder.id})">Settle Bill</button>` : ''}
          ${isFree
            ? `<button class="btn btn-sm btn-ghost" onclick="toggleTableStatus(${t.id})">Mark Occupied</button>`
            : `<button class="btn btn-sm btn-ghost" onclick="toggleTableStatus(${t.id})">Mark Free</button>`
          }
        </div>
      </div>
    `;
  }).join('');
}

function addTable() {
  const cap = parseInt(document.getElementById('add-table-capacity').value);
  if (!cap || cap < 1) { showToast('Enter a valid capacity.', 'error'); return; }
  addTableData(cap);
  closeModal('modal-add-table');
  document.getElementById('add-table-capacity').value = '';
  renderTables();
  showToast(`Table added (${cap} seats).`, 'success');
}

function openEditTable(id) {
  const t = tables.find(x => x.id === id);
  if (!t) return;
  document.getElementById('edit-table-id').value       = t.id;
  document.getElementById('edit-table-capacity').value = t.capacity;
  document.getElementById('edit-table-status').value   = t.status;
  openModal('modal-edit-table');
}

function updateTable() {
  const id  = parseInt(document.getElementById('edit-table-id').value);
  const cap = parseInt(document.getElementById('edit-table-capacity').value);
  const st  = document.getElementById('edit-table-status').value;
  if (!cap || cap < 1) { showToast('Enter a valid capacity.', 'error'); return; }
  const t = tables.find(x => x.id === id);
  if (!t) return;
  t.capacity = cap;
  t.status   = st;
  closeModal('modal-edit-table');
  renderTables();
  showToast('Table updated.', 'success');
}

function deleteTable(id) {
  if (!confirm('Delete this table?')) return;
  tables = tables.filter(t => t.id !== id);
  renderTables();
  showToast('Table deleted.', 'info');
}

function toggleTableStatus(id) {
  const t = tables.find(x => x.id === id);
  if (!t) return;
  t.status = t.status === 'FREE' ? 'OCCUPIED' : 'FREE';
  renderTables();
  showToast(`Table #${id} is now ${t.status}.`, 'info');
}

// ─────────────────────────────────────────────────────────────────
// 6. MENU MANAGEMENT
// ─────────────────────────────────────────────────────────────────

let _menuFilter = 'ALL';

const catEmoji = {
  STARTER: '&#127825;',
  MAIN_COURSE: '&#127859;',
  DESSERT: '&#127856;',
  BEVERAGE: '&#129380;'
};

const catLabel = {
  STARTER: 'Starter',
  MAIN_COURSE: 'Main Course',
  DESSERT: 'Dessert',
  BEVERAGE: 'Beverage'
};

function filterMenu(cat, btn) {
  _menuFilter = cat;
  document.querySelectorAll('#menu-filter-bar .filter-btn').forEach(b => b.classList.remove('active'));
  btn.classList.add('active');
  renderMenu(cat);
}

function renderMenu(cat) {
  const grid = document.getElementById('menu-grid');
  const items = cat === 'ALL' ? menuItems : menuItems.filter(m => m.category === cat);

  if (items.length === 0) {
    grid.innerHTML = '<p style="color:var(--text-muted);padding:20px">No items in this category.</p>';
    return;
  }

  grid.innerHTML = items.map(m => `
    <div class="menu-card">
      <div class="menu-card-cat cat-${m.category}">${catEmoji[m.category]} ${catLabel[m.category]}</div>
      <div class="menu-card-name">${escHtml(m.name)}</div>
      <div class="menu-card-price">$${m.price.toFixed(2)}</div>
      <span class="menu-card-avail ${m.available ? 'avail-yes' : 'avail-no'}">${m.available ? 'Available' : 'Unavailable'}</span>
      <div class="menu-card-actions">
        <button class="btn-icon" title="Edit" onclick="openEditMenuItem(${m.id})">&#9998;</button>
        <button class="btn-icon danger" title="Delete" onclick="deleteMenuItem(${m.id})">&#128465;</button>
        <button class="btn btn-sm btn-ghost" onclick="toggleMenuAvail(${m.id})">
          ${m.available ? 'Disable' : 'Enable'}
        </button>
      </div>
    </div>
  `).join('');
}

function addMenuItem() {
  const name  = document.getElementById('add-menu-name').value.trim();
  const price = parseFloat(document.getElementById('add-menu-price').value);
  const cat   = document.getElementById('add-menu-category').value;
  if (!name)         { showToast('Name is required.',         'error'); return; }
  if (isNaN(price) || price < 0) { showToast('Enter a valid price.', 'error'); return; }
  addMenuItemData(name, price, cat);
  closeModal('modal-add-menu');
  document.getElementById('add-menu-name').value  = '';
  document.getElementById('add-menu-price').value = '';
  renderMenu(_menuFilter);
  showToast(`"${name}" added to menu.`, 'success');
}

function openEditMenuItem(id) {
  const m = menuItems.find(x => x.id === id);
  if (!m) return;
  document.getElementById('edit-menu-id').value        = m.id;
  document.getElementById('edit-menu-name').value      = m.name;
  document.getElementById('edit-menu-price').value     = m.price;
  document.getElementById('edit-menu-category').value  = m.category;
  document.getElementById('edit-menu-available').value = String(m.available);
  openModal('modal-edit-menu');
}

function updateMenuItem() {
  const id   = parseInt(document.getElementById('edit-menu-id').value);
  const name = document.getElementById('edit-menu-name').value.trim();
  const price = parseFloat(document.getElementById('edit-menu-price').value);
  const cat  = document.getElementById('edit-menu-category').value;
  const avail = document.getElementById('edit-menu-available').value === 'true';
  if (!name)         { showToast('Name is required.',   'error'); return; }
  if (isNaN(price) || price < 0) { showToast('Enter a valid price.', 'error'); return; }
  const m = menuItems.find(x => x.id === id);
  if (!m) return;
  m.name = name; m.price = price; m.category = cat; m.available = avail;
  closeModal('modal-edit-menu');
  renderMenu(_menuFilter);
  showToast('Menu item updated.', 'success');
}

function deleteMenuItem(id) {
  if (!confirm('Delete this menu item?')) return;
  menuItems = menuItems.filter(m => m.id !== id);
  renderMenu(_menuFilter);
  showToast('Menu item deleted.', 'info');
}

function toggleMenuAvail(id) {
  const m = menuItems.find(x => x.id === id);
  if (!m) return;
  m.available = !m.available;
  renderMenu(_menuFilter);
  showToast(`"${m.name}" marked as ${m.available ? 'Available' : 'Unavailable'}.`, 'info');
}

// ─────────────────────────────────────────────────────────────────
// 7. PLACE ORDER
// ─────────────────────────────────────────────────────────────────

let _orderMenuFilter = 'ALL';

function renderOrderSection() {
  // Populate table selector (free tables only)
  const sel = document.getElementById('order-table-select');
  const prevTable = sel.value;
  sel.innerHTML = '<option value="">-- Select Table --</option>' +
    tables.map(t => `<option value="${t.id}" ${t.id == prevTable ? 'selected' : ''}>${'T' + String(t.id).padStart(2,'0')} — ${t.capacity} seats (${t.status})</option>`).join('');

  renderOrderMenu(_orderMenuFilter);
  renderCart();
}

function setOrderTable(val) {
  currentOrder.tableId = val ? parseInt(val) : null;
  renderCart();
}

function filterOrderMenu(cat, btn) {
  _orderMenuFilter = cat;
  document.querySelectorAll('#section-order .filter-bar.small .filter-btn').forEach(b => b.classList.remove('active'));
  btn.classList.add('active');
  renderOrderMenu(cat);
}

function renderOrderMenu(cat) {
  const list  = document.getElementById('order-menu-list');
  const items = cat === 'ALL' ? menuItems : menuItems.filter(m => m.category === cat);

  if (items.length === 0) {
    list.innerHTML = '<p style="color:var(--text-muted);padding:16px">No items in this category.</p>';
    return;
  }

  list.innerHTML = items.map(m => `
    <div class="order-menu-item ${m.available ? '' : 'unavailable'}" title="${m.available ? '' : 'Currently unavailable'}">
      <div class="order-menu-item-info">
        <div class="order-menu-item-name">${escHtml(m.name)}</div>
        <div class="order-menu-item-meta">${catLabel[m.category]}${m.available ? '' : ' &nbsp;&#10005; Unavailable'}</div>
      </div>
      <div class="order-menu-item-price">$${m.price.toFixed(2)}</div>
      <button class="btn btn-sm btn-primary" onclick="openAddToCart(${m.id})">+ Add</button>
    </div>
  `).join('');
}

function openAddToCart(menuItemId) {
  if (!currentOrder.tableId) {
    showToast('Please select a table first.', 'error');
    return;
  }
  const m = menuItems.find(x => x.id === menuItemId);
  if (!m || !m.available) { showToast('Item unavailable.', 'error'); return; }
  _pendingMenuItem = m;
  _pendingQty      = 1;
  document.getElementById('modal-qty-item-name').textContent  = m.name;
  document.getElementById('modal-qty-item-price').textContent = '$' + m.price.toFixed(2) + ' each';
  document.getElementById('qty-display').textContent = '1';
  openModal('modal-qty');
}

function changeQty(delta) {
  _pendingQty = Math.max(1, _pendingQty + delta);
  document.getElementById('qty-display').textContent = _pendingQty;
}

function confirmAddToCart() {
  if (!_pendingMenuItem) return;
  const existing = currentOrder.items.find(i => i.menuItemId === _pendingMenuItem.id);
  if (existing) {
    existing.quantity += _pendingQty;
  } else {
    currentOrder.items.push({
      menuItemId: _pendingMenuItem.id,
      name:       _pendingMenuItem.name,
      price:      _pendingMenuItem.price,
      quantity:   _pendingQty
    });
  }
  closeModal('modal-qty');
  renderCart();
  showToast(`${_pendingMenuItem.name} x${_pendingQty} added.`, 'success');
}

function renderCart() {
  const empty  = document.getElementById('cart-empty');
  const items  = document.getElementById('cart-items');
  const footer = document.getElementById('cart-footer');
  const cancelBtn = document.getElementById('cancel-order-btn');

  if (currentOrder.items.length === 0) {
    empty.style.display  = 'block';
    items.innerHTML      = '';
    footer.style.display = 'none';
    cancelBtn.style.display = 'none';
    return;
  }

  empty.style.display     = 'none';
  footer.style.display    = 'block';
  cancelBtn.style.display = 'inline-flex';

  items.innerHTML = currentOrder.items.map((item, idx) => `
    <div class="cart-item">
      <div class="cart-item-info">
        <div class="cart-item-name">${escHtml(item.name)}</div>
        <div class="cart-item-sub">$${item.price.toFixed(2)} &times; ${item.quantity}</div>
      </div>
      <div class="cart-item-total">$${(item.price * item.quantity).toFixed(2)}</div>
      <div class="cart-item-actions">
        <button class="btn-icon" title="Update qty" onclick="openUpdateCart(${idx})">&#9998;</button>
        <button class="btn-icon danger" title="Remove" onclick="removeCartItem(${idx})">&#10005;</button>
      </div>
    </div>
  `).join('');

  document.getElementById('cart-total').textContent = '$' + getCartTotal().toFixed(2);
}

function openUpdateCart(idx) {
  const item = currentOrder.items[idx];
  if (!item) return;
  _updatingCartIndex = idx;
  _updatingCartQty   = item.quantity;
  document.getElementById('update-cart-item-name').textContent = item.name;
  document.getElementById('update-qty-display').textContent    = item.quantity;
  openModal('modal-update-cart');
}

function changeUpdateQty(delta) {
  _updatingCartQty = Math.max(1, _updatingCartQty + delta);
  document.getElementById('update-qty-display').textContent = _updatingCartQty;
}

function confirmUpdateCart() {
  if (_updatingCartIndex < 0) return;
  currentOrder.items[_updatingCartIndex].quantity = _updatingCartQty;
  closeModal('modal-update-cart');
  renderCart();
  showToast('Quantity updated.', 'success');
}

function removeCartItem(idx) {
  const name = currentOrder.items[idx].name;
  currentOrder.items.splice(idx, 1);
  renderCart();
  showToast(`${name} removed.`, 'info');
}

function cancelOrder() {
  if (!confirm('Cancel the current order and clear all items?')) return;
  currentOrder.items   = [];
  currentOrder.tableId = null;
  document.getElementById('order-table-select').value = '';
  renderCart();
  showToast('Order cancelled.', 'info');
}

function placeOrder() {
  if (!currentOrder.tableId) { showToast('Select a table first.', 'error'); return; }
  if (currentOrder.items.length === 0) { showToast('Add at least one item.', 'error'); return; }

  const order = createOrderData(currentOrder.tableId);
  order.items = currentOrder.items.map(i => ({ ...i }));  // snapshot

  // Mark table as occupied
  const tbl = tables.find(t => t.id === currentOrder.tableId);
  if (tbl) tbl.status = 'OCCUPIED';

  // Reset cart
  currentOrder.items   = [];
  currentOrder.tableId = null;
  document.getElementById('order-table-select').value = '';
  renderCart();
  renderOrderSection();

  showToast(`Order #${order.id} placed! Total: $${getOrderTotal(order).toFixed(2)}`, 'success');
}

// ─────────────────────────────────────────────────────────────────
// 8. ORDER HISTORY
// ─────────────────────────────────────────────────────────────────

function renderHistory() {
  const list = document.getElementById('history-list');

  // Top 10 sorted by createdAt descending
  const top10 = [...orders]
    .sort((a, b) => b.createdAt - a.createdAt)
    .slice(0, 10);

  if (top10.length === 0) {
    list.innerHTML = '<p style="color:var(--text-muted);padding:20px">No orders yet.</p>';
    return;
  }

  list.innerHTML = top10.map(o => {
    const total   = getOrderTotal(o);
    const dateStr = formatDate(o.createdAt);
    const pay     = payments.find(p => p.orderId === o.id);
    let payHtml   = '';
    if (pay && pay.status === 'COMPLETED') {
      payHtml = `<span class="badge-method ${pay.method.toLowerCase()}">&#10003; Paid (${pay.method})</span>
                 <button class="btn btn-sm btn-ghost" style="padding:2px 8px;font-size:0.75rem;" title="View Receipt" onclick="event.stopPropagation(); openReceiptModal(${pay.id})">&#128196; Receipt</button>`;
    } else if (pay && pay.status === 'REFUNDED') {
      payHtml = `<span class="badge-status-refunded">REFUNDED</span>`;
    } else if (o.status !== 'CANCELLED') {
      payHtml = `<button class="btn btn-sm btn-primary" style="padding:2px 8px;font-size:0.75rem;" onclick="event.stopPropagation(); openPaymentModal(${o.id})">&#128179; Settle Bill</button>`;
    }

    return `
      <div class="history-card" id="hist-card-${o.id}">
        <div class="history-card-header" onclick="toggleHistoryCard(${o.id})">
          <div class="history-order-num">Order #${o.id}</div>
          <div class="history-card-meta">
            <span>Table: <strong>T${String(o.tableId).padStart(2,'0')}</strong></span>
            <span>Items: <strong>${o.items.length}</strong></span>
            <span>Total: <strong>$${total.toFixed(2)}</strong></span>
            <span>${dateStr}</span>
            <span style="display:flex;align-items:center;gap:6px;">${payHtml}</span>
          </div>
          <span class="history-status status-${o.status}">${o.status}</span>
          <div class="history-card-actions">
            <button class="btn-icon" title="Edit (Admin)" onclick="event.stopPropagation(); requireAdmin(() => openEditOrder(${o.id}))">&#9998;</button>
            <button class="btn-icon danger" title="Delete (Admin)" onclick="event.stopPropagation(); requireAdmin(() => deleteOrder(${o.id}))">&#128465;</button>
          </div>
        </div>
        <div class="history-card-body" id="hist-body-${o.id}">
          <table class="history-items-table">
            <thead>
              <tr><th>Item</th><th>Qty</th><th>Unit</th><th>Subtotal</th></tr>
            </thead>
            <tbody>
              ${o.items.map(i => `
                <tr>
                  <td>${escHtml(i.name)}</td>
                  <td>${i.quantity}</td>
                  <td>$${i.price.toFixed(2)}</td>
                  <td>$${(i.price * i.quantity).toFixed(2)}</td>
                </tr>
              `).join('')}
            </tbody>
          </table>
          <div class="history-total">
            <span>Grand Total</span>
            <span>$${total.toFixed(2)}</span>
          </div>
        </div>
      </div>
    `;
  }).join('');
}

function toggleHistoryCard(id) {
  const body = document.getElementById('hist-body-' + id);
  if (body) body.classList.toggle('open');
}

// ─── Admin-protected actions ────────────────────────────────────

function requireAdmin(action) {
  _adminAction = action;
  document.getElementById('admin-password-input').value = '';
  document.getElementById('admin-error').style.display  = 'none';
  openModal('modal-admin');
  setTimeout(() => document.getElementById('admin-password-input').focus(), 200);
}

function verifyAdmin() {
  const pw = document.getElementById('admin-password-input').value;
  if (pw === ADMIN_PASSWORD) {
    closeModal('modal-admin');
    if (_adminAction) { _adminAction(); _adminAction = null; }
  } else {
    document.getElementById('admin-error').style.display = 'block';
    document.getElementById('admin-password-input').value = '';
    document.getElementById('admin-password-input').focus();
  }
}

function openEditOrder(id) {
  const o = orders.find(x => x.id === id);
  if (!o) return;
  document.getElementById('edit-order-id').value     = o.id;
  document.getElementById('edit-order-status').value = o.status;

  const itemsDiv = document.getElementById('edit-order-items');
  itemsDiv.innerHTML = o.items.map((item, idx) => `
    <div class="edit-order-item-row">
      <span>${escHtml(item.name)}</span>
      <input type="number" id="edit-item-qty-${idx}" value="${item.quantity}" min="1" max="99" />
      <span>$${item.price.toFixed(2)}</span>
      <button class="btn-icon danger" onclick="removeOrderItemFromEdit(${id}, ${idx})">&#10005;</button>
    </div>
  `).join('');

  openModal('modal-edit-order');
}

function removeOrderItemFromEdit(orderId, idx) {
  const o = orders.find(x => x.id === orderId);
  if (!o) return;
  o.items.splice(idx, 1);
  openEditOrder(orderId);   // re-render
}

function saveOrderEdits() {
  const id     = parseInt(document.getElementById('edit-order-id').value);
  const status = document.getElementById('edit-order-status').value;
  const o      = orders.find(x => x.id === id);
  if (!o) return;

  o.status = status;
  o.items.forEach((item, idx) => {
    const input = document.getElementById('edit-item-qty-' + idx);
    if (input) {
      const q = parseInt(input.value);
      if (q > 0) item.quantity = q;
    }
  });

  // If COMPLETED or CANCELLED, free the table
  if (status === 'COMPLETED' || status === 'CANCELLED') {
    const tbl = tables.find(t => t.id === o.tableId);
    if (tbl) tbl.status = 'FREE';
  }

  closeModal('modal-edit-order');
  renderHistory();
  showToast(`Order #${id} updated.`, 'success');
}

function deleteOrder(id) {
  if (!confirm(`Permanently delete Order #${id}?`)) return;
  orders = orders.filter(o => o.id !== id);
  renderHistory();
  showToast(`Order #${id} deleted.`, 'info');
}

// ─────────────────────────────────────────────────────────────────
// 9. MODAL UTILITIES
// ─────────────────────────────────────────────────────────────────

function openModal(id) {
  document.getElementById(id).classList.add('open');
}

function closeModal(id) {
  document.getElementById(id).classList.remove('open');
}

// Close modal on overlay click
document.querySelectorAll('.modal-overlay').forEach(overlay => {
  overlay.addEventListener('click', (e) => {
    if (e.target === overlay) closeModal(overlay.id);
  });
});

// Close modal on Escape
document.addEventListener('keydown', (e) => {
  if (e.key === 'Escape') {
    document.querySelectorAll('.modal-overlay.open').forEach(m => closeModal(m.id));
  }
});

// ─────────────────────────────────────────────────────────────────
// 10. TOAST NOTIFICATIONS
// ─────────────────────────────────────────────────────────────────

let _toastTimer = null;

function showToast(message, type = 'info') {
  const toast = document.getElementById('toast');
  toast.textContent = message;
  toast.className   = `toast ${type} show`;
  clearTimeout(_toastTimer);
  _toastTimer = setTimeout(() => { toast.classList.remove('show'); }, 3000);
}

// ─────────────────────────────────────────────────────────────────
// 11. LIVE CLOCK
// ─────────────────────────────────────────────────────────────────

function updateClock() {
  const now = new Date();
  const h   = String(now.getHours()).padStart(2, '0');
  const m   = String(now.getMinutes()).padStart(2, '0');
  const s   = String(now.getSeconds()).padStart(2, '0');
  const el  = document.getElementById('header-clock');
  if (el) el.textContent = `${h}:${m}:${s}`;
}

setInterval(updateClock, 1000);
updateClock();

// ─────────────────────────────────────────────────────────────────
// 12. HELPERS
// ─────────────────────────────────────────────────────────────────

function escHtml(str) {
  const div = document.createElement('div');
  div.appendChild(document.createTextNode(str));
  return div.innerHTML;
}

function formatDate(date) {
  const d = new Date(date);
  return d.toLocaleDateString() + ' ' + d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

// ─────────────────────────────────────────────────────────────────
// 14. PAYMENT & BILLING LOGIC
// ─────────────────────────────────────────────────────────────────

function renderPaymentsSection() {
  renderPaymentStats();
  renderUnsettledOrders();
  renderPaymentHistory(_currentPaymentFilter);
}

function renderPaymentStats() {
  const statsEl = document.getElementById('payment-stats');
  if (!statsEl) return;

  const completed = payments.filter(p => p.status === 'COMPLETED');
  const refunded  = payments.filter(p => p.status === 'REFUNDED');
  const totalRev  = completed.reduce((sum, p) => sum + p.amountDue, 0);

  // Unsettled orders count
  const unsettledCount = orders.filter(o => 
    o.status !== 'CANCELLED' && !payments.some(p => p.orderId === o.id && p.status === 'COMPLETED')
  ).length;

  statsEl.innerHTML = `
    <div class="stat-card green">
      <div class="stat-value">$${totalRev.toFixed(2)}</div>
      <div class="stat-label">Net Revenue</div>
    </div>
    <div class="stat-card amber">
      <div class="stat-value">${completed.length}</div>
      <div class="stat-label">Completed Settlements</div>
    </div>
    <div class="stat-card blue">
      <div class="stat-value">${unsettledCount}</div>
      <div class="stat-label">Pending Settlements</div>
    </div>
    <div class="stat-card red">
      <div class="stat-value">${refunded.length}</div>
      <div class="stat-label">Refunded</div>
    </div>
  `;
}

function renderUnsettledOrders() {
  const listEl  = document.getElementById('unsettled-orders-list');
  const badgeEl = document.getElementById('unsettled-count-badge');
  if (!listEl) return;

  const unsettled = orders.filter(o => 
    o.status !== 'CANCELLED' && !payments.some(p => p.orderId === o.id && p.status === 'COMPLETED')
  );

  if (badgeEl) {
    badgeEl.textContent = `${unsettled.length} Pending`;
  }

  if (unsettled.length === 0) {
    listEl.innerHTML = `<p style="color:var(--text-muted);padding:14px;grid-column:1/-1;">All active orders have been paid and settled.</p>`;
    return;
  }

  listEl.innerHTML = unsettled.map(o => {
    const total = getOrderTotal(o);
    return `
      <div class="unsettled-order-card">
        <div class="unsettled-header">
          <span class="unsettled-title">Order #${o.id}</span>
          <span class="history-status status-${o.status}">${o.status}</span>
        </div>
        <div class="unsettled-meta">
          <span>Table: <strong>Table #${o.tableId}</strong></span>
          <span>Items: <strong>${o.items.length} items</strong></span>
          <span>Placed: ${formatDate(o.createdAt)}</span>
        </div>
        <div class="unsettled-footer">
          <div class="unsettled-amount">$${total.toFixed(2)}</div>
          <button class="btn btn-primary btn-sm" onclick="openPaymentModal(${o.id})">&#128179; Settle Payment</button>
        </div>
      </div>
    `;
  }).join('');
}

function renderPaymentHistory(filter = 'ALL') {
  const tbody = document.getElementById('payment-history-tbody');
  if (!tbody) return;

  let list = [...payments].sort((a, b) => b.paidAt - a.paidAt);

  if (filter === 'REFUNDED') {
    list = list.filter(p => p.status === 'REFUNDED');
  } else if (filter !== 'ALL') {
    list = list.filter(p => p.method === filter);
  }

  if (list.length === 0) {
    tbody.innerHTML = `<tr><td colspan="9" style="text-align:center;color:var(--text-muted);padding:24px;">No payment records found for "${filter}".</td></tr>`;
    return;
  }

  tbody.innerHTML = list.map(p => {
    const change = Math.max(0, p.amountPaid - p.amountDue);
    const dateStr = formatDate(p.paidAt);
    const methodCls = p.method.toLowerCase();
    const isCompleted = p.status === 'COMPLETED';

    return `
      <tr>
        <td><strong>#REC-${String(p.id).padStart(4, '0')}</strong></td>
        <td>Order #${p.orderId}</td>
        <td>Table #${p.tableId}</td>
        <td><span class="badge-method ${methodCls}">${p.method}</span></td>
        <td>
          <span class="${isCompleted ? 'badge-status-completed' : 'badge-status-refunded'}">
            ${p.status}
          </span>
        </td>
        <td><strong>$${p.amountDue.toFixed(2)}</strong></td>
        <td>
          $${p.amountPaid.toFixed(2)}
          ${change > 0 ? `<br/><small style="color:#4ade80;">(Chg: $${change.toFixed(2)})</small>` : ''}
        </td>
        <td style="font-size:0.8rem;color:var(--text-secondary);">${dateStr}</td>
        <td>
          <div style="display:flex;gap:6px;">
            <button class="btn btn-sm btn-ghost" style="padding:4px 8px;font-size:0.75rem;" title="View & Print Receipt" onclick="openReceiptModal(${p.id})">
              &#128196; Receipt
            </button>
            ${isCompleted ? `
              <button class="btn btn-sm btn-danger-sm" style="padding:4px 8px;font-size:0.75rem;" title="Refund" onclick="refundPayment(${p.id})">
                Refund
              </button>
            ` : ''}
          </div>
        </td>
      </tr>
    `;
  }).join('');
}

function filterPayments(filter, btnEl) {
  _currentPaymentFilter = filter;
  const bar = document.getElementById('payment-filter-bar');
  if (bar) {
    bar.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
  }
  if (btnEl) btnEl.classList.add('active');
  renderPaymentHistory(filter);
}

function openSettleOrderSelector() {
  const unsettled = orders.filter(o => 
    o.status !== 'CANCELLED' && !payments.some(p => p.orderId === o.id && p.status === 'COMPLETED')
  );
  const container = document.getElementById('unsettled-modal-list');
  if (!container) return;

  if (unsettled.length === 0) {
    container.innerHTML = '<p style="color:var(--text-muted);padding:14px;">No unsettled orders at the moment.</p>';
  } else {
    container.innerHTML = unsettled.map(o => {
      const total = getOrderTotal(o);
      return `
        <div class="unsettled-modal-item">
          <div>
            <strong>Order #${o.id}</strong> (Table #${o.tableId}) &bull; ${o.items.length} items
            <div style="font-size:0.8rem;color:var(--text-secondary);">${formatDate(o.createdAt)}</div>
          </div>
          <div style="display:flex;align-items:center;gap:12px;">
            <strong style="color:var(--accent);font-size:1.1rem;">$${total.toFixed(2)}</strong>
            <button class="btn btn-sm btn-primary" onclick="closeModal('modal-select-order'); openPaymentModal(${o.id})">Select</button>
          </div>
        </div>
      `;
    }).join('');
  }
  openModal('modal-select-order');
}

function openPaymentModal(orderId) {
  const order = orders.find(o => o.id === orderId);
  if (!order) {
    showToast('Order not found.', 'error');
    return;
  }

  const total = getOrderTotal(order);
  if (total <= 0) {
    showToast('Order total is $0.00. Cannot process payment.', 'error');
    return;
  }

  _activePaymentOrderId = orderId;
  _selectedPaymentMethod = 'CASH';

  document.getElementById('pay-order-id').value = order.id;
  document.getElementById('pay-order-ref').textContent = `Order #${order.id}`;
  document.getElementById('pay-table-ref').textContent = `Table #${order.tableId}`;
  
  const discInput = document.getElementById('pay-discount-percent');
  if (discInput) discInput.value = 0;

  // Items brief
  const itemsPreview = document.getElementById('pay-items-preview');
  if (itemsPreview) {
    itemsPreview.innerHTML = order.items.map(i => 
      `<div>${escHtml(i.name)} x${i.quantity} &bull; $${(i.price * i.quantity).toFixed(2)}</div>`
    ).join('');
  }

  // Reset method buttons
  document.querySelectorAll('.method-btn').forEach(btn => {
    btn.classList.toggle('active', btn.getAttribute('data-method') === 'CASH');
  });

  updatePaymentBillDiscount();

  // Show cash container, hide digital
  document.getElementById('pay-cash-container').style.display    = 'block';
  document.getElementById('pay-digital-container').style.display = 'none';

  // Checked by default
  document.getElementById('pay-free-table-check').checked = true;

  openModal('modal-payment');
}

function updatePaymentBillDiscount() {
  const orderId = _activePaymentOrderId;
  const order = orders.find(o => o.id === orderId);
  if (!order) return;

  const subTotal = getOrderTotal(order);
  const discInput = document.getElementById('pay-discount-percent');
  const discountPercent = Math.min(100, Math.max(0, parseFloat(discInput ? discInput.value : 0) || 0));
  const discountAmount = (subTotal * discountPercent) / 100;
  const netDue = Math.max(0, subTotal - discountAmount);

  const subTotalEl = document.getElementById('pay-subtotal');
  if (subTotalEl) subTotalEl.textContent = `$${subTotal.toFixed(2)}`;

  const discLine = document.getElementById('pay-discount-line');
  const discAmtEl = document.getElementById('pay-discount-amount');
  if (discLine && discAmtEl) {
    if (discountAmount > 0) {
      discLine.style.display = 'flex';
      discAmtEl.textContent = `-$${discountAmount.toFixed(2)} (${discountPercent.toFixed(1)}%)`;
    } else {
      discLine.style.display = 'none';
    }
  }

  const dueEl = document.getElementById('pay-amount-due');
  if (dueEl) dueEl.textContent = `$${netDue.toFixed(2)}`;

  // Reset cash fields
  const tenderedInput = document.getElementById('pay-cash-tendered');
  if (tenderedInput && _selectedPaymentMethod === 'CASH') {
    tenderedInput.min = netDue.toFixed(2);
    tenderedInput.value = netDue.toFixed(2);
  }

  // Setup quick tender chips
  const chipsContainer = document.getElementById('tender-quick-chips');
  if (chipsContainer) {
    const round10 = Math.ceil(netDue / 10) * 10;
    const round50 = Math.ceil(netDue / 50) * 50;
    chipsContainer.innerHTML = `
      <button type="button" class="tender-chip" onclick="setQuickTender(${netDue})">Exact ($${netDue.toFixed(2)})</button>
      ${round10 > netDue ? `<button type="button" class="tender-chip" onclick="setQuickTender(${round10})">$${round10.toFixed(2)}</button>` : ''}
      ${round50 > round10 ? `<button type="button" class="tender-chip" onclick="setQuickTender(${round50})">$${round50.toFixed(2)}</button>` : ''}
      <button type="button" class="tender-chip" onclick="setQuickTender(${netDue + 10})">+$10 Tip</button>
      <button type="button" class="tender-chip" onclick="setQuickTender(${netDue + 20})">+$20 Tip</button>
    `;
  }

  calculateCashChange();
}

function selectPaymentMethod(method, btnEl) {
  _selectedPaymentMethod = method;
  document.querySelectorAll('.method-btn').forEach(b => b.classList.remove('active'));
  if (btnEl) btnEl.classList.add('active');

  const cashBox    = document.getElementById('pay-cash-container');
  const digitalBox = document.getElementById('pay-digital-container');
  const iconEl     = document.getElementById('pay-digital-icon');
  const msgEl      = document.getElementById('pay-digital-msg');

  if (method === 'CASH') {
    cashBox.style.display    = 'block';
    digitalBox.style.display = 'none';
    calculateCashChange();
  } else {
    cashBox.style.display    = 'none';
    digitalBox.style.display = 'block';

    if (method === 'CREDIT_CARD' || method === 'DEBIT_CARD') {
      iconEl.innerHTML = '&#128179;';
      msgEl.textContent = `Insert chip, tap card, or swipe to authorize with POS.`;
    } else if (method === 'UPI') {
      iconEl.innerHTML = '&#128242;';
      msgEl.textContent = `Scan dynamic UPI QR code on terminal or accept push prompt.`;
    } else {
      iconEl.innerHTML = '&#127760;';
      msgEl.textContent = `Online invoice link dispatched to customer device.`;
    }
  }
}

function setQuickTender(amount) {
  const input = document.getElementById('pay-cash-tendered');
  if (input) {
    input.value = amount.toFixed(2);
    calculateCashChange();
  }
}

function calculateCashChange() {
  const orderId = _activePaymentOrderId;
  const order = orders.find(o => o.id === orderId);
  if (!order) return;

  const dueEl = document.getElementById('pay-amount-due');
  const totalDue  = dueEl ? (parseFloat(dueEl.textContent.replace('$', '')) || 0) : getOrderTotal(order);
  const inputVal  = parseFloat(document.getElementById('pay-cash-tendered').value) || 0;
  const diff      = inputVal - totalDue;
  const amountEl  = document.getElementById('pay-change-amount');
  const calloutEl = document.getElementById('pay-change-callout');

  if (!amountEl || !calloutEl) return;

  if (diff >= 0) {
    amountEl.textContent = `$${diff.toFixed(2)}`;
    amountEl.style.color = '#4ade80';
    calloutEl.style.borderColor = 'rgba(34, 197, 94, 0.25)';
    calloutEl.style.background  = 'rgba(34, 197, 94, 0.1)';
  } else {
    amountEl.textContent = `-$${Math.abs(diff).toFixed(2)} (Short)`;
    amountEl.style.color = '#ef4444';
    calloutEl.style.borderColor = 'rgba(239, 68, 68, 0.25)';
    calloutEl.style.background  = 'rgba(239, 68, 68, 0.1)';
  }
}

function submitPayment() {
  const orderId = _activePaymentOrderId;
  const order = orders.find(o => o.id === orderId);
  if (!order) {
    showToast('Invalid order.', 'error');
    return;
  }

  const subTotal = getOrderTotal(order);
  const discountPercent = Math.min(100, Math.max(0, parseFloat(document.getElementById('pay-discount-percent')?.value) || 0));

  // Generate Bill entity for order including discounts
  const bill = addBillData(order.id, order.tableId, subTotal, discountPercent);
  bill.status = 'PAID';

  const totalDue = bill.totalAmount;
  let amountPaid = totalDue;

  if (_selectedPaymentMethod === 'CASH') {
    amountPaid = parseFloat(document.getElementById('pay-cash-tendered').value) || 0;
    if (amountPaid < totalDue) {
      showToast(`Tendered cash ($${amountPaid.toFixed(2)}) is less than total due ($${totalDue.toFixed(2)}).`, 'error');
      return;
    }
  }

  // Create payment record solely using the Bill entity
  const newPayment = addPaymentData(
    bill,
    amountPaid,
    _selectedPaymentMethod,
    'COMPLETED',
    new Date()
  );

  // Mark order COMPLETED
  order.status = 'COMPLETED';

  // Free table if checked
  const freeTableCheck = document.getElementById('pay-free-table-check');
  if (freeTableCheck && freeTableCheck.checked) {
    const table = tables.find(t => t.id === order.tableId);
    if (table) table.status = 'FREE';
  }

  closeModal('modal-payment');
  showToast(`Bill #${bill.id} settled! Payment of $${totalDue.toFixed(2)} completed successfully!`, 'success');

  // Refresh current sections
  renderTables();
  renderHistory();
  renderPaymentsSection();

  // Prompt receipt display
  openReceiptModal(newPayment.id);
}

function openReceiptModal(paymentId) {
  _activeReceiptPaymentId = paymentId;
  const payment = payments.find(p => p.id === paymentId);
  if (!payment) return;

  const order = orders.find(o => o.id === payment.orderId);
  const container = document.getElementById('receipt-paper');
  if (!container) return;

  const bill = bills.find(b => b.id === payment.billId) || {
    id: payment.billId || 1,
    subTotal: payment.amountDue,
    discountAmount: 0,
    discountPercentage: 0,
    totalAmount: payment.amountDue
  };

  const change = Math.max(0, payment.amountPaid - payment.amountDue);
  const dateStr = formatDate(payment.paidAt);
  const isRefunded = payment.status === 'REFUNDED';

  let itemsRows = '';
  if (order && order.items.length > 0) {
    itemsRows = order.items.map(i => `
      <tr>
        <td>${escHtml(i.name)}</td>
        <td style="text-align:center;">${i.quantity}</td>
        <td style="text-align:right;">$${i.price.toFixed(2)}</td>
        <td style="text-align:right;">$${(i.price * i.quantity).toFixed(2)}</td>
      </tr>
    `).join('');
  } else {
    itemsRows = `<tr><td colspan="4">Order items settled</td></tr>`;
  }

  container.innerHTML = `
    <div class="receipt-stamp ${isRefunded ? 'refunded' : ''}">${payment.status}</div>
    <div class="receipt-header">
      <div class="receipt-brand">&#127869; GRAND BISTRO</div>
      <div class="receipt-sub">FINE DINING &amp; COCKTAIL BAR</div>
      <div class="receipt-sub">100 Luxury Avenue, Suite 400</div>
      <div class="receipt-sub">Phone: +1 (555) 019-2834 &bull; GSTIN: 29AABCG1234D1Z2</div>
    </div>

    <div class="receipt-divider"></div>

    <div class="receipt-meta-row">
      <span>Receipt #: <strong>REC-${String(payment.id).padStart(4, '0')}</strong></span>
      <span>Bill #: <strong>BILL-${String(bill.id).padStart(4, '0')}</strong></span>
    </div>
    <div class="receipt-meta-row">
      <span>Order: <strong>#${payment.orderId}</strong></span>
      <span>Table: <strong>Table #${payment.tableId}</strong></span>
    </div>
    <div class="receipt-meta-row">
      <span>Date: ${dateStr}</span>
      <span>Server: POS Station 1</span>
    </div>

    <div class="receipt-divider"></div>

    <table class="receipt-table-content">
      <thead>
        <tr>
          <th>Item</th>
          <th style="text-align:center;">Qty</th>
          <th style="text-align:right;">Price</th>
          <th style="text-align:right;">Total</th>
        </tr>
      </thead>
      <tbody>
        ${itemsRows}
      </tbody>
    </table>

    <div class="receipt-divider"></div>

    <div class="receipt-meta-row">
      <span>Gross Subtotal</span>
      <span>$${bill.subTotal.toFixed(2)}</span>
    </div>
    ${bill.discountAmount > 0 ? `
    <div class="receipt-meta-row" style="color:#4ade80;">
      <span>Discount (${bill.discountPercentage.toFixed(1)}%)</span>
      <span>-$${bill.discountAmount.toFixed(2)}</span>
    </div>
    ` : ''}
    <div class="receipt-meta-row">
      <span>Taxes &amp; Fees</span>
      <span>$0.00 (Incl.)</span>
    </div>

    <div class="receipt-double-divider"></div>

    <div class="receipt-total-row">
      <span>TOTAL DUE</span>
      <span>$${payment.amountDue.toFixed(2)}</span>
    </div>

    <div class="receipt-meta-row" style="margin-top:8px;">
      <span>Method: <strong>${payment.method}</strong></span>
      <span>Tendered: $${payment.amountPaid.toFixed(2)}</span>
    </div>
    <div class="receipt-meta-row">
      <span>Change Returned:</span>
      <span><strong>$${change.toFixed(2)}</strong></span>
    </div>

    <div class="receipt-divider"></div>

    <div class="receipt-footer-text">
      THANK YOU FOR DINING WITH US!<br/>
      Please retain this receipt for your records.<br/>
      Visit us again soon &bull; www.grandbistro.com
    </div>
  `;

  openModal('modal-receipt');
}

function printReceipt() {
  window.print();
}

function refundPayment(paymentId) {
  const payment = payments.find(p => p.id === paymentId);
  if (!payment) return;

  if (payment.status !== 'COMPLETED') {
    showToast('Only completed payments can be refunded.', 'error');
    return;
  }

  requireAdmin(() => {
    payment.status = 'REFUNDED';
    showToast(`Payment #REC-${String(payment.id).padStart(4, '0')} has been REFUNDED.`, 'info');
    renderHistory();
    renderPaymentsSection();
    if (_activeReceiptPaymentId === paymentId) {
      openReceiptModal(paymentId);
    }
  });
}

// ─────────────────────────────────────────────────────────────────
// 13. BOOT
// ─────────────────────────────────────────────────────────────────

seedData();
renderTables();   // render initial section
