import React, { useState, useEffect } from 'react'
import axios from 'axios'
import {
  Header,
  HeaderName,
  HeaderGlobalBar,
  HeaderGlobalAction,
  Button,
  TextInput,
  Grid,
  Column,
  Tile,
  Form,
  FormGroup,
  TextArea,
  InlineNotification,
  Loading,
  ContainedList,
  ContainedListItem
} from '@carbon/react'
import {
  ShoppingCart,
  User,
  Logout,
  Add,
  ShoppingBag,
  TrashCan,
  Subtract
} from '@carbon/icons-react'

// Base URL configuration for Axios
const API_URL = '/api'

export default function App() {
  // Auth state
  const [user, setUser] = useState(() => {
    const saved = localStorage.getItem('user_session')
    return saved ? JSON.parse(saved) : null
  })
  const [isRegistering, setIsRegistering] = useState(false)
  const [authUsername, setAuthUsername] = useState('')
  const [authPassword, setAuthPassword] = useState('')
  const [authError, setAuthError] = useState('')

  // Core application state
  const [products, setProducts] = useState([])
  const [cart, setCart] = useState({ items: [], totalAmount: 0.0 })
  const [currentPage, setCurrentPage] = useState('catalog') // 'catalog', 'cart', 'add-product'
  const [loading, setLoading] = useState(false)
  const [notification, setNotification] = useState(null) // { type: 'success'|'error', title: '', message: '' }

  // Admin: Add Product form state
  const [newProdName, setNewProdName] = useState('')
  const [newProdDesc, setNewProdDesc] = useState('')
  const [newProdPrice, setNewProdPrice] = useState('')
  const [newProdImg, setNewProdImg] = useState('')
  const [newProdInventory, setNewProdInventory] = useState('')

  // Configure Axios default headers when user state changes
  useEffect(() => {
    if (user && user.token) {
      axios.defaults.headers.common['Authorization'] = `Bearer ${user.token}`
      fetchCart()
    } else {
      delete axios.defaults.headers.common['Authorization']
      setCart({ items: [], totalAmount: 0.0 })
    }
    fetchProducts()
  }, [user])

  // Clear notification after 5 seconds
  useEffect(() => {
    if (notification) {
      const timer = setTimeout(() => setNotification(null), 5000)
      return () => clearTimeout(timer)
    }
  }, [notification])

  const showNotification = (type, title, message) => {
    setNotification({ type, title, message })
  }

  // API calls - Products
  const fetchProducts = async () => {
    try {
      setLoading(true)
      const res = await axios.get(`${API_URL}/products`)
      setProducts(res.data)
    } catch (err) {
      console.error(err)
      showNotification('error', 'Error', 'Failed to fetch products.')
    } finally {
      setLoading(false)
    }
  }

  const handleAddProduct = async (e) => {
    e.preventDefault()
    if (!newProdName || !newProdPrice || !newProdInventory) {
      showNotification('error', 'Validation Error', 'Please fill in Name, Price, and Inventory.')
      return;
    }
    try {
      setLoading(true)
      const payload = {
        name: newProdName,
        description: newProdDesc,
        price: parseFloat(newProdPrice),
        imageUrl: newProdImg || 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=500&auto=format&fit=crop&q=60&ixlib=rb-4.0.3',
        inventory: parseInt(newProdInventory)
      }
      await axios.post(`${API_URL}/products`, payload)
      showNotification('success', 'Success', 'Product added successfully!')
      
      // Reset form & reload catalog
      setNewProdName('')
      setNewProdDesc('')
      setNewProdPrice('')
      setNewProdImg('')
      setNewProdInventory('')
      setCurrentPage('catalog')
      fetchProducts()
    } catch (err) {
      console.error(err)
      const errorMsg = err.response?.data?.error || 'Failed to add product.'
      showNotification('error', 'Error', errorMsg)
    } finally {
      setLoading(false)
    }
  }

  // API calls - Cart
  const fetchCart = async () => {
    if (!user) return
    try {
      const res = await axios.get(`${API_URL}/cart`)
      setCart(res.data)
    } catch (err) {
      console.error(err)
      // If unauthorized, log out session
      if (err.response?.status === 401) {
        handleLogout()
      }
    }
  }

  const handleAddToCart = async (product) => {
    if (!user) {
      showNotification('error', 'Authorization Needed', 'Please log in to add items to your cart.')
      return
    }
    try {
      setLoading(true)
      await axios.post(`${API_URL}/cart`, { productId: product.id, quantity: 1 })
      showNotification('success', 'Cart Updated', `${product.name} added to cart!`)
      fetchCart()
    } catch (err) {
      console.error(err)
      const errorMsg = err.response?.data?.error || 'Failed to add item to cart.'
      showNotification('error', 'Error', errorMsg)
    } finally {
      setLoading(false)
    }
  }

  const handleUpdateCartQuantity = async (productId, currentQty, increment) => {
    const newQty = currentQty + increment
    if (newQty < 0) return
    try {
      setLoading(true)
      await axios.put(`${API_URL}/cart`, { productId, quantity: newQty })
      fetchCart()
      // Reload products to reflect updated inventories if checked out
    } catch (err) {
      console.error(err)
      const errorMsg = err.response?.data?.error || 'Failed to update quantity.'
      showNotification('error', 'Error', errorMsg)
    } finally {
      setLoading(false)
    }
  }

  const handleRemoveFromCart = async (productId) => {
    try {
      setLoading(true)
      await axios.delete(`${API_URL}/cart/${productId}`)
      showNotification('success', 'Cart Updated', 'Product removed from cart.')
      fetchCart()
    } catch (err) {
      console.error(err)
      showNotification('error', 'Error', 'Failed to remove product.')
    } finally {
      setLoading(false)
    }
  }

  const handleCheckout = async () => {
    try {
      setLoading(true)
      await axios.post(`${API_URL}/cart/checkout`)
      showNotification('success', 'Success', 'Checkout completed successfully! Thank you for your purchase.')
      setCart({ items: [], totalAmount: 0.0 })
      setCurrentPage('catalog')
      fetchProducts()
    } catch (err) {
      console.error(err)
      const errorMsg = err.response?.data?.error || 'Checkout failed.'
      showNotification('error', 'Checkout Error', errorMsg)
    } finally {
      setLoading(false)
    }
  }

  // API calls - Auth
  const handleAuthSubmit = async (e) => {
    e.preventDefault()
    if (!authUsername || !authPassword) {
      setAuthError('Please fill in all fields.')
      return
    }
    setAuthError('')
    try {
      setLoading(true)
      const endpoint = isRegistering ? 'register' : 'login'
      const res = await axios.post(`${API_URL}/auth/${endpoint}`, {
        username: authUsername,
        password: authPassword
      })
      
      const sessionData = res.data
      setUser(sessionData)
      localStorage.setItem('user_session', JSON.stringify(sessionData))
      
      showNotification('success', 'Welcome', `Successfully logged in as ${sessionData.username}!`)
      setAuthUsername('')
      setAuthPassword('')
    } catch (err) {
      console.error(err)
      const errorMsg = err.response?.data?.error || 'Authentication failed.'
      setAuthError(errorMsg)
    } finally {
      setLoading(false)
    }
  }

  const handleLogout = () => {
    setUser(null)
    localStorage.removeItem('user_session')
    showNotification('success', 'Logged Out', 'Successfully logged out.')
    setCurrentPage('catalog')
  }

  return (
    <div className="app-container">
      {loading && <Loading withOverlay={true} />}
      
      {/* Navigation Header */}
      <Header aria-label="Carbon Shopping Cart">
        <HeaderName href="#" prefix="IBM Carbon" onClick={() => setCurrentPage('catalog')}>
          Shopping App
        </HeaderName>
        <HeaderGlobalBar>
          {user && (
            <HeaderGlobalAction
              aria-label="Product Catalog"
              onClick={() => setCurrentPage('catalog')}
              tooltipAlignment="end"
            >
              <ShoppingBag size={20} />
            </HeaderGlobalAction>
          )}

          {user && (
            <HeaderGlobalAction
              aria-label={`Cart (${cart.items.reduce((acc, i) => acc + i.quantity, 0)})`}
              onClick={() => setCurrentPage('cart')}
              tooltipAlignment="end"
            >
              <ShoppingCart size={20} />
            </HeaderGlobalAction>
          )}

          {user && user.role === 'ADMIN' && (
            <HeaderGlobalAction
              aria-label="Add Product"
              onClick={() => setCurrentPage('add-product')}
              tooltipAlignment="end"
            >
              <Add size={20} />
            </HeaderGlobalAction>
          )}

          {user ? (
            <HeaderGlobalAction
              aria-label={`Logout (${user.username})`}
              onClick={handleLogout}
              tooltipAlignment="end"
            >
              <Logout size={20} />
            </HeaderGlobalAction>
          ) : (
            <HeaderGlobalAction
              aria-label="User Login"
              onClick={() => setCurrentPage('catalog')}
              tooltipAlignment="end"
            >
              <User size={20} />
            </HeaderGlobalAction>
          )}
        </HeaderGlobalBar>
      </Header>

      {/* Main Content Area */}
      <main className="main-content">
        {/* Active notifications */}
        {notification && (
          <div style={{ marginBottom: '1.5rem', maxWidth: '600px' }}>
            <InlineNotification
              kind={notification.type}
              title={notification.title}
              subtitle={notification.message}
              onClose={() => setNotification(null)}
            />
          </div>
        )}

        {/* Auth page if user not logged in */}
        {!user ? (
          <div className="auth-wrapper">
            <div className="auth-card">
              <div className="auth-header">
                <h2>{isRegistering ? 'Create Account' : 'Welcome Back'}</h2>
                <p style={{ color: '#6f6f6f', fontSize: '0.875rem', marginTop: '0.25rem' }}>
                  {isRegistering ? 'Sign up to start shopping with Carbon' : 'Log in to manage your cart and purchases'}
                </p>
              </div>

              {authError && (
                <div style={{ marginBottom: '1rem' }}>
                  <InlineNotification
                    kind="error"
                    title="Authentication Error"
                    subtitle={authError}
                    onClose={() => setAuthError('')}
                  />
                </div>
              )}

              <Form onSubmit={handleAuthSubmit}>
                <FormGroup legendText="">
                  <TextInput
                    id="username"
                    labelText="Username"
                    placeholder="Enter your username"
                    value={authUsername}
                    onChange={(e) => setAuthUsername(e.target.value)}
                    style={{ marginBottom: '1.25rem' }}
                  />
                  <TextInput
                    id="password"
                    type="password"
                    labelText="Password"
                    placeholder="Enter your password"
                    value={authPassword}
                    onChange={(e) => setAuthPassword(e.target.value)}
                    style={{ marginBottom: '1.5rem' }}
                  />
                  <Button type="submit" size="md" style={{ width: '100%' }}>
                    {isRegistering ? 'Register' : 'Log In'}
                  </Button>
                </FormGroup>
              </Form>

              <div className="auth-toggle">
                {isRegistering ? (
                  <p>
                    Already have an account?{' '}
                    <span onClick={() => { setIsRegistering(false); setAuthError(''); }}>Log In</span>
                  </p>
                ) : (
                  <p>
                    Don't have an account?{' '}
                    <span onClick={() => { setIsRegistering(true); setAuthError(''); }}>Create one</span>
                  </p>
                )}
              </div>
            </div>
          </div>
        ) : (
          /* Logged In Content */
          <>
            {/* Catalog Page */}
            {currentPage === 'catalog' && (
              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
                  <div>
                    <h1>Product Catalog</h1>
                    <p style={{ color: '#525252' }}>Welcome, <strong>{user.username}</strong> ({user.role})! Browse our top quality items.</p>
                  </div>
                  {user.role === 'ADMIN' && (
                    <Button onClick={() => setCurrentPage('add-product')} size="md" renderIcon={Add}>
                      New Product
                    </Button>
                  )}
                </div>

                {products.length === 0 ? (
                  <Tile style={{ padding: '2rem', textAlign: 'center', color: '#6f6f6f' }}>
                    <h3>No products available in the store.</h3>
                  </Tile>
                ) : (
                  <Grid style={{ paddingLeft: 0, paddingRight: 0 }}>
                    {products.map((product) => (
                      <Column key={product.id} sm={4} md={4} lg={4} style={{ marginBottom: '1.5rem' }}>
                        <div className="product-card">
                          <img
                            src={product.imageUrl}
                            alt={product.name}
                            className="product-image"
                          />
                          <div className="product-info">
                            <h3 className="product-title">{product.name}</h3>
                            <p className="product-description">{product.description}</p>
                            <p className="product-price">${product.price.toFixed(2)}</p>
                            <span className="product-meta">
                              {product.inventory > 0 ? (
                                <span style={{ color: '#24a148' }}>In Stock ({product.inventory} available)</span>
                              ) : (
                                <span style={{ color: '#da1e28', fontWeight: 'bold' }}>Out of Stock</span>
                              )}
                            </span>
                            <div className="product-actions">
                              <Button
                                size="sm"
                                disabled={product.inventory <= 0}
                                onClick={() => handleAddToCart(product)}
                                style={{ width: '100%', display: 'flex', justifyContent: 'center' }}
                              >
                                Add to Cart
                              </Button>
                            </div>
                          </div>
                        </div>
                      </Column>
                    ))}
                  </Grid>
                )}
              </div>
            )}

            {/* Cart Page */}
            {currentPage === 'cart' && (
              <div>
                <div style={{ marginBottom: '1.5rem' }}>
                  <h1>Your Shopping Cart</h1>
                  <p style={{ color: '#525252' }}>Manage the items you have added and proceed to checkout.</p>
                </div>

                {cart.items.length === 0 ? (
                  <Tile style={{ padding: '3rem', textAlign: 'center', color: '#6f6f6f' }}>
                    <ShoppingCart size={32} style={{ marginBottom: '1rem', color: '#a8a8a8' }} />
                    <h3>Your cart is empty.</h3>
                    <Button onClick={() => setCurrentPage('catalog')} size="md" style={{ marginTop: '1.5rem' }}>
                      Go Shopping
                    </Button>
                  </Tile>
                ) : (
                  <div>
                    <div style={{ background: '#fff', border: '1px solid #e0e0e0', padding: '1rem' }}>
                      {cart.items.map((item) => (
                        <div className="cart-item-row" key={item.id}>
                          <img
                            className="cart-item-img"
                            src={item.productImageUrl}
                            alt={item.productName}
                          />
                          <div className="cart-item-details">
                            <h3 style={{ fontSize: '1.1rem', fontWeight: 600 }}>{item.productName}</h3>
                            <p style={{ color: '#0f62fe', fontWeight: 'bold', margin: '0.25rem 0' }}>
                              ${item.productPrice.toFixed(2)} each
                            </p>
                            <span style={{ fontSize: '0.75rem', color: '#525252' }}>
                              Inventory Limit: {item.inventory} units
                            </span>
                          </div>

                          <div className="cart-item-qty-actions">
                            <Button
                              hasIconOnly
                              renderIcon={Subtract}
                              iconDescription="Decrease quantity"
                              size="sm"
                              kind="ghost"
                              disabled={item.quantity <= 1}
                              onClick={() => handleUpdateCartQuantity(item.productId, item.quantity, -1)}
                            />
                            <span style={{ fontWeight: 'bold', fontSize: '1.1rem', minWidth: '24px', textAlign: 'center' }}>
                              {item.quantity}
                            </span>
                            <Button
                              hasIconOnly
                              renderIcon={Add}
                              iconDescription="Increase quantity"
                              size="sm"
                              kind="ghost"
                              disabled={item.quantity >= item.inventory}
                              onClick={() => handleUpdateCartQuantity(item.productId, item.quantity, 1)}
                            />
                          </div>

                          <div style={{ marginRight: '2rem', minWidth: '80px', textAlign: 'right', fontWeight: 'bold' }}>
                            ${(item.productPrice * item.quantity).toFixed(2)}
                          </div>

                          <Button
                            hasIconOnly
                            renderIcon={TrashCan}
                            iconDescription="Remove item"
                            size="md"
                            kind="danger--ghost"
                            onClick={() => handleRemoveFromCart(item.productId)}
                          />
                        </div>
                      ))}

                      <div className="cart-total-section">
                        <div>
                          <span style={{ fontSize: '1.1rem', color: '#525252', marginRight: '1rem' }}>Grand Total:</span>
                          <span className="cart-total-price">${cart.totalAmount.toFixed(2)}</span>
                        </div>
                      </div>
                      
                      <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '1rem', marginTop: '1rem' }}>
                        <Button
                          kind="secondary"
                          size="lg"
                          onClick={() => setCurrentPage('catalog')}
                        >
                          Continue Shopping
                        </Button>
                        <Button
                          kind="primary"
                          size="lg"
                          onClick={handleCheckout}
                        >
                          Place Order (Checkout)
                        </Button>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            )}

            {/* Add Product Page (Admin Only) */}
            {currentPage === 'add-product' && user.role === 'ADMIN' && (
              <div style={{ maxWidth: '600px', margin: '0 auto' }}>
                <div style={{ marginBottom: '1.5rem' }}>
                  <h1>Add New Product</h1>
                  <p style={{ color: '#525252' }}>Fill out the form below to add a new item to the store's global catalog.</p>
                </div>

                <Tile style={{ padding: '2rem', backgroundColor: '#fff', border: '1px solid #e0e0e0' }}>
                  <Form onSubmit={handleAddProduct}>
                    <FormGroup legendText="">
                      <TextInput
                        id="prodName"
                        labelText="Product Name *"
                        placeholder="e.g. Mechanical Gaming Keyboard"
                        value={newProdName}
                        onChange={(e) => setNewProdName(e.target.value)}
                        style={{ marginBottom: '1.25rem' }}
                      />
                      <TextArea
                        id="prodDesc"
                        labelText="Description"
                        placeholder="Describe the product features, specifications..."
                        value={newProdDesc}
                        onChange={(e) => setNewProdDesc(e.target.value)}
                        style={{ marginBottom: '1.25rem' }}
                      />
                      <TextInput
                        id="prodPrice"
                        type="number"
                        step="0.01"
                        labelText="Price ($) *"
                        placeholder="e.g. 119.99"
                        value={newProdPrice}
                        onChange={(e) => setNewProdPrice(e.target.value)}
                        style={{ marginBottom: '1.25rem' }}
                      />
                      <TextInput
                        id="prodImg"
                        labelText="Image URL (Unsplash/Web image)"
                        placeholder="https://images.unsplash.com/..."
                        value={newProdImg}
                        onChange={(e) => setNewProdImg(e.target.value)}
                        style={{ marginBottom: '1.25rem' }}
                      />
                      <TextInput
                        id="prodInventory"
                        type="number"
                        labelText="Inventory (Quantity) *"
                        placeholder="e.g. 30"
                        value={newProdInventory}
                        onChange={(e) => setNewProdInventory(e.target.value)}
                        style={{ marginBottom: '1.5rem' }}
                      />
                      <div style={{ display: 'flex', gap: '1rem' }}>
                        <Button
                          kind="secondary"
                          onClick={() => setCurrentPage('catalog')}
                          style={{ width: '50%' }}
                        >
                          Cancel
                        </Button>
                        <Button
                          type="submit"
                          style={{ width: '50%' }}
                        >
                          Add Product
                        </Button>
                      </div>
                    </FormGroup>
                  </Form>
                </Tile>
              </div>
            )}
          </>
        )}
      </main>
    </div>
  )
}
