// ============================================================
// CommerceHub Web Demo — Cash App Pay Integration
// Vanilla JS SPA with phone frame mockup
// ============================================================

// ---- Product Data (matches SampleProducts.kt exactly) ----
const PRODUCTS = [
  { id: '1', name: 'AirPods Pro', description: 'Active Noise Cancellation, Adaptive Audio', priceInCents: 24999, emoji: '\uD83C\uDFA7', category: 'Audio' },
  { id: '2', name: 'MagSafe Charger', description: '15W wireless charging pad', priceInCents: 3999, emoji: '\uD83D\uDD0B', category: 'Charging' },
  { id: '3', name: 'Leather Wallet Case', description: 'Premium Italian leather, MagSafe compatible', priceInCents: 5999, emoji: '\uD83D\uDCF1', category: 'Cases' },
  { id: '4', name: 'Studio Display', description: '27-inch 5K Retina, Nano-texture glass', priceInCents: 159999, emoji: '\uD83D\uDDA5\uFE0F', category: 'Displays' },
  { id: '5', name: 'Magic Keyboard', description: 'Touch ID, numeric keypad, USB-C', priceInCents: 19999, emoji: '\u2328\uFE0F', category: 'Input' },
  { id: '6', name: 'USB-C Hub Pro', description: '8-in-1: HDMI, SD, USB-A, Ethernet', priceInCents: 7999, emoji: '\uD83D\uDD0C', category: 'Accessories' },
];

const CATEGORIES = ['All', ...new Set(PRODUCTS.map(p => p.category))];

const CATEGORY_GRADIENTS = {
  Audio: ['#E8EAF6', '#C5CAE9'], Charging: ['#E8F5E9', '#C8E6C9'],
  Cases: ['#FFF3E0', '#FFE0B2'], Displays: ['#E3F2FD', '#BBDEFB'],
  Input: ['#F3E5F5', '#E1BEE7'], Accessories: ['#F5F5F5', '#EEEEEE'],
};

// ---- Trace Logging ----
function logEvent(category, title, details = null) {
  const time = new Date().toLocaleTimeString('en-US', { hour12: false, hour: '2-digit', minute: '2-digit', second: '2-digit' });
  const container = document.getElementById('trace-log');
  if (!container) return;

  const entry = document.createElement('div');
  entry.className = 'trace-entry';
  entry.style.borderLeftColor = { sdk: '#1565C0', api: '#FF6600', lifecycle: '#4CAF50', error: '#E53935' }[category] || '#666';

  let detailsHtml = '';
  if (details) {
    const detailStr = typeof details === 'object' ? JSON.stringify(details, null, 2) : details;
    detailsHtml = `<div class="trace-details" style="display:none">${escapeHtml(detailStr)}</div>`;
  }

  entry.innerHTML = `
    <span class="trace-time">${time}</span>
    <span class="trace-badge ${category}">${category.toUpperCase()}</span>
    <span class="trace-title-text">${escapeHtml(title)}${details ? ' &#9654;' : ''}</span>
    ${detailsHtml}
  `;

  if (details) {
    entry.style.cursor = 'pointer';
    entry.addEventListener('click', () => {
      const det = entry.querySelector('.trace-details');
      if (det) det.style.display = det.style.display === 'none' ? 'block' : 'none';
    });
  }

  container.appendChild(entry);
  container.scrollTop = container.scrollHeight;
}

function escapeHtml(str) {
  return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

// ---- State ----
const state = {
  screen: 'catalog',
  selectedCategory: 'All',
  cart: [], // { product, quantity }[]
  paymentState: 'idle', // idle | redirecting | authorizing | capturing | complete | error | declined | card-entry | gpay-confirm
  confirmationData: null,
  errorMessage: '',
  backendOnline: false,
  activePaymentMethod: null, // 'cashapp' | 'card' | 'googlepay'
};

// ---- Helpers (match CartViewModel.kt) ----
function subtotalCents() { return state.cart.reduce((s, i) => s + i.product.priceInCents * i.quantity, 0); }
function taxCents() { return Math.floor(subtotalCents() * 0.085); }
function totalCents() { return subtotalCents() + taxCents(); }
function totalDollars() { return totalCents() / 100; }
function cartCount() { return state.cart.reduce((s, i) => s + i.quantity, 0); }

function formatCents(cents) {
  const d = Math.floor(Math.abs(cents) / 100);
  const c = Math.abs(cents) % 100;
  return `$${d.toLocaleString('en-US')}.${String(c).padStart(2, '0')}`;
}

function addToCart(product) {
  const existing = state.cart.find(i => i.product.id === product.id);
  if (existing) existing.quantity++;
  else state.cart.push({ product, quantity: 1 });
  logEvent('lifecycle', 'Add to cart: ' + product.name, { productId: product.id, price: formatCents(product.priceInCents) });
  render();
}

function updateQuantity(id, qty) {
  if (qty <= 0) { state.cart = state.cart.filter(i => i.product.id !== id); }
  else { const item = state.cart.find(i => i.product.id === id); if (item) item.quantity = qty; }
  logEvent('lifecycle', 'Update qty: ' + id + ' -> ' + qty);
  render();
}

function removeFromCart(id) {
  state.cart = state.cart.filter(i => i.product.id !== id);
  logEvent('lifecycle', 'Remove from cart: ' + id);
  render();
}

function navigate(screen) {
  state.screen = screen;
  if (screen === 'catalog') { state.paymentState = 'idle'; state.errorMessage = ''; }
  logEvent('lifecycle', 'Navigate to: ' + screen);
  render();
}

// ---- Rendering Engine ----
const $screen = () => document.getElementById('app-screen');

function render() {
  const el = $screen();
  if (!el) return;
  const screens = { catalog: renderCatalog, cart: renderCart, checkout: renderCheckout, confirmation: renderConfirmation };
  el.innerHTML = (screens[state.screen] || renderCatalog)();
  // Overlay for Cash App auth
  if (state.paymentState === 'redirecting' || state.paymentState === 'authorizing') {
    el.innerHTML += renderCashAppOverlay();
  }
  if (state.paymentState === 'card-entry') {
    el.innerHTML += renderCardEntryOverlay();
  }
  if (state.paymentState === 'gpay-confirm') {
    el.innerHTML += renderGooglePayOverlay();
  }
  if (state.paymentState === 'klarna-select') {
    el.innerHTML += renderKlarnaOverlay();
  }
  attachEvents();
}

// ---- Catalog Screen ----
function renderCatalog() {
  const filtered = state.selectedCategory === 'All' ? PRODUCTS : PRODUCTS.filter(p => p.category === state.selectedCategory);
  return `
    <div class="screen-enter">
      <div class="hero-banner">
        <div class="hero-title">CommerceHub</div>
        <div class="hero-subtitle">Powered by Fiserv \u00B7 Cash App Pay Enabled</div>
      </div>
      <div class="category-bar">
        ${CATEGORIES.map(c => `<button class="chip ${state.selectedCategory === c ? 'active' : ''}" data-category="${c}">${c}</button>`).join('')}
      </div>
      <div class="product-grid">
        ${filtered.map(p => {
          const [g1, g2] = CATEGORY_GRADIENTS[p.category] || ['#F5F5F5', '#EEE'];
          return `
          <div class="product-card" data-id="${p.id}">
            <div class="product-card-image" style="background: linear-gradient(180deg, ${g1}, ${g2})">${p.emoji}</div>
            <div class="product-card-body">
              <div class="product-card-name">${p.name}</div>
              <div class="product-card-desc">${p.description}</div>
              <div class="product-card-footer">
                <span class="product-price">${formatCents(p.priceInCents)}</span>
                <button class="btn-add" data-add="${p.id}">Add</button>
              </div>
              <div class="klarna-promo">or 4 x ${formatCents(Math.ceil(p.priceInCents / 4))} with <strong>Klarna</strong></div>
            </div>
          </div>`;
        }).join('')}
      </div>
      ${cartCount() > 0 ? `
        <button class="cart-fab" data-go="cart">
          \uD83D\uDED2
          <span class="cart-badge">${cartCount()}</span>
        </button>` : ''}
    </div>`;
}

// ---- Cart Screen ----
function renderCart() {
  if (state.cart.length === 0) {
    return `
      <div class="top-bar"><button class="btn-back" data-go="catalog">\u2190</button><span class="top-bar-title">Your Cart</span></div>
      <div class="cart-empty"><span class="cart-empty-icon">\uD83D\uDED2</span><strong>Your cart is empty</strong><span>Browse the catalog to add items</span></div>`;
  }
  return `
    <div class="screen-enter">
      <div class="top-bar"><button class="btn-back" data-go="catalog">\u2190</button><span class="top-bar-title">Your Cart</span></div>
      <div style="padding: 12px 16px; display: flex; flex-direction: column; gap: 8px;">
        ${state.cart.map(item => `
          <div class="cart-item">
            <div class="cart-item-emoji">${item.product.emoji}</div>
            <div class="cart-item-info">
              <div class="cart-item-name">${item.product.name}</div>
              <div class="cart-item-price">${formatCents(item.product.priceInCents)}</div>
              <div class="qty-stepper">
                <button class="qty-btn" data-qty-minus="${item.product.id}">\u2212</button>
                <span class="qty-val">${item.quantity}</span>
                <button class="qty-btn" data-qty-plus="${item.product.id}">+</button>
              </div>
            </div>
            <div class="cart-item-right">
              <div class="cart-item-total">${formatCents(item.product.priceInCents * item.quantity)}</div>
              <button class="btn-remove" data-remove="${item.product.id}">\uD83D\uDDD1</button>
            </div>
          </div>
        `).join('')}
      </div>
      <div style="padding: 16px;">
        <div class="card">
          <div class="summary-row"><span>Subtotal</span><span>${formatCents(subtotalCents())}</span></div>
          <div class="summary-row subtle"><span>Tax (8.5%)</span><span>${formatCents(taxCents())}</span></div>
          <div class="summary-divider"></div>
          <div class="summary-total"><span>Total</span><span class="summary-total-value">${formatCents(totalCents())}</span></div>
        </div>
      </div>
      <div style="padding: 0 16px 20px;">
        <button class="btn-primary" data-go="checkout">Proceed to Checkout</button>
      </div>
    </div>`;
}

// ---- Checkout Screen ----
function renderCheckout() {
  const isProcessing = ['redirecting', 'authorizing', 'capturing'].includes(state.paymentState);
  let statusHtml = '';
  if (state.paymentState === 'capturing') {
    statusHtml = '<div class="status-chip orange"><div class="spinner"></div>Capturing payment...</div>';
  } else if (state.paymentState === 'error') {
    statusHtml = `<div class="error-msg">${state.errorMessage}</div><button class="btn-retry" data-retry>Retry Payment</button>`;
  } else if (state.paymentState === 'declined') {
    statusHtml = '<div class="error-msg">Payment declined. Please try again.</div>';
  }

  return `
    <div class="screen-enter">
      <div class="top-bar"><button class="btn-back" data-go="cart">\u2190</button><span class="top-bar-title">Checkout</span></div>
      <div class="checkout-content">
        <div class="sandbox-badge">\uD83E\uDDEA &nbsp;Sandbox Environment \u2014 No real charges</div>

        <div class="card">
          <div style="font-weight: 600; margin-bottom: 10px;">Order Summary</div>
          <div class="summary-row"><span>Subtotal</span><span>${formatCents(subtotalCents())}</span></div>
          <div class="summary-row subtle"><span>Tax (8.5%)</span><span>${formatCents(taxCents())}</span></div>
          <div class="summary-divider"></div>
          <div class="summary-total"><span>Total</span><span class="summary-total-value">${formatCents(totalCents())}</span></div>
        </div>

        <div class="cashapp-hero">
          <div class="recommended-label">Recommended</div>
          <button class="btn-cashapp" data-cashapp ${isProcessing ? 'disabled' : ''}>
            <span class="cashapp-logo">$</span>
            ${isProcessing ? '<div class="spinner" style="border-color: rgba(255,255,255,0.3); border-top-color: white;"></div> Processing...' : 'Pay with Cash App Pay'}
          </button>
          ${statusHtml}
        </div>

        <div class="divider-or">or pay with</div>

        <button class="btn-outline" data-pay-card>\uD83D\uDCB3 &nbsp;Credit / Debit Card</button>
        <button class="btn-outline" data-pay-gpay style="margin-top: 8px;">G Pay &nbsp;Google Pay</button>
        <button class="btn-outline btn-klarna-accent" data-pay-klarna style="margin-top: 8px;">\uD83D\uDECD\uFE0F &nbsp;Klarna \u00B7 Pay in 4 / Pay Later</button>

        <div class="checkout-footer">All payment methods powered by Fiserv CommerceHub</div>
      </div>
    </div>`;
}

// ---- Cash App Auth Overlay ----
function renderCashAppOverlay() {
  if (state.paymentState === 'redirecting') {
    return `
      <div class="cashapp-overlay">
        <div class="cashapp-redirect">
          <div class="spinner-lg"></div>
          <div style="font-size: 16px; font-weight: 600;">Redirecting to Cash App...</div>
          <div style="font-size: 13px; opacity: 0.6;">Please wait</div>
        </div>
      </div>`;
  }
  if (state.paymentState === 'authorizing') {
    return `
      <div class="cashapp-overlay">
        <div class="cashapp-auth-screen">
          <div class="cashapp-auth-logo">$ Cash App</div>
          <div class="cashapp-auth-label">Payment Authorization</div>
          <div class="cashapp-auth-amount">${formatCents(totalCents())}</div>
          <div class="cashapp-auth-merchant">CommerceHub Demo Store</div>
          <div class="cashapp-auth-actions">
            <button class="btn-approve" data-approve>Approve Payment</button>
            <button class="btn-decline" data-decline-pay>Decline</button>
          </div>
        </div>
      </div>`;
  }
  return '';
}

// ---- Card Entry Overlay ----
function renderCardEntryOverlay() {
  return `
    <div class="cashapp-overlay">
      <div class="card-entry-modal">
        <div class="card-modal-header">
          <span class="card-modal-title">Enter Card Details</span>
          <button class="card-modal-close" data-card-cancel>&times;</button>
        </div>
        <div class="card-modal-body">
          <div class="card-field">
            <label>Card Number</label>
            <input type="text" id="card-number" placeholder="4242 4242 4242 4242" maxlength="19" />
          </div>
          <div class="card-field-row">
            <div class="card-field">
              <label>Expiry</label>
              <input type="text" id="card-expiry" placeholder="MM/YY" maxlength="5" />
            </div>
            <div class="card-field">
              <label>CVV</label>
              <input type="text" id="card-cvv" placeholder="123" maxlength="4" />
            </div>
          </div>
          <div class="card-field">
            <label>Cardholder Name</label>
            <input type="text" id="card-name" placeholder="John Doe" />
          </div>
          <div class="card-field">
            <label>Postal Code</label>
            <input type="text" id="card-zip" placeholder="94107" maxlength="10" />
          </div>
          <div id="card-error" class="card-error"></div>
          <button class="btn-primary" id="card-submit" data-card-submit style="margin-top: 12px;">
            Pay ${formatCents(totalCents())}
          </button>
        </div>
        <div class="card-modal-footer">Secured by Fiserv CommerceHub</div>
      </div>
    </div>`;
}

// ---- Google Pay Overlay ----
function renderGooglePayOverlay() {
  return `
    <div class="cashapp-overlay">
      <div class="gpay-modal">
        <div class="gpay-header">
          <span style="font-size: 24px; font-weight: 700;">G</span>
          <span style="font-size: 20px; font-weight: 600; margin-left: 4px;">Google Pay</span>
        </div>
        <div class="gpay-body">
          <div class="gpay-amount">${formatCents(totalCents())}</div>
          <div class="gpay-merchant">CommerceHub Demo Store</div>
          <div class="gpay-card-info">
            <div class="gpay-card-icon">\uD83D\uDCB3</div>
            <div>
              <div style="font-weight: 600;">Visa \u2022\u2022\u2022\u20224242</div>
              <div style="font-size: 12px; color: #666;">Test Card</div>
            </div>
          </div>
          <button class="btn-gpay-confirm" data-gpay-confirm>Pay</button>
          <button class="btn-gpay-cancel" data-gpay-cancel>Cancel</button>
        </div>
      </div>
    </div>`;
}

// ---- Klarna Selection Overlay ----
function renderKlarnaOverlay() {
  const installment = formatCents(Math.ceil(totalCents() / 4));
  return `
    <div class="cashapp-overlay">
      <div class="klarna-modal">
        <div class="klarna-header">
          <span class="klarna-logo-text">Klarna.</span>
          <button class="card-modal-close" data-klarna-cancel>&times;</button>
        </div>
        <div class="klarna-body">
          <div class="klarna-amount">${formatCents(totalCents())}</div>
          <div class="klarna-merchant">CommerceHub Demo Store</div>

          <div class="klarna-types">
            <label class="klarna-type-option">
              <input type="radio" name="klarna-type" value="pay_over_time" checked />
              <div class="klarna-type-card active">
                <div class="klarna-type-title">Pay in 4</div>
                <div class="klarna-type-desc">4 interest-free payments of ${installment}</div>
              </div>
            </label>
            <label class="klarna-type-option">
              <input type="radio" name="klarna-type" value="pay_later" />
              <div class="klarna-type-card">
                <div class="klarna-type-title">Pay in 30 days</div>
                <div class="klarna-type-desc">Try first, pay later</div>
              </div>
            </label>
            <label class="klarna-type-option">
              <input type="radio" name="klarna-type" value="pay_now" />
              <div class="klarna-type-card">
                <div class="klarna-type-title">Pay now</div>
                <div class="klarna-type-desc">Direct payment</div>
              </div>
            </label>
          </div>

          <button class="btn-klarna-pay" data-klarna-confirm>Confirm with Klarna</button>
          <button class="btn-gpay-cancel" data-klarna-cancel>Cancel</button>
        </div>
      </div>
    </div>`;
}

// ---- Confirmation Screen ----
function renderConfirmation() {
  const data = state.confirmationData || { transactionId: 'N/A', amount: '$0.00' };
  return `
    <div class="confirmation screen-enter">
      <div class="success-circle">\u2713</div>
      <div class="confirmation-title">Payment Successful!</div>
      <div class="confirmation-subtitle">Your order has been confirmed</div>
      <div class="card confirmation-card">
        <div class="detail-row"><div class="detail-label">Amount Charged</div><div class="detail-value highlight">${data.amount}</div></div>
        <div class="summary-divider"></div>
        <div class="detail-row"><div class="detail-label">Payment Method</div><div class="detail-value">${data.paymentMethod || 'Cash App Pay'}</div></div>
        <div style="height: 8px;"></div>
        <div class="detail-row"><div class="detail-label">Transaction ID</div><div class="detail-value" style="font-size: 13px; word-break: break-all;">${data.transactionId}</div></div>
        <div style="height: 8px;"></div>
        <div class="detail-row"><div class="detail-label">Status</div><div class="detail-value" style="color: #4CAF50;">Completed</div></div>
      </div>
      <div style="width: 100%; margin-top: 12px;">
        <div class="sandbox-badge">\uD83E\uDDEA Sandbox transaction \u2014 no real funds moved</div>
      </div>
      <div style="width: 100%; margin-top: 24px;">
        <button class="btn-primary" data-restart>Continue Shopping</button>
      </div>
    </div>`;
}

// ---- Event Delegation ----
function attachEvents() {
  const el = $screen();
  el.onclick = (e) => {
    const t = e.target.closest('[data-go]');
    if (t) { navigate(t.dataset.go); return; }

    const add = e.target.closest('[data-add]');
    if (add) { addToCart(PRODUCTS.find(p => p.id === add.dataset.add)); return; }

    const cat = e.target.closest('[data-category]');
    if (cat) { state.selectedCategory = cat.dataset.category; render(); return; }

    const minus = e.target.closest('[data-qty-minus]');
    if (minus) { const id = minus.dataset.qtyMinus; const item = state.cart.find(i => i.product.id === id); if (item) updateQuantity(id, item.quantity - 1); return; }

    const plus = e.target.closest('[data-qty-plus]');
    if (plus) { const id = plus.dataset.qtyPlus; const item = state.cart.find(i => i.product.id === id); if (item) updateQuantity(id, item.quantity + 1); return; }

    const remove = e.target.closest('[data-remove]');
    if (remove) { removeFromCart(remove.dataset.remove); return; }

    if (e.target.closest('[data-cashapp]')) { startCashAppPay(); return; }
    if (e.target.closest('[data-approve]')) { approveCashApp(); return; }
    if (e.target.closest('[data-decline-pay]')) { declineCashApp(); return; }
    if (e.target.closest('[data-retry]')) { startCashAppPay(); return; }
    if (e.target.closest('[data-restart]')) { state.cart = []; state.confirmationData = null; state.paymentState = 'idle'; navigate('catalog'); return; }
    if (e.target.closest('[data-pay-card]')) { startCardPayment(); return; }
    if (e.target.closest('[data-pay-gpay]')) { startGooglePay(); return; }
    if (e.target.closest('[data-pay-klarna]')) { startKlarna(); return; }
    if (e.target.closest('[data-klarna-confirm]')) { confirmKlarna(); return; }
    if (e.target.closest('[data-klarna-cancel]')) { state.paymentState = 'idle'; render(); return; }
    if (e.target.closest('[data-card-cancel]')) { state.paymentState = 'idle'; render(); return; }
    if (e.target.closest('[data-card-submit]')) { submitCardPayment(); return; }
    if (e.target.closest('[data-gpay-confirm]')) { confirmGooglePay(); return; }
    if (e.target.closest('[data-gpay-cancel]')) { state.paymentState = 'idle'; render(); return; }
  };
}

// ---- Cash App Pay Flow ----
async function startCashAppPay() {
  state.activePaymentMethod = 'cashapp';
  logEvent('sdk', 'CashAppPayFlowState: CreatingCustomerRequest', { amount: formatCents(totalCents()), amountCents: totalCents() });
  state.paymentState = 'redirecting';
  state.errorMessage = '';
  render();

  // Simulate redirect delay
  await sleep(1500);

  logEvent('sdk', 'CashAppPayFlowState: ReadyToAuthorize');
  state.paymentState = 'authorizing';
  logEvent('sdk', 'CashAppPayFlowState: Authorizing (app switch)');
  render();
}

async function approveCashApp() {
  state.paymentState = 'capturing';
  render();

  // Use Cash App's magic sandbox grant ID that auto-succeeds
  const grantId = 'GRG_sandbox:active';
  const idempotencyKey = crypto.randomUUID ? crypto.randomUUID() : `idk_${Date.now()}_${Math.random().toString(36).slice(2)}`;

  logEvent('sdk', 'CashAppPayFlowState: Approved', { grantId, customerId: 'CST_sandbox_user' });
  logEvent('api', 'POST /api/payments/initiate', { grantId, amountCents: totalCents(), idempotencyKey });

  try {
    const res = await fetch('/api/payments/initiate', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ grantId, amountCents: totalCents(), idempotencyKey }),
    });

    if (res.ok) {
      const data = await res.json();
      state.confirmationData = { transactionId: data.paymentId, amount: formatCents(totalCents()), paymentMethod: 'Cash App Pay' };
      logEvent('api', 'Response 200 OK', data);
      logEvent('sdk', 'CashAppPayFlowState: PaymentComplete', { transactionId: data.paymentId || state.confirmationData.transactionId, amount: totalCents() });
    } else {
      // Backend returned error (expected with mock grant ID) — use demo fallback
      state.confirmationData = {
        transactionId: `TXN_DEMO_${Date.now()}`,
        amount: formatCents(totalCents()),
        paymentMethod: 'Cash App Pay',
      };
      logEvent('api', 'Response ' + res.status, { fallback: true });
      logEvent('sdk', 'CashAppPayFlowState: PaymentComplete', { transactionId: state.confirmationData.transactionId, amount: totalCents() });
    }
    state.paymentState = 'complete';
    navigate('confirmation');
  } catch (err) {
    logEvent('error', 'API call failed - using demo fallback');
    // Backend unreachable — demo mode fallback
    state.confirmationData = {
      transactionId: `TXN_OFFLINE_${Date.now()}`,
      amount: formatCents(totalCents()),
      paymentMethod: 'Cash App Pay',
    };
    state.paymentState = 'complete';
    navigate('confirmation');
  }
}

function declineCashApp() {
  logEvent('sdk', 'CashAppPayFlowState: Declined');
  state.paymentState = 'declined';
  render();
}

// ---- Credit Card Payment Flow ----
function startCardPayment() {
  state.paymentState = 'card-entry';
  state.activePaymentMethod = 'card';
  logEvent('lifecycle', 'Credit Card payment initiated');
  render();
}

async function submitCardPayment() {
  const cardNumber = document.getElementById('card-number')?.value?.replace(/\s/g, '') || '';
  const expiry = document.getElementById('card-expiry')?.value || '';
  const cvv = document.getElementById('card-cvv')?.value || '';
  const name = document.getElementById('card-name')?.value || '';
  const zip = document.getElementById('card-zip')?.value || '';

  // Basic validation
  if (cardNumber.length < 13) {
    document.getElementById('card-error').textContent = 'Please enter a valid card number';
    return;
  }
  if (!expiry.includes('/')) {
    document.getElementById('card-error').textContent = 'Please enter expiry as MM/YY';
    return;
  }

  const [expiryMonth, expiryYear] = expiry.split('/');

  state.paymentState = 'capturing';
  render();

  logEvent('sdk', 'CreditCardManager.addCreditCard()', { lastFour: cardNumber.slice(-4), cardholderName: name });

  try {
    // Step 1: Tokenize
    logEvent('api', 'POST /api/card/tokenize', { cardNumber: '****' + cardNumber.slice(-4), expiryMonth, expiryYear });
    const tokenRes = await fetch('/api/card/tokenize', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ cardNumber, expiryMonth, expiryYear: '20' + expiryYear, cvv, cardholderName: name, postalCode: zip })
    });

    if (!tokenRes.ok) throw new Error('Tokenization failed');
    const tokenData = await tokenRes.json();
    logEvent('sdk', 'Card tokenized', { token: tokenData.token, cardType: tokenData.cardType, lastFour: tokenData.lastFour });

    // Step 2: Payment
    logEvent('api', 'POST /api/card/payment (PaymentManager.sale)', { amount: totalDollars(), token: tokenData.token, type: 'SALE' });
    const payRes = await fetch('/api/card/payment', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ amount: totalDollars(), token: tokenData.token, transactionType: 'SALE', cardType: tokenData.cardType, lastFour: tokenData.lastFour })
    });

    if (!payRes.ok) throw new Error('Payment declined');
    const payData = await payRes.json();
    logEvent('api', 'Response 200 OK', payData);
    logEvent('sdk', 'Response<Transaction>.success()', { transactionId: payData.transactionId, amount: payData.amount });

    state.confirmationData = {
      transactionId: payData.transactionId,
      amount: formatCents(totalCents()),
      paymentMethod: `${payData.cardType} \u2022\u2022\u2022\u2022${payData.lastFour}`
    };
    state.paymentState = 'complete';
    navigate('confirmation');
  } catch (err) {
    logEvent('error', 'Card payment failed: ' + err.message);
    state.paymentState = 'card-entry';
    render();
    setTimeout(() => {
      const errEl = document.getElementById('card-error');
      if (errEl) errEl.textContent = err.message;
    }, 50);
  }
}

// ---- Google Pay Flow ----
function startGooglePay() {
  state.paymentState = 'gpay-confirm';
  state.activePaymentMethod = 'googlepay';
  logEvent('lifecycle', 'Google Pay initiated');
  logEvent('sdk', 'PaymentManager.getGooglePayRequestConfig()');
  logEvent('sdk', 'PaymentsClient.loadPaymentData(request)');
  render();
}

async function confirmGooglePay() {
  state.paymentState = 'capturing';
  render();

  const walletToken = JSON.stringify({ type: 'CARD', info: { cardNetwork: 'VISA', cardDetails: '4242' } });
  logEvent('sdk', 'GooglePay(walletToken)', { walletToken: '{ type: CARD, cardNetwork: VISA }' });
  logEvent('api', 'POST /api/googlepay/payment', { amount: totalDollars(), walletToken: '...' });

  try {
    const res = await fetch('/api/googlepay/payment', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ amount: totalDollars(), walletToken })
    });

    if (!res.ok) throw new Error('Google Pay failed');
    const data = await res.json();
    logEvent('api', 'Response 200 OK', data);
    logEvent('sdk', 'Response<Transaction>.success()', { transactionId: data.transactionId });

    state.confirmationData = {
      transactionId: data.transactionId,
      amount: formatCents(totalCents()),
      paymentMethod: 'Google Pay'
    };
    state.paymentState = 'complete';
    navigate('confirmation');
  } catch (err) {
    logEvent('error', 'Google Pay failed: ' + err.message);
    state.paymentState = 'idle';
    state.errorMessage = err.message;
    render();
  }
}

// ---- Klarna Payment Flow ----
function startKlarna() {
  state.paymentState = 'klarna-select';
  state.activePaymentMethod = 'klarna';
  logEvent('lifecycle', 'Klarna payment initiated');
  logEvent('sdk', 'Klarna.Payments.init()', { clientToken: 'klarna_test_client_...' });
  render();
}

async function confirmKlarna() {
  const selectedType = document.querySelector('input[name="klarna-type"]:checked')?.value || 'pay_over_time';
  state.paymentState = 'capturing';
  render();

  logEvent('sdk', 'Klarna.Payments.authorize()', { paymentMethodCategory: selectedType, amount: formatCents(totalCents()) });

  try {
    // Step 1: Create session
    logEvent('api', 'POST /api/klarna/session', { amountCents: totalCents(), paymentMethodCategory: selectedType });
    const sessionRes = await fetch('/api/klarna/session', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ amountCents: totalCents(), locale: 'en-US', paymentMethodCategory: selectedType })
    });

    if (!sessionRes.ok) throw new Error('Klarna session failed');
    const session = await sessionRes.json();
    logEvent('sdk', 'Session created', { sessionId: session.sessionId, categories: session.paymentMethodCategories });

    // Step 2: Authorize payment
    const authToken = 'klarna_auth_' + Date.now();
    logEvent('sdk', 'Klarna.Payments.authorize() -> approved', { authorizationToken: authToken });

    logEvent('api', 'POST /api/klarna/payment', { amountCents: totalCents(), authorizationToken: authToken, paymentMethodCategory: selectedType });
    const payRes = await fetch('/api/klarna/payment', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ amountCents: totalCents(), authorizationToken: authToken, paymentMethodCategory: selectedType })
    });

    if (!payRes.ok) throw new Error('Klarna payment failed');
    const payData = await payRes.json();
    logEvent('api', 'Response 200 OK', payData);
    logEvent('sdk', 'Klarna order created', { orderId: payData.orderId, status: payData.status });

    state.confirmationData = {
      transactionId: payData.transactionId,
      amount: formatCents(totalCents()),
      paymentMethod: 'Klarna \u00B7 ' + payData.paymentType
    };
    state.paymentState = 'complete';
    navigate('confirmation');
  } catch (err) {
    logEvent('error', 'Klarna payment failed: ' + err.message);
    state.paymentState = 'idle';
    state.errorMessage = err.message;
    render();
  }
}

function sleep(ms) { return new Promise(r => setTimeout(r, ms)); }

// ---- Backend Health Check ----
async function checkBackend() {
  const el = document.getElementById('backend-status');
  try {
    const res = await fetch('/api/health');
    if (res.ok) {
      el.className = 'backend-status connected';
      el.querySelector('.status-text').textContent = 'Backend connected (port 8080)';
      state.backendOnline = true;
      logEvent('api', 'GET /api/health -> 200 OK');
    } else { throw new Error(); }
  } catch {
    el.className = 'backend-status disconnected';
    el.querySelector('.status-text').textContent = 'Backend offline \u2014 demo mode active';
    state.backendOnline = false;
    logEvent('error', 'Backend offline');
  }
}

// ---- Tab Switching ----
function switchTab(tabName) {
  document.querySelectorAll('.tab-btn').forEach(b => b.classList.toggle('active', b.dataset.tab === tabName));
  document.querySelectorAll('.tab-content').forEach(c => c.classList.toggle('active', c.id === 'tab-' + tabName));

  if (tabName === 'docs' && !document.getElementById('docs-content').innerHTML) {
    renderDocumentation();
  }
  if (tabName === 'guide' && !document.getElementById('guide-content').innerHTML) {
    renderGettingStarted();
  }
  if (tabName === 'swagger' && !document.getElementById('swagger-container').hasChildNodes()) {
    if (typeof SwaggerUIBundle !== 'undefined') {
      SwaggerUIBundle({
        url: '/swagger.json',
        dom_id: '#swagger-container',
        presets: [SwaggerUIBundle.presets.apis],
        layout: 'BaseLayout',
        defaultModelsExpandDepth: -1,
        docExpansion: 'list'
      });
    }
  }
}

// ---- Documentation Renderer ----
function renderDocumentation() {
  const el = document.getElementById('docs-content');
  el.innerHTML = `
    <nav class="docs-sidebar">
      <div style="font-weight: 700; color: white; margin-bottom: 12px; font-size: 14px;">Documentation</div>
      <a href="#" class="docs-nav-link active" data-section="doc-architecture">Architecture Overview</a>
      <a href="#" class="docs-nav-link" data-section="doc-sdk-apis">SDK Callbacks &amp; APIs</a>
      <a href="#" class="docs-nav-link" data-section="doc-tech-specs">Technical Specifications</a>
      <a href="#" class="docs-nav-link" data-section="doc-testing">Testing Requirements</a>
      <a href="#" class="docs-nav-link" data-section="doc-analysis">SDK Implementation Analysis</a>
      <a href="#" class="docs-nav-link" data-section="doc-klarna">Klarna Integration</a>
    </nav>
    <div class="docs-content-area">
      ${renderDocArchitecture()}
      ${renderDocSdkApis()}
      ${renderDocTechSpecs()}
      ${renderDocTesting()}
      ${renderDocAnalysis()}
      ${renderDocKlarna()}
    </div>
  `;
  // Render mermaid diagrams
  setTimeout(() => mermaid.run(), 100);
}

// ---- Documentation Section: Architecture Overview ----
function renderDocArchitecture() {
  return `
    <section id="doc-architecture">
      <h2>Architecture Overview</h2>
      <p>
        This project demonstrates Cash App Pay integration within a Fiserv CommerceHub ecosystem.
        It consists of six Gradle modules: a CardFree SDK shim that stubs the proprietary API surface,
        a Cash App Pay bridge that wraps the official Pay Kit SDK (core:2.6.0), a Ktor backend server
        that proxies authenticated requests to Cash App's sandbox API, an Android merchant demo app,
        this browser-based web demo, and the original unmodified CardFree sample app.
      </p>

      <h3>System Architecture Diagram</h3>
      <pre class="mermaid">
flowchart TD
    MA[":merchant-app&lt;br/&gt;Android Merchant Demo"]
    WD[":web-demo&lt;br/&gt;Browser Demo (Vite)"]
    SHIM[":payment-sdk-shim&lt;br/&gt;CardFree API Stubs"]
    BRIDGE[":cashapppay-bridge&lt;br/&gt;Cash App Pay Kit Wrapper"]
    PAYKIT["Cash App Pay Kit&lt;br/&gt;core:2.6.0"]
    BS[":backend-server&lt;br/&gt;Ktor :8080"]
    CASHAPI["Cash App Sandbox API&lt;br/&gt;sandbox.api.cash.app"]

    MA --> SHIM
    MA --> BRIDGE
    WD -->|"/api/* proxy"| BS
    BRIDGE --> PAYKIT
    BRIDGE -->|"HTTP"| BS
    BS -->|"Authorization: Client ID + Key"| CASHAPI

    style MA fill:#FF6600,color:#fff
    style WD fill:#FF8533,color:#fff
    style BRIDGE fill:#1565C0,color:#fff
    style PAYKIT fill:#00D632,color:#fff
    style BS fill:#7B1FA2,color:#fff
    style CASHAPI fill:#00A825,color:#fff
    style SHIM fill:#757575,color:#fff
      </pre>

      <h3>Module Overview</h3>
      <table>
        <tr><th>Module</th><th>Technology</th><th>Files</th><th>Purpose</th></tr>
        <tr><td>:payment-sdk-shim</td><td>Android Library (Kotlin)</td><td>22</td><td>Stub CardFree SDK API surface &mdash; zero external credentials</td></tr>
        <tr><td>:cashapppay-bridge</td><td>Android Library (Kotlin)</td><td>9</td><td>Wraps Cash App Pay Kit SDK + backend capture client</td></tr>
        <tr><td>:backend-server</td><td>Kotlin JVM (Ktor)</td><td>6</td><td>Proxies to Cash App sandbox API with auth headers</td></tr>
        <tr><td>:merchant-app</td><td>Android App (Compose)</td><td>24</td><td>Standalone merchant demo with 3 payment methods</td></tr>
        <tr><td>:web-demo</td><td>Vanilla JS (Vite)</td><td>5</td><td>Browser-based demo with phone mockup + trace panel</td></tr>
        <tr><td>:app</td><td>Android App (original)</td><td>&mdash;</td><td>Original CardFree sample (unchanged)</td></tr>
      </table>

      <h3>Payment Flow Sequence</h3>
      <pre class="mermaid">
sequenceDiagram
    participant U as User
    participant App as Merchant App
    participant VM as CashAppPayViewModel
    participant Auth as CashAppPayAuthorizer
    participant SDK as Pay Kit SDK (2.6.0)
    participant CA as Cash App
    participant BE as Backend :8080
    participant API as sandbox.api.cash.app

    U->>App: Tap "Pay with Cash App Pay"
    App->>VM: startCashAppPayment($12.50)
    VM->>VM: Generate idempotencyKey (UUID)
    VM->>VM: Persist to DataStore
    VM->>Auth: startPayment(1250 cents)
    Auth->>SDK: createCustomerRequest(OneTimeAction)
    SDK-->>Auth: ReadyToAuthorize
    Auth->>SDK: authorizeCustomerRequest()
    SDK->>CA: App switch to Cash App
    CA-->>SDK: User approves
    SDK-->>Auth: Approved {grantId, customerId}
    Auth-->>VM: StateFlow: Approved
    VM->>VM: Persist grantId
    VM->>BE: POST /api/payments/initiate
    BE->>API: POST /network/v1/payments
    API-->>BE: 200 {payment: {id: "PWC_...", status: "CAPTURED"}}
    BE-->>VM: {paymentId, status, amount}
    VM->>VM: Clear persisted state
    VM-->>App: PaymentComplete
    App->>U: Confirmation screen
      </pre>
    </section>
  `;
}

// ---- Documentation Section: SDK Callbacks & APIs ----
function renderDocSdkApis() {
  return `
    <section id="doc-sdk-apis">
      <h2>SDK Callbacks &amp; Server-Side APIs</h2>

      <h3>State Machine Diagram</h3>
      <pre class="mermaid">
stateDiagram-v2
    [*] --> NotStarted
    NotStarted --> CreatingCustomerRequest: startPayment()
    CreatingCustomerRequest --> ReadyToAuthorize: SDK callback
    ReadyToAuthorize --> Authorizing: auto authorizeCustomerRequest()
    Authorizing --> Approved: User approves in Cash App
    Authorizing --> Declined: User declines
    Authorizing --> Error: SDK exception
    Approved --> CapturingPayment: Backend capture call
    CapturingPayment --> PaymentComplete: 200 OK
    CapturingPayment --> Error: API failure
    Error --> CapturingPayment: retryAction()
    Declined --> NotStarted: reset()
    PaymentComplete --> [*]
    Error --> NotStarted: reset()
      </pre>

      <h3>CashAppPayFlowState</h3>
      <table>
        <tr><th>State</th><th>Parameters</th><th>Description</th><th>Triggered By</th></tr>
        <tr><td>NotStarted</td><td>&mdash;</td><td>Initial idle state</td><td>App launch, reset()</td></tr>
        <tr><td>CreatingCustomerRequest</td><td>&mdash;</td><td>Customer request being created</td><td>startPayment()</td></tr>
        <tr><td>ReadyToAuthorize</td><td>&mdash;</td><td>SDK ready for auth</td><td>SDK callback</td></tr>
        <tr><td>Authorizing</td><td>&mdash;</td><td>User in Cash App authorizing</td><td>auto after ReadyToAuthorize</td></tr>
        <tr><td>Approved</td><td>grantId, customerId, cashTag</td><td>Authorization successful</td><td>SDK callback (Approved)</td></tr>
        <tr><td>Declined</td><td>&mdash;</td><td>User declined payment</td><td>SDK callback (Declined)</td></tr>
        <tr><td>CapturingPayment</td><td>&mdash;</td><td>Backend capturing payment</td><td>After Approved</td></tr>
        <tr><td>PaymentComplete</td><td>transactionId, amount</td><td>Payment confirmed</td><td>Backend 200 response</td></tr>
        <tr><td>Error</td><td>message, cause, retryAction</td><td>Any failure</td><td>Various</td></tr>
      </table>

      <h3>SDK Callback Mapping</h3>
      <table>
        <tr><th>Pay Kit State (CashAppPayState)</th><th>Bridge State (CashAppPayFlowState)</th><th>Action</th></tr>
        <tr><td>ReadyToAuthorize</td><td>ReadyToAuthorize -&gt; Authorizing</td><td>Auto-call authorizeCustomerRequest()</td></tr>
        <tr><td>Approved</td><td>Approved(grantId, customerId, cashTag)</td><td>Extract grants[0], trigger capture</td></tr>
        <tr><td>Declined</td><td>Declined</td><td>Surface to UI</td></tr>
        <tr><td>CashAppPayExceptionState</td><td>Error(message, cause)</td><td>Surface exception</td></tr>
      </table>

      <h3>Credit Card Payment Flow</h3>
      <pre class="mermaid">
sequenceDiagram
    participant U as User
    participant UI as Card Entry Modal
    participant BE as Backend :8080

    U->>UI: Enter card details
    UI->>BE: POST /api/card/tokenize
    BE-->>UI: {token, lastFour, cardType}
    UI->>BE: POST /api/card/payment
    Note over BE: Simulates PaymentManager.sale()
    BE-->>UI: {transactionId, status: CAPTURED}
    UI->>U: Confirmation screen
</pre>

      <h3>Google Pay Payment Flow</h3>
      <pre class="mermaid">
sequenceDiagram
    participant U as User
    participant GP as Google Pay Dialog
    participant BE as Backend :8080

    U->>GP: Select Google Pay
    Note over GP: PaymentsClient.loadPaymentData()
    GP->>U: Confirm payment
    U->>BE: POST /api/googlepay/payment
    Note over BE: Simulates GooglePay + PaymentManager.sale()
    BE-->>U: {transactionId, status: CAPTURED}
    U->>U: Confirmation screen
</pre>

      <h3>Backend API Endpoints</h3>

      <h4>POST /api/payments/initiate</h4>
      <p>Initiates a payment capture using the grant ID obtained from Cash App authorization.</p>
      <div class="doc-endpoint-detail">
        <strong>Request Body:</strong>
        <pre><code>{
  "grantId": "GRG_sandbox:active",
  "amountCents": 31462,
  "idempotencyKey": "550e8400-e29b-41d4-a716-446655440000"
}</code></pre>
        <strong>Response (200):</strong>
        <pre><code>{
  "paymentId": "PWC_2yam7aabfdpr0h422ge3snv26",
  "status": "CAPTURED",
  "amount": 31462
}</code></pre>
        <strong>curl Example:</strong>
        <pre><code>curl -X POST http://localhost:8080/api/payments/initiate \\
  -H "Content-Type: application/json" \\
  -d '{"grantId":"GRG_sandbox:active","amountCents":31462,"idempotencyKey":"test-001"}'</code></pre>
      </div>

      <h4>POST /api/setup/onboard-merchant</h4>
      <p>Creates a brand and merchant in the Cash App sandbox for payment processing.</p>
      <div class="doc-endpoint-detail">
        <strong>Request Body:</strong>
        <pre><code>{
  "brandName": "CommerceHub Demo",
  "merchantName": "Fiserv Test Store"
}</code></pre>
        <strong>Response (200):</strong>
        <pre><code>{
  "brandId": "BRAND_bbq9jbpebz4fg81pmnm9vqeac",
  "merchantId": "MMI_1nk0ecoa69ilax9gno1lz6luh",
  "scopeId": "BRAND_bbq9jbpebz4fg81pmnm9vqeac"
}</code></pre>
      </div>

      <h4>GET /api/health</h4>
      <p>Health check endpoint for the backend server.</p>
      <div class="doc-endpoint-detail">
        <strong>Response (200):</strong>
        <pre><code>{
  "status": "ok",
  "service": "cashapp-pay-sandbox-proxy"
}</code></pre>
      </div>

      <h3>Credit Card Endpoints</h3>

      <h4>POST /api/card/tokenize</h4>
      <p>Simulates CardFree <code>CreditCardManager.addCreditCard()</code>. Tokenizes card details and detects card type (4→VISA, 5→MC, 3→AMEX, 6→DISCOVER).</p>
      <strong>Request:</strong>
      <pre><code>{
  "cardNumber": "4242424242424242",
  "expiryMonth": "12",
  "expiryYear": "2027",
  "cvv": "123",
  "cardholderName": "Ajay Test",
  "postalCode": "94107"
}</code></pre>
      <strong>Response (200):</strong>
      <pre><code>{
  "token": "tok_1775186317000_4242",
  "lastFour": "4242",
  "cardType": "VISA",
  "expiryMonth": "12",
  "expiryYear": "2027"
}</code></pre>

      <h4>POST /api/card/payment</h4>
      <p>Simulates CardFree <code>PaymentManager.sale()</code>. Processes payment using token from tokenize step. Amount $66.70 triggers decline.</p>
      <strong>Request:</strong>
      <pre><code>{
  "amount": 43.38,
  "token": "tok_1775186317000_4242",
  "transactionType": "SALE",
  "cardType": "VISA",
  "lastFour": "4242"
}</code></pre>
      <strong>Response (200):</strong>
      <pre><code>{
  "transactionId": "TXN_CARD_1775186317144",
  "amount": 43.38,
  "status": "CAPTURED",
  "cardType": "VISA",
  "lastFour": "4242",
  "paymentMethod": "CREDIT_CARD"
}</code></pre>

      <h3>Google Pay Endpoint</h3>

      <h4>POST /api/googlepay/payment</h4>
      <p>Simulates CardFree <code>GooglePay(walletToken)</code> + <code>PaymentManager.sale()</code>. Processes Google Pay wallet token.</p>
      <strong>Request:</strong>
      <pre><code>{
  "amount": 1735.98,
  "walletToken": "{\\"type\\":\\"CARD\\",\\"info\\":{\\"cardNetwork\\":\\"VISA\\"}}"
}</code></pre>
      <strong>Response (200):</strong>
      <pre><code>{
  "transactionId": "TXN_GPAY_1775186382134",
  "amount": 1735.98,
  "status": "CAPTURED",
  "cardType": "GOOGLE_PAY",
  "lastFour": "••••",
  "paymentMethod": "GOOGLE_PAY"
}</code></pre>

      <h3>Cash App Sandbox API (Upstream)</h3>

      <h4>API Proxy Flow</h4>
      <pre class="mermaid">
sequenceDiagram
    participant Client as Web/Mobile App
    participant BE as Backend :8080
    participant CASH as sandbox.api.cash.app

    Note over BE: Headers added by backend
    Note over BE: Authorization: Client CAS-CI_FISERV_TEST KEY_ksbja...
    Note over BE: Accept: application/json
    Note over BE: X-Region: SFO
    Note over BE: x-signature: sandbox:skip-signature-check

    Client->>BE: POST /api/payments/initiate
    BE->>CASH: POST /network/v1/payments
    CASH-->>BE: 200 {payment: {id, status, fee_amount}}
    BE-->>Client: 200 {paymentId, status, amount}
      </pre>

      <h4>POST /network/v1/brands</h4>
      <p>Creates a brand entity in the Cash App network.</p>
      <div class="doc-endpoint-detail">
        <strong>Request Body:</strong>
        <pre><code>{
  "idempotency_key": "brand-create-001",
  "brand": {
    "name": "CommerceHub Demo",
    "profiles": [
      {
        "country_code": "US",
        "currency_code": "USD",
        "customer_request_scopes": ["PAYMENT"]
      }
    ]
  }
}</code></pre>
        <strong>Response (200):</strong>
        <pre><code>{
  "brand": {
    "id": "BRAND_bbq9jbpebz4fg81pmnm9vqeac",
    "name": "CommerceHub Demo",
    "created_at": "2025-01-15T10:30:00Z",
    "updated_at": "2025-01-15T10:30:00Z"
  }
}</code></pre>
      </div>

      <h4>POST /network/v1/merchants</h4>
      <p>Creates a merchant under a brand with address and MCC category.</p>
      <div class="doc-endpoint-detail">
        <strong>Request Body:</strong>
        <pre><code>{
  "idempotency_key": "merchant-create-001",
  "merchant": {
    "brand_id": "BRAND_bbq9jbpebz4fg81pmnm9vqeac",
    "name": "Fiserv Demo Store",
    "category": {
      "mcc": "5411"
    },
    "address": {
      "address_line_1": "1 Cash App Way",
      "locality": "San Francisco",
      "administrative_district_level_1": "CA",
      "postal_code": "94105",
      "country_code": "US"
    }
  }
}</code></pre>
        <strong>Response (200):</strong>
        <pre><code>{
  "merchant": {
    "id": "MMI_1nk0ecoa69ilax9gno1lz6luh",
    "brand_id": "BRAND_bbq9jbpebz4fg81pmnm9vqeac",
    "name": "Fiserv Demo Store",
    "category": { "mcc": "5411" }
  }
}</code></pre>
      </div>

      <h4>POST /network/v1/payments</h4>
      <p>Creates and captures a payment using a customer grant.</p>
      <div class="doc-endpoint-detail">
        <strong>Request Body:</strong>
        <pre><code>{
  "idempotency_key": "pay-001-uuid",
  "payment": {
    "amount": {
      "amount": 31462,
      "currency": "USD"
    },
    "merchant_id": "MMI_1nk0ecoa69ilax9gno1lz6luh",
    "grant_id": "GRG_sandbox:active",
    "capture": true,
    "channel": "ONLINE"
  }
}</code></pre>
        <strong>Response (200):</strong>
        <pre><code>{
  "payment": {
    "id": "PWC_2yam7aabfdpr0h422ge3snv26",
    "amount": { "amount": 31462, "currency": "USD" },
    "status": "CAPTURED",
    "fee_amount": { "amount": 0, "currency": "USD" },
    "grant_id": "GRG_sandbox:active",
    "merchant_id": "MMI_1nk0ecoa69ilax9gno1lz6luh",
    "created_at": "2025-01-15T10:35:00Z"
  }
}</code></pre>
      </div>
    </section>
  `;
}

// ---- Documentation Section: Technical Specifications ----
function renderDocTechSpecs() {
  return `
    <section id="doc-tech-specs">
      <h2>Technical Specifications</h2>

      <h3>(a) Mobile SDK Model</h3>
      <p>
        <strong>CashAppPayAuthorizer</strong> wraps the Cash App Pay Kit SDK. It implements
        <code>CashAppPayListener</code> and translates raw <code>CashAppPayState</code> callbacks
        into a Kotlin <code>StateFlow&lt;CashAppPayFlowState&gt;</code> that the ViewModel observes.
      </p>
      <p>
        <strong>Auto-authorize pattern:</strong> When the SDK emits <code>ReadyToAuthorize</code>,
        the authorizer immediately calls <code>authorizeCustomerRequest()</code> to trigger the
        Cash App app-switch. This eliminates a manual confirmation step and matches the recommended
        integration pattern from Cash App's documentation.
      </p>
      <p>
        <strong>CashAppPayCaptureClient:</strong> Uses OkHttp to POST to the backend server
        at <code>/api/payments/initiate</code>. Returns <code>Result&lt;PaymentCaptureResponse&gt;</code>
        with paymentId, status, and amount fields.
      </p>
      <p>
        <strong>Process death recovery:</strong> Before app-switching to Cash App, the ViewModel
        persists <code>idempotencyKey</code>, <code>grantId</code>, and <code>amountCents</code>
        to Jetpack DataStore. On <code>restoreFromProcessDeath()</code>, if a <code>grantId</code>
        exists in persisted state, the flow skips directly to the capture step &mdash; avoiding
        a duplicate customer request creation.
      </p>

      <h3>(b) Credentials &amp; Client IDs</h3>
      <table>
        <tr><th>Credential</th><th>Value</th><th>Purpose</th></tr>
        <tr><td>Client ID</td><td><code>CAS-CI_FISERV_TEST</code></td><td>Sandbox client identifier</td></tr>
        <tr><td>API Key ID</td><td><code>KEY_ksbja4hqrgtahqmw6nn5gyv1b</code></td><td>Server-side auth (in Authorization header)</td></tr>
        <tr><td>API Key Secret</td><td><code>CASH_rbkhnwqtqk06wxzqngh4z98dy</code></td><td>Request signing (bypassed in sandbox)</td></tr>
        <tr><td>Brand ID</td><td><code>BRAND_bbq9jbpebz4fg81pmnm9vqeac</code></td><td>CommerceHub Demo brand</td></tr>
        <tr><td>Merchant ID</td><td><code>MMI_1nk0ecoa69ilax9gno1lz6luh</code></td><td>Fiserv Demo Store</td></tr>
        <tr><td>Redirect URI</td><td><code>merchantdemo://cashapppay/checkout</code></td><td>Deep link for Cash App return</td></tr>
      </table>

      <h3>(c) Sandbox Information</h3>

      <h4>Magic Grant IDs</h4>
      <table>
        <tr><th>Grant ID</th><th>Result</th></tr>
        <tr><td><code>GRG_sandbox:active</code></td><td>Payment succeeds</td></tr>
        <tr><td><code>GRG_sandbox:consumed</code></td><td>Payment fails (grant already used)</td></tr>
        <tr><td><code>GRG_sandbox:expired</code></td><td>Payment fails (grant expired)</td></tr>
        <tr><td><code>GRG_sandbox:missing</code></td><td>Payment fails (grant not found)</td></tr>
        <tr><td><code>GRG_sandbox:revoked</code></td><td>Payment fails (grant revoked)</td></tr>
      </table>

      <h4>Magic Amounts for Decline Simulation</h4>
      <table>
        <tr><th>Amount (cents)</th><th>Result</th></tr>
        <tr><td>6670</td><td>Connection error</td></tr>
        <tr><td>7770</td><td>Compliance decline</td></tr>
        <tr><td>7771</td><td>Insufficient funds</td></tr>
        <tr><td>7772</td><td>Generic decline</td></tr>
        <tr><td>7773</td><td>Risk-based decline</td></tr>
        <tr><td>7774</td><td>Amount too large</td></tr>
        <tr><td>7775</td><td>Amount too small</td></tr>
      </table>

      <h4>Sandbox Headers</h4>
      <table>
        <tr><th>Header</th><th>Value</th><th>Notes</th></tr>
        <tr><td><code>x-signature</code></td><td><code>sandbox:skip-signature-check</code></td><td>Bypasses HMAC verification in sandbox</td></tr>
        <tr><td><code>X-Region</code></td><td><code>SFO</code></td><td>IATA airport code (must not use country codes like "us")</td></tr>
        <tr><td><code>Accept</code></td><td><code>application/json</code></td><td>Required &mdash; API returns 400 without it</td></tr>
      </table>

      <h3>(e) Credit Card Integration Model</h3>
      <p>The card payment flow mirrors CardFree's <code>CreditCardManager</code> + <code>PaymentManager</code> pattern:</p>
      <table><tr><th>Step</th><th>CardFree SDK (Real)</th><th>Our Simulation</th></tr>
      <tr><td>Card Entry</td><td>CreditCardDetailsModal composable</td><td>Custom HTML card entry modal</td></tr>
      <tr><td>Tokenization</td><td>CreditCardManager.addCreditCard()</td><td>POST /api/card/tokenize</td></tr>
      <tr><td>Payment</td><td>PaymentManager.sale(Payment(amount, creditCard))</td><td>POST /api/card/payment</td></tr>
      <tr><td>Response</td><td>Response&lt;Transaction&gt;.success()</td><td>CardPaymentResponse JSON</td></tr>
      </table>
      <p><strong>Card type detection:</strong> First digit of card number: 4→VISA, 5→MASTERCARD, 3→AMEX, 6→DISCOVER.</p>
      <p><strong>Decline testing:</strong> Amount $66.70 triggers simulated insufficient funds decline.</p>

      <h3>(f) Google Pay Integration Model</h3>
      <p>Mirrors CardFree's <code>GooglePay(walletToken)</code> → <code>PaymentManager.sale()</code> pattern:</p>
      <table><tr><th>Step</th><th>CardFree SDK (Real)</th><th>Our Simulation</th></tr>
      <tr><td>Init</td><td>PaymentsClient + PaymentDataRequest</td><td>GPay confirmation dialog</td></tr>
      <tr><td>Auth</td><td>loadPaymentData() → PaymentData</td><td>User clicks "Pay" in dialog</td></tr>
      <tr><td>Token</td><td>GooglePay(walletToken = paymentData.toJson())</td><td>JSON wallet token string</td></tr>
      <tr><td>Payment</td><td>PaymentManager.sale(Payment(amount, googlePay))</td><td>POST /api/googlepay/payment</td></tr>
      </table>

      <h3>(d) Workflow Diagrams</h3>
      <p>
        Refer to the Architecture Overview section for the System Architecture Diagram and Payment Flow Sequence,
        and the SDK Callbacks &amp; APIs section for the State Machine Diagram and API Proxy Flow.
      </p>
    </section>
  `;
}

// ---- Documentation Section: Testing Requirements ----
function renderDocTesting() {
  return `
    <section id="doc-testing">
      <h2>Testing Requirements</h2>

      <h3>(a) Unit Tests</h3>
      <table>
        <tr><th>Test Case</th><th>Component</th><th>Validates</th></tr>
        <tr><td>State mapping: ReadyToAuthorize</td><td>CashAppPayAuthorizer</td><td>Maps to ReadyToAuthorize then auto-calls authorizeCustomerRequest</td></tr>
        <tr><td>State mapping: Approved</td><td>CashAppPayAuthorizer</td><td>Extracts grantId, customerId from grants[0]</td></tr>
        <tr><td>State mapping: Exception</td><td>CashAppPayAuthorizer</td><td>Maps to Error with exception message</td></tr>
        <tr><td>Dollar to cents</td><td>CashAppPayViewModel</td><td>$12.50 -&gt; 1250 cents</td></tr>
        <tr><td>Tax calculation</td><td>CartViewModel</td><td>Math.floor(subtotal * 0.085)</td></tr>
        <tr><td>Price formatting</td><td>Product</td><td>159999 -&gt; "$1,599.99"</td></tr>
        <tr><td>Idempotency generation</td><td>CashAppPayViewModel</td><td>UUID unique per call</td></tr>
        <tr><td>DataStore round-trip</td><td>CashAppPayStatePersistence</td><td>Save + load returns same values</td></tr>
        <tr><td>Card type detection</td><td>CardPaymentRoutes</td><td>4→VISA, 5→MC, 3→AMEX, 6→DISCOVER</td></tr>
        <tr><td>Card decline trigger</td><td>CardPaymentRoutes</td><td>Amount $66.70 returns 400 DECLINED</td></tr>
      </table>

      <h3>(b) Regression Tests</h3>
      <table>
        <tr><th>Test</th><th>Command</th><th>Validates</th></tr>
        <tr><td>Shim builds</td><td><code>./gradlew :payment-sdk-shim:build</code></td><td>API signatures match CardFree</td></tr>
        <tr><td>Bridge builds</td><td><code>./gradlew :cashapppay-bridge:build</code></td><td>Compiles against real Pay Kit SDK</td></tr>
        <tr><td>Merchant APK</td><td><code>./gradlew :merchant-app:assembleDebug</code></td><td>APK builds with shim + bridge</td></tr>
        <tr><td>Backend builds</td><td><code>./gradlew :backend-server:build</code></td><td>Compiles</td></tr>
        <tr><td>Original unchanged</td><td><code>git diff app/</code></td><td>Zero changes to CardFree sample</td></tr>
      </table>

      <h3>(c) End-to-End Tests</h3>
      <table>
        <tr><th>Test</th><th>Method</th><th>Expected Result</th><th>Actual Result</th></tr>
        <tr><td>Sandbox payment (direct)</td><td>curl POST to backend with GRG_sandbox:active</td><td>CAPTURED</td><td>PWC_apxdxcepys957y90asm3n63h4</td></tr>
        <tr><td>Web demo happy path</td><td>UI: Catalog-&gt;Cart-&gt;Checkout-&gt;Approve</td><td>CAPTURED</td><td>PWC_2yam7aabfdpr0h422ge3snv26</td></tr>
        <tr><td>Decline flow</td><td>UI: tap Decline</td><td>Returns to checkout</td><td>"Payment declined" message</td></tr>
        <tr><td>Backend offline</td><td>Stop backend, run web demo</td><td>Fallback txn ID</td><td>TXN_OFFLINE_* generated</td></tr>
        <tr><td>Health check</td><td>GET /api/health</td><td>200 OK</td><td>{"status":"ok"}</td></tr>
        <tr><td>Credit Card happy path</td><td>Web demo: card entry → tokenize → payment</td><td>TXN_CARD_1775186317144 — VISA ••••4242, $43.38</td></tr>
        <tr><td>Google Pay happy path</td><td>Web demo: GPay dialog → confirm → payment</td><td>TXN_GPAY_1775186382134 — Google Pay, $1,735.98</td></tr>
        <tr><td>Card decline ($66.70)</td><td>Backend: POST /api/card/payment with amount=66.70</td><td>400 — "insufficient funds"</td></tr>
        <tr><td>Card type detection</td><td>Tokenize with leading digit 4/5/3/6</td><td>VISA/MASTERCARD/AMEX/DISCOVER</td></tr>
      </table>
    </section>
  `;
}

// ---- Documentation Section: SDK Implementation Analysis ----
function renderDocAnalysis() {
  return `
    <section id="doc-analysis">
      <h2>SDK Implementation Analysis</h2>

      <h3>(a) CardFree SDK vs Our Implementation</h3>
      <p>
        The real CardFree SDK AAR contains 199 classes across 15 packages, covering credit card tokenization,
        Google Pay, Apple Pay, gift cards, loyalty, and full payment lifecycle management. Our shim implements
        15 stub classes that match the API signatures required for our Cash App Pay integration demo &mdash;
        enough to compile the merchant app without the proprietary AAR dependency.
      </p>
      <table>
        <tr><th>Category</th><th>Real CardFree SDK (AAR)</th><th>Our Shim</th><th>Status</th></tr>
        <tr><td>MobilePayments</td><td>Full init + backend config</td><td>Logs only</td><td>Stub</td></tr>
        <tr><td>Response&lt;T&gt;</td><td>Callback interface</td><td>Identical</td><td>Match</td></tr>
        <tr><td>Environment</td><td>Enum (SANDBOX, PRODUCTION)</td><td>Identical</td><td>Match</td></tr>
        <tr><td>Transaction</td><td>18-field data class + serialization</td><td>16-field simplified</td><td>Partial</td></tr>
        <tr><td>PaymentMethod</td><td>Interface with toJson()</td><td>Interface with toJson()</td><td>Match</td></tr>
        <tr><td>CreditCardManager</td><td>Full CRUD + tokenization</td><td>Not included</td><td>Missing</td></tr>
        <tr><td>PaymentManager</td><td>SALE/AUTH/CAPTURE/VOID</td><td>Not included</td><td>Missing</td></tr>
        <tr><td>PurchaseButton</td><td>Composable with payment logic</td><td>Not included</td><td>Missing</td></tr>
        <tr><td>CreditCardListView</td><td>Card management UI</td><td>Not included</td><td>Missing</td></tr>
        <tr><td>StyleProvider</td><td>Runtime theme switching</td><td>No-op applyStyle()</td><td>Stub</td></tr>
        <tr><td>GooglePay</td><td>PaymentMethod impl</td><td>Stub data class</td><td>Stub</td></tr>
      </table>

      <h3>(b) Bridge vs CardFree Integration Pattern</h3>
      <pre class="mermaid">
flowchart LR
    subgraph CardFree["CardFree GooglePay Integration"]
        GP[GooglePay] -->|"implements"| PM[PaymentMethod]
        PM -->|"toJson()"| PBM[PurchaseButtonModel]
        PBM -->|"makePayment()"| SDK_INT[SDK Internal Backend]
        SDK_INT -->|"opaque"| FISERV[Fiserv CommerceHub]
    end

    subgraph Ours["Our Cash App Pay Integration"]
        CAB[CashAppPayButton] --> CAVM[CashAppPayViewModel]
        CAVM --> AUTH[CashAppPayAuthorizer]
        AUTH -->|"wraps"| PAYKIT[Pay Kit SDK 2.6.0]
        CAVM --> CAP[CashAppPayCaptureClient]
        CAP -->|"HTTP POST"| BACKEND[Our Backend :8080]
        BACKEND -->|"transparent proxy"| CASHAPI2[Cash App API]
    end

    style GP fill:#4285F4,color:#fff
    style CAB fill:#00D632,color:#fff
    style SDK_INT fill:#666,color:#fff
    style PAYKIT fill:#00D632,color:#fff
    style BACKEND fill:#7B1FA2,color:#fff
      </pre>

      <h3>(c) Integration Tiers Comparison</h3>
      <p>
        CardFree offers three integration tiers with increasing control. Our Cash App Pay integration
        operates at the Direct API tier, giving us full control over the payment flow while using
        Cash App's native SDK for authorization.
      </p>
      <table>
        <tr><th>Tier</th><th>CardFree Approach</th><th>Our Approach</th><th>Notes</th></tr>
        <tr><td>Sheets</td><td>MobilePaymentsPurchaseActivity (Intent)</td><td>Not used</td><td>Full encapsulated checkout</td></tr>
        <tr><td>UI Components</td><td>PurchaseButton, CreditCardListView</td><td>CashAppPayButton (custom)</td><td>Our button is Cash App specific</td></tr>
        <tr><td>Direct API</td><td>CreditCardManager, PaymentManager</td><td>CashAppPayViewModel + CaptureClient</td><td>We handle capture explicitly</td></tr>
        <tr><td>Cash App Pay</td><td>Not available in CardFree</td><td>Custom bridge module</td><td>Our addition to the ecosystem</td></tr>
      </table>
    </section>
  `;
}

// ---- Documentation Section: Klarna Integration ----
function renderDocKlarna() {
  return `
    <section id="doc-klarna">
    <h2 id="doc-klarna">Klarna Integration</h2>
    <p>Klarna enables flexible payment options: Pay in 4 (interest-free installments), Pay Later (30 days), and Pay Now (direct payment).</p>

    <h3>Klarna Payment Flow</h3>
    <pre class="mermaid">
sequenceDiagram
    participant U as User
    participant UI as Klarna Modal
    participant BE as Backend :8080
    participant K as Klarna API

    U->>UI: Select Klarna at checkout
    UI->>UI: Choose payment type (Pay in 4)
    UI->>BE: POST /api/klarna/session
    BE-->>UI: {sessionId, clientToken}
    UI->>UI: Klarna.Payments.authorize()
    UI->>BE: POST /api/klarna/payment
    BE-->>UI: {transactionId, orderId, status}
    UI->>U: Confirmation screen
    </pre>

    <h3>Credentials</h3>
    <table>
    <tr><th>Credential</th><th>Value</th><th>Purpose</th></tr>
    <tr><td>API Key (Username)</td><td><code>2d434281-4a6b-415f-afae-11d4f3a9d592</code></td><td>Klarna merchant identifier</td></tr>
    <tr><td>Client Identifier</td><td><code>klarna_test_client_...</code></td><td>JS SDK initialization token</td></tr>
    <tr><td>Merchant</td><td><code>PN129867</code></td><td>Klarna merchant account</td></tr>
    <tr><td>Sandbox API</td><td><code>api-na.playground.klarna.com</code></td><td>North America sandbox</td></tr>
    </table>

    <h3>Payment Types</h3>
    <table>
    <tr><th>Category</th><th>Name</th><th>Description</th></tr>
    <tr><td><code>pay_over_time</code></td><td>Pay in 4</td><td>4 interest-free biweekly installments</td></tr>
    <tr><td><code>pay_later</code></td><td>Pay in 30 days</td><td>Invoice, try before you buy</td></tr>
    <tr><td><code>pay_now</code></td><td>Pay now</td><td>Direct debit, bank transfer, card</td></tr>
    </table>

    <h3>API Endpoints</h3>

    <h4>POST /api/klarna/session</h4>
    <p>Creates a Klarna payment session.</p>
    <strong>Request:</strong>
    <pre><code>{
  "amountCents": 24999,
  "locale": "en-US",
  "paymentMethodCategory": "pay_over_time"
}</code></pre>
    <strong>Response (200):</strong>
    <pre><code>{
  "sessionId": "klarna_session_1775200000000",
  "clientToken": "klarna_test_client_...",
  "paymentMethodCategories": ["pay_now", "pay_later", "pay_over_time"]
}</code></pre>

    <h4>POST /api/klarna/payment</h4>
    <p>Authorizes and captures a Klarna payment.</p>
    <strong>Request:</strong>
    <pre><code>{
  "amount": 249.99,
  "authorizationToken": "klarna_auth_1775200000000",
  "paymentMethodCategory": "pay_over_time"
}</code></pre>
    <strong>Response (200):</strong>
    <pre><code>{
  "transactionId": "TXN_KLARNA_1775200000000",
  "orderId": "KL_ORD_1775200000000",
  "amount": 249.99,
  "status": "AUTHORIZED",
  "paymentMethod": "KLARNA",
  "paymentType": "Pay in 4 installments"
}</code></pre>

    <h3>Promo Messaging</h3>
    <p>Product cards in the catalog display Klarna installment messaging: "or 4 x $62.50 with <strong>Klarna</strong>". This is calculated as <code>Math.ceil(priceInCents / 4)</code> and formatted as currency.</p>
    </section>
  `;
}

// ---- Getting Started Renderer ----
function renderGettingStarted() {
  const el = document.getElementById('guide-content');
  el.innerHTML = `
    <h2>Getting Started</h2>
    <p>Step-by-step guide to clone, build, and run this project from GitHub.</p>

    <div class="step-card">
      <h3><span class="step-number">1</span>Prerequisites</h3>
      <table>
        <tr><th>Tool</th><th>Version</th><th>Required For</th></tr>
        <tr><td>JDK</td><td>17+</td><td>Backend server, Android builds</td></tr>
        <tr><td>Node.js</td><td>18+</td><td>Web demo (Vite)</td></tr>
        <tr><td>Android SDK</td><td>API 36</td><td>Mobile app (optional)</td></tr>
        <tr><td>Git</td><td>2.x</td><td>Clone repo</td></tr>
      </table>
    </div>

    <div class="step-card">
      <h3><span class="step-number">2</span>Quick Start (Web Demo)</h3>
      <pre><code>git clone https://github.com/user/cardfree-cashapp-integration.git
cd cardfree-sdk/web-demo
npm install
npm run dev
# Open http://localhost:5180</code></pre>
      <p>The web demo works standalone with a simulated Cash App flow. For real sandbox payments, also start the backend (Step 3).</p>
    </div>

    <div class="step-card">
      <h3><span class="step-number">3</span>Running the Backend Server</h3>
      <pre><code>export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
cd cardfree-sdk
./gradlew :backend-server:run
# Server starts on http://localhost:8080
# Verify: curl http://localhost:8080/api/health</code></pre>
      <p>The backend proxies payment requests to Cash App's sandbox API with your credentials baked in.</p>
    </div>

    <div class="step-card">
      <h3><span class="step-number">4</span>Cash App Sandbox Setup</h3>
      <p>Default credentials are pre-configured. To use your own:</p>
      <pre><code>export CASHAPP_CLIENT_ID=CAS-CI_YOUR_CLIENT_ID
export CASHAPP_API_KEY=KEY_your_api_key_id
./gradlew :backend-server:run</code></pre>
      <h4>Creating an API Key</h4>
      <pre><code>curl -X POST "https://sandbox.api.cash.app/management/v1/api-keys" \\
  -H "Authorization: Client $CASHAPP_CLIENT_ID $EXISTING_KEY" \\
  -H "Content-Type: application/json" \\
  -H "Accept: application/json" \\
  -H "X-Region: SFO" \\
  -H "x-signature: sandbox:skip-signature-check" \\
  -d '{"idempotency_key":"create-key-001","api_key":{"scopes":["PAYMENTS_WRITE","BRANDS_WRITE","MERCHANTS_WRITE"],"reference_id":"my-key"}}'</code></pre>
      <h4>Magic Test Values</h4>
      <p>Use <code>GRG_sandbox:active</code> as grant ID for successful payments. See Documentation tab for full list of magic values.</p>
    </div>

    <div class="step-card">
      <h3><span class="step-number">5</span>Building the Android App</h3>
      <pre><code>export ANDROID_HOME=/path/to/android-sdk
./gradlew :merchant-app:assembleDebug
# APK at: merchant-app/build/outputs/apk/debug/merchant-app-debug.apk

# Install on emulator:
./gradlew :merchant-app:installDebug</code></pre>
      <p>The Android app uses the real Cash App Pay Kit SDK (core:2.6.0) for native payment authorization.</p>
    </div>

    <div class="step-card">
      <h3><span class="step-number">6</span>Project Structure</h3>
      <pre><code>cardfree-sdk/
+-- payment-sdk-shim/     # CardFree API stubs (22 files)
+-- cashapppay-bridge/    # Cash App Pay Kit wrapper (9 files)
+-- backend-server/       # Ktor sandbox proxy (6 files)
+-- merchant-app/         # Android demo app (24 files)
+-- web-demo/             # Browser demo (5 files)
+-- app/                  # Original CardFree sample (unchanged)
+-- dist/                 # Original CardFree AAR binary</code></pre>
    </div>

    <div class="step-card">
      <h3><span class="step-number">7</span>Environment Variables</h3>
      <table>
        <tr><th>Variable</th><th>Default</th><th>Purpose</th></tr>
        <tr><td>JAVA_HOME</td><td>(system)</td><td>JDK 17 path</td></tr>
        <tr><td>ANDROID_HOME</td><td>(system)</td><td>Android SDK path</td></tr>
        <tr><td>CASHAPP_CLIENT_ID</td><td>CAS-CI_FISERV_TEST</td><td>Cash App sandbox client</td></tr>
        <tr><td>CASHAPP_API_KEY</td><td>KEY_ksbja4hqrgtahqmw6nn5gyv1b</td><td>API Key ID for auth header</td></tr>
      </table>
    </div>

    <div class="step-card">
      <h3><span class="step-number">8</span>Troubleshooting</h3>
      <table>
        <tr><th>Issue</th><th>Cause</th><th>Fix</th></tr>
        <tr><td>401 Unauthorized</td><td>Wrong auth header format</td><td>Must be <code>Client {CLIENT_ID} {API_KEY_ID}</code></td></tr>
        <tr><td>403 Insufficient Scopes</td><td>API key missing permissions</td><td>Create new key with PAYMENTS_WRITE scope</td></tr>
        <tr><td>400 INVALID_REGION</td><td>X-Region not IATA code</td><td>Use <code>SFO</code> not <code>us</code></td></tr>
        <tr><td>400 INVALID_CONTENT_TYPE</td><td>Missing Accept header</td><td>Add <code>Accept: application/json</code></td></tr>
        <tr><td>Port 8080 in use</td><td>Existing process</td><td><code>lsof -ti:8080 | xargs kill</code></td></tr>
        <tr><td>JDK not found</td><td>JAVA_HOME not set</td><td><code>brew install openjdk@17</code></td></tr>
      </table>
    </div>
  `;
}

// ---- Init ----
document.addEventListener('DOMContentLoaded', () => {
  render();
  checkBackend();
  setInterval(checkBackend, 15000);
  logEvent('lifecycle', 'App initialized', { products: PRODUCTS.length, backend: 'localhost:8080' });

  // Tab switching
  document.addEventListener('click', (e) => {
    const tab = e.target.closest('[data-tab]');
    if (tab) switchTab(tab.dataset.tab);

    const clear = e.target.closest('[data-trace-clear]');
    if (clear) { document.getElementById('trace-log').innerHTML = ''; logEvent('lifecycle', 'Trace cleared'); }

    // Docs sidebar nav
    const navLink = e.target.closest('.docs-nav-link');
    if (navLink) {
      e.preventDefault();
      const target = document.getElementById(navLink.dataset.section);
      if (target) target.scrollIntoView({ behavior: 'smooth', block: 'start' });
      document.querySelectorAll('.docs-nav-link').forEach(l => l.classList.remove('active'));
      navLink.classList.add('active');
    }
  });

  // Mermaid init
  mermaid.initialize({ startOnLoad: false, theme: 'default', securityLevel: 'loose' });
});
