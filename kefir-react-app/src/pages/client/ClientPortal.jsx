import React, { useState, useEffect, useCallback, useMemo } from 'react';
import axios from 'axios';
import PaymentModal from './PaymentModal';
import '../../App.css';
import './ClientPortal.css';

const ClientPortal = () => {
  const [products, setProducts] = useState([]);
  const [filteredProducts, setFilteredProducts] = useState([]);
  const [cart, setCart] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [orderStatus, setOrderStatus] = useState(null);
  const [selectedCategory, setSelectedCategory] = useState('Все');
  const [categories, setCategories] = useState([]);
  const [selectedProduct, setSelectedProduct] = useState(null);
  const [showProductModal, setShowProductModal] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [sortOrder, setSortOrder] = useState('name-asc');
  // Добавьте это состояние
const [showPaymentModal, setShowPaymentModal] = useState(false);
const [currentOrderDetails, setCurrentOrderDetails] = useState(null);

  const getAuthToken = useCallback(() => {
    return localStorage.getItem('token');
  }, []);

  const getCleanToken = useCallback(() => {
    const token = getAuthToken();
    if (!token) return null;
    
    return token.replace(/^Bearer\s+/i, '');
  }, [getAuthToken]);

  const getAuthHeaders = useCallback(() => {
    const token = getCleanToken();
    if (!token) {
      console.warn('Токен не найден или пустой');
      return {};
    }

    let cleanToken = token;
    if (!cleanToken.startsWith('auth-') && !cleanToken.includes('-')) {
      cleanToken = `auth-${cleanToken}`;
    }

    return {
      headers: {
        'Authorization': `Bearer ${cleanToken}`,
        'Content-Type': 'application/json'
      }
    };
  }, [getCleanToken]);

  // В методе fetchProducts внутри ClientPortal.jsx
const fetchProducts = useCallback(async () => {
  try {
    setLoading(true);
    const headers = getAuthHeaders();
    
    // ИЗМЕНЕНИЕ 1: Новый URL для клиентских товаров
    const response = await axios.get('http://localhost:8080/api/client/products', headers);
    const responseData = response.data;
    
    // ИЗМЕНЕНИЕ 2: Новый формат ответа (объект с полем products)
    let productsData = [];
    if (responseData.success && responseData.products) {
      productsData = responseData.products;
      
      // Логируем информацию о складе для отладки
      console.log('✅ Товары получены со склада:', responseData.warehouse);
      console.log('📍 Город пользователя:', responseData.userCity);
      console.log('📦 Количество товаров:', productsData.length);
    } else if (Array.isArray(responseData)) {
      // Обратная совместимость: если ответ массив (старый формат)
      productsData = responseData;
      console.warn('⚠️ Получен старый формат ответа (массив)');
    } else {
      console.error('❌ Неожиданный формат ответа:', responseData);
      throw new Error('Неверный формат ответа от сервера');
    }
    
    productsData.sort((a, b) => a.name.localeCompare(b.name));
    
    setProducts(productsData);
    
    // Извлекаем категории из полученных товаров
    const uniqueCategories = ['Все', ...new Set(productsData.map(p => p.category))];
    setCategories(uniqueCategories);
    setFilteredProducts(productsData);
    setError(null);
    
    // Сохраняем информацию о складе в состоянии (для отображения)
    if (responseData.warehouse) {
      console.log(`🏪 Текущий склад: ${responseData.warehouse}`);
      // Можно добавить состояние для отображения информации о складе
      // setCurrentWarehouse(responseData.warehouse);
    }
    
  } catch (err) {
    console.error('❌ Ошибка при загрузке товаров:', err);
    
    // Улучшенная обработка ошибок
    if (err.response) {
      const status = err.response.status;
      const errorData = err.response.data;
      
      if (status === 401) {
        setError('Сессия истекла. Пожалуйста, войдите снова.');
        localStorage.removeItem('token');
        // Можно добавить редирект на логин
        // window.location.href = '/login';
      } else if (status === 403) {
        setError('Доступ запрещен. Недостаточно прав.');
      } else if (status === 404) {
        setError('Товары не найдены на вашем складе.');
      } else if (errorData && errorData.message) {
        setError(`Ошибка: ${errorData.message}`);
      } else {
        setError(`Ошибка сервера (${status}). Пожалуйста, попробуйте позже.`);
      }
    } else if (err.request) {
      setError('Нет ответа от сервера. Проверьте подключение к сети.');
    } else if (err.message) {
      setError(`Ошибка: ${err.message}`);
    } else {
      setError('Неизвестная ошибка при загрузке товаров.');
    }
    
    setProducts([]);
    setFilteredProducts([]);
    setCategories(['Все']);
  } finally {
    setLoading(false);
  }
}, [getAuthHeaders]);
  useEffect(() => {
    const checkAuth = () => {
      const token = getAuthToken();
      if (!token) {
        setError('Требуется авторизация. Пожалуйста, войдите в систему.');
      }
    };
    
    checkAuth();
  }, [getAuthToken]);

  useEffect(() => {
    fetchProducts();
  }, [fetchProducts]);

  useEffect(() => {
    let result = [...products];
    
    if (selectedCategory !== 'Все') {
      result = result.filter(product => product.category === selectedCategory);
    }
    
    if (searchQuery.trim()) {
      const query = searchQuery.toLowerCase();
      result = result.filter(product => 
        product.name.toLowerCase().includes(query) ||
        (product.description && product.description.toLowerCase().includes(query))
      );
    }
    
    result.sort((a, b) => {
      switch(sortOrder) {
        case 'name-asc':
          return a.name.localeCompare(b.name);
        case 'name-desc':
          return b.name.localeCompare(a.name);
        case 'price-asc':
          return a.price - b.price;
        case 'price-desc':
          return b.price - a.price;
        default:
          return a.name.localeCompare(b.name);
      }
    });
    
    setFilteredProducts(result);
  }, [products, selectedCategory, searchQuery, sortOrder]);

  const addToCart = useCallback((product) => {
    if (product.count <= 0) {
      alert('Товара нет в наличии');
      return;
    }

    const existingItem = cart.find(item => item.id === product.id);
    
    if (existingItem) {
      if (existingItem.quantity >= product.count) {
        alert('Недостаточно товара на складе');
        return;
      }
      setCart(cart.map(item =>
        item.id === product.id
          ? { ...item, quantity: item.quantity + 1 }
          : item
      ));
    } else {
      setCart([...cart, { 
        ...product, 
        quantity: 1
      }]);
    }
  }, [cart]);

  const removeFromCart = useCallback((productId) => {
    setCart(cart.filter(item => item.id !== productId));
  }, [cart]);

  const updateQuantity = useCallback((productId, newQuantity) => {
    if (newQuantity < 1) {
      removeFromCart(productId);
      return;
    }

    const product = products.find(p => p.id === productId);
    if (product && newQuantity > product.count) {
      alert(`Максимальное количество: ${product.count}`);
      return;
    }

    setCart(cart.map(item =>
      item.id === productId
        ? { ...item, quantity: newQuantity }
        : item
    ));
  }, [cart, products, removeFromCart]);

 const calculateTotal = useMemo(() => {
  return Number(cart.reduce((total, item) => total + (item.price * item.quantity), 0).toFixed(2));
}, [cart]);

const extractUserIdFromToken = useCallback((token) => {
  try {
    if (!token) return null;
    
    const cleanToken = token.replace('Bearer ', '');
    
    // Для auth-токенов
    if (cleanToken.startsWith('auth-')) {
      // TODO: Получить userId из auth сервиса
      // Пока возвращаем заглушку
      return 1;
    }
    
    // Для JWT токенов
    if (cleanToken.includes('.')) {
      const payload = JSON.parse(atob(cleanToken.split('.')[1]));
      return payload.userId || payload.id || payload.sub;
    }
    
    return null;
  } catch (err) {
    console.error('Ошибка извлечения userId:', err);
    return null;
  }
}, []);

const handleCheckout = useCallback(async () => {
  if (cart.length === 0) {
    alert('Корзина пуста!');
    return;
  }

  const token = getAuthToken();
  if (!token) {
    alert('Требуется авторизация. Пожалуйста, войдите в систему.');
    return;
  }

  try {
    setOrderStatus('processing');
    
    // Извлекаем userId из токена
    const userId = extractUserIdFromToken(token);
    
    // НЕ СОЗДАЕМ ЗАКАЗ НА СЕРВЕРЕ!
    // Просто подготавливаем данные для модального окна
    const orderDetails = {
      userId: userId,
      items: cart.map(item => ({
        productId: item.id,
        productName: item.name,
        quantity: item.quantity,
        price: item.price
      })),
      totalAmount: calculateTotal
    };
    
    // Открываем модальное окно оплаты
    setCurrentOrderDetails(orderDetails);
    setShowPaymentModal(true);
    
    setOrderStatus(null);
    
  } catch (err) {
    console.error('Ошибка при подготовке к оплате:', err);
    setOrderStatus('error');
    setTimeout(() => setOrderStatus(null), 3000);
  }
}, [cart, calculateTotal, getAuthToken, extractUserIdFromToken]);

const handlePaymentSuccess = useCallback((paymentData) => {
  console.log('✅ Оплата успешна:', paymentData);
  setOrderStatus('success');
  setCart([]); // Очищаем корзину
  fetchProducts(); // ← ЭТО ОБНОВИТ ТОВАРЫ
  setTimeout(() => setOrderStatus(null), 3000);
}, [fetchProducts]);

  return (
    <div className="client-portal-container">
      {orderStatus === 'success' && (
        <div className="alert alert-success alert-dismissible fade show" role="alert">
          ✅ Заказ успешно оформлен!
          <button type="button" className="btn-close" onClick={() => setOrderStatus(null)}></button>
        </div>
      )}

      {orderStatus === 'error' && (
        <div className="alert alert-danger alert-dismissible fade show" role="alert">
          ❌ Ошибка при оформлении заказа
          <button type="button" className="btn-close" onClick={() => setOrderStatus(null)}></button>
        </div>
      )}

      {orderStatus === 'auth_error' && (
        <div className="alert alert-warning alert-dismissible fade show" role="alert">
          ⚠️ Проблема с авторизацией. Пожалуйста, войдите снова.
          <button type="button" className="btn-close" onClick={() => setOrderStatus(null)}></button>
        </div>
      )}

      <div className="client-portal-layout">
        <div className="catalog-section">
          <div className="card catalog-card">
            <div className="card-header bg-primary text-white d-flex flex-wrap justify-content-between align-items-center py-2">
              <h5 className="mb-0 mb-sm-0 me-3">📦 Каталог товаров</h5>
              <div className="d-flex align-items-center flex-wrap gap-2">
                <div className="d-flex align-items-center">
                  <span className="me-2 d-none d-sm-block">Категории:</span>
                  <div className="category-buttons">
                    {categories.map(category => (
                      <button
                        key={category}
                        className={`category-btn ${selectedCategory === category ? 'active' : ''}`}
                        onClick={() => setSelectedCategory(category)}
                      >
                        {category}
                      </button>
                    ))}
                  </div>
                </div>
              </div>
            </div>
            
            <div className="card-body">
              <div className="row mb-3">
                <div className="col-md-6 mb-2 mb-md-0">
                  <div className="input-group">
                    <span className="input-group-text">
                      <i className="bi bi-search"></i>
                    </span>
                    <input
                      type="text"
                      className="form-control"
                      placeholder="Поиск товаров по названию..."
                      value={searchQuery}
                      onChange={(e) => setSearchQuery(e.target.value)}
                    />
                  </div>
                </div>
                <div className="col-md-6">
                  <div className="input-group">
                    <span className="input-group-text">
                      <i className="bi bi-sort-alpha-down"></i>
                    </span>
                    <select 
                      className="form-select" 
                      value={sortOrder}
                      onChange={(e) => setSortOrder(e.target.value)}
                    >
                      <option value="name-asc">Название (А-Я)</option>
                      <option value="name-desc">Название (Я-А)</option>
                      <option value="price-asc">Цена (по возрастанию)</option>
                      <option value="price-desc">Цена (по убыванию)</option>
                    </select>
                  </div>
                </div>
              </div>

              {(selectedCategory !== 'Все' || searchQuery) && (
                <div className="alert alert-info mb-3 py-2">
                  <small>
                    <strong>Фильтры:</strong>
                    {selectedCategory !== 'Все' && ` Категория: ${selectedCategory}`}
                    {searchQuery && ` Поиск: "${searchQuery}"`}
                    <button 
                      className="btn btn-sm btn-outline-info ms-2"
                      onClick={() => {
                        setSelectedCategory('Все');
                        setSearchQuery('');
                      }}
                    >
                      Сбросить
                    </button>
                  </small>
                </div>
              )}

              <div className="product-grid-container">
                {loading ? (
                  <div className="text-center py-5">
                    <div className="spinner-border text-primary" role="status">
                      <span className="visually-hidden">Загрузка...</span>
                    </div>
                    <p className="mt-3">Загрузка товаров...</p>
                  </div>
                ) : error ? (
                  <div className="alert alert-warning m-2" role="alert">
                    <strong>Внимание:</strong> {error}
                    {error.includes('авторизация') && (
                      <button 
                        className="btn btn-sm btn-outline-warning ms-2"
                        onClick={() => window.location.href = '/login'}
                      >
                        Войти
                      </button>
                    )}
                  </div>
                ) : filteredProducts.length === 0 ? (
                  <div className="text-center py-5">
                    <p className="text-muted">Товары не найдены</p>
                    {(selectedCategory !== 'Все' || searchQuery) && (
                      <button 
                        className="btn btn-outline-primary btn-sm mt-2"
                        onClick={() => {
                          setSelectedCategory('Все');
                          setSearchQuery('');
                        }}
                      >
                        Показать все товары
                      </button>
                    )}
                  </div>
                ) : (
                  <div className="row g-2">
                    {filteredProducts.map(product => (
                      <div key={product.id} className="col-xxl-3 col-xl-3 col-lg-4 col-md-6 col-sm-6">
                        <div className="card h-100 product-card">
                          <div className="card-body d-flex flex-column p-2">
                            <div className="category-top mb-2">
                              <span className="badge category-badge">{product.category}</span>
                            </div>
                            
                            <div className="flex-grow-1">
                              <h6 className="card-title mb-1 fw-bold product-name">{product.name}</h6>
                              
                              <div className="mb-3">
                                <span className={`stock-badge ${product.count > 10 ? 'in-stock' : product.count > 0 ? 'low-stock' : 'out-of-stock'}`}>
                                  {product.count > 0 ? `${product.count} шт.` : 'Нет в наличии'}
                                </span>
                              </div>
                            </div>
                            
                            <div className="product-footer">
                              <div className="price-section mb-1">
                                <strong className="product-price">{product.price} ₽</strong>
                              </div>
                              
                              <div className="d-grid gap-1">
                                <button
                                  className="btn btn-info btn-sm details-btn"
                                  onClick={() => {
                                    setSelectedProduct(product);
                                    setShowProductModal(true);
                                  }}
                                >
                                  <i className="bi bi-info-circle me-1"></i> Подробнее
                                </button>
                                <button
                                  className="btn btn-primary add-to-cart-btn"
                                  onClick={() => addToCart(product)}
                                  disabled={product.count <= 0}
                                >
                                  {product.count > 0 ? (
                                    <>
                                      <i className="bi bi-cart-plus me-1"></i> В корзину
                                    </>
                                  ) : 'Нет в наличии'}
                                </button>
                              </div>
                            </div>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>

        <div className="cart-section">
          <div className="card cart-card">
            <div className="card-header bg-success text-white d-flex justify-content-between align-items-center py-2">
              <h5 className="mb-0">
                <i className="bi bi-cart3 me-2"></i> Корзина
              </h5>
              <span className="badge bg-light text-dark cart-count">
                {cart.reduce((sum, item) => sum + item.quantity, 0)}
              </span>
            </div>
            
            <div className="cart-content">
              {cart.length === 0 ? (
                <div className="text-center text-muted p-4">
                  <div className="mb-2">
                    <i className="bi bi-cart" style={{ fontSize: '3rem', opacity: 0.3 }}></i>
                  </div>
                  <p className="mb-1">Корзина пуста</p>
                  <small>Добавьте товары из каталога</small>
                </div>
              ) : (
                <div className="p-2">
                  {cart.map(item => (
                    <div key={item.id} className="cart-item mb-2 p-2 border rounded">
                      <div className="d-flex justify-content-between align-items-start">
                        <div className="flex-grow-1">
                          <h6 className="mb-1 fw-bold cart-item-name">{item.name}</h6>
                          <p className="mb-1 small">
                            <strong>{item.price} ₽</strong>
                          </p>
                        </div>
                        <button
                          className="btn btn-outline-danger btn-sm ms-1"
                          onClick={() => removeFromCart(item.id)}
                          title="Удалить"
                        >
                          ✕
                        </button>
                      </div>
                      
                      <div className="d-flex align-items-center justify-content-between mt-2">
                        <div className="btn-group btn-group-sm">
                          <button
                            className="btn btn-outline-secondary"
                            onClick={() => updateQuantity(item.id, item.quantity - 1)}
                          >
                            -
                          </button>
                          <span className="px-2">{item.quantity}</span>
                          <button
                            className="btn btn-outline-secondary"
                            onClick={() => updateQuantity(item.id, item.quantity + 1)}
                            disabled={item.quantity >= item.count}
                          >
                            +
                          </button>
                        </div>
                        <small className="text-muted">
                          {(item.price * item.quantity).toFixed(2)} ₽
                        </small>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
            
            <div className="card-footer border-top p-2 bg-light">
              <div className="d-flex justify-content-between align-items-center mb-2">
                <h5 className="mb-0">Итого:</h5>
                <h4 className="mb-0 text-success total-price">
                  {calculateTotal} ₽
                </h4>
              </div>
              
              <button
                className="btn btn-success w-100 mb-2 checkout-btn"
                onClick={handleCheckout}
                disabled={cart.length === 0 || orderStatus === 'processing'}
              >
                {orderStatus === 'processing' ? (
                  <>
                    <span className="spinner-border spinner-border-sm me-2" role="status"></span>
                    Оформление...
                  </>
                ) : (
                  <>
                    <i className="bi bi-credit-card me-2"></i> Оплатить
                  </>
                )}
              </button>
              
              <button
                className="btn btn-outline-secondary w-100 btn-sm"
                onClick={() => {
                  if (window.confirm('Очистить корзину?')) {
                    setCart([]);
                  }
                }}
                disabled={cart.length === 0}
              >
                <i className="bi bi-trash me-1"></i> Очистить корзину
              </button>
              
       
            </div>
          </div>
        </div>
      </div>

      {showProductModal && selectedProduct && (
        <div className="modal show d-block" tabIndex="-1" role="dialog">
          <div className="modal-dialog modal-dialog-centered modal-md" role="document">
            <div className="modal-content">
              <div className="modal-header bg-light py-2">
                <div className="d-flex align-items-center">
                  <span className="badge bg-secondary me-2">{selectedProduct.category}</span>
                  <h5 className="modal-title mb-0 fs-6">{selectedProduct.name}</h5>
                </div>
                <button
                  type="button"
                  className="btn-close"
                  onClick={() => setShowProductModal(false)}
                  aria-label="Закрыть"
                ></button>
              </div>
              <div className="modal-body p-3">
                <div className="row">
                  <div className="col-12">
                    <div className="mb-2">
                      <h6 className="text-muted mb-1 small">Основная информация</h6>
                      <div className="row">
                        <div className="col-6 mb-1">
                          <strong className="small">Цена:</strong>
                          <p className="fs-5 text-primary mb-0">{selectedProduct.price} ₽</p>
                        </div>
                        <div className="col-6 mb-1">
                          <strong className="small">Наличие:</strong>
                          <p className={`fs-5 mb-0 ${selectedProduct.count > 0 ? 'text-success' : 'text-secondary'}`}>
                            {selectedProduct.count > 0 ? `${selectedProduct.count} шт. в наличии` : 'Нет в наличии'}
                          </p>
                        </div>
                      </div>
                    </div>
                    
                    {selectedProduct.description && (
                      <div className="mb-2">
                        <h6 className="text-muted mb-1 small">Описание</h6>
                        <div className="border rounded p-2 bg-light">
                          <p className="mb-0 small">{selectedProduct.description}</p>
                        </div>
                      </div>
                    )}
                    
                    {(selectedProduct.akticul || selectedProduct.supplier || selectedProduct.weight || 
                      selectedProduct.expirationDate || selectedProduct.brand) && (
                      <div className="mb-2">
                        <h6 className="text-muted mb-1 small">Дополнительная информация</h6>
                        <div className="row">
                          {selectedProduct.akticul && (
                            <div className="col-md-6 mb-1">
                              <strong className="small">Артикул:</strong>
                              <p className="mb-0 small">{selectedProduct.akticul}</p>
                            </div>
                          )}
                          {selectedProduct.supplier && (
                            <div className="col-md-6 mb-1">
                              <strong className="small">Поставщик:</strong>
                              <p className="mb-0 small">{selectedProduct.supplier}</p>
                            </div>
                          )}
                          {selectedProduct.weight && (
                            <div className="col-md-6 mb-1">
                              <strong className="small">Вес:</strong>
                              <p className="mb-0 small">{selectedProduct.weight}</p>
                            </div>
                          )}
                          {selectedProduct.expirationDate && (
                            <div className="col-md-6 mb-1">
                              <strong className="small">Срок годности:</strong>
                              <p className="mb-0 small">{selectedProduct.expirationDate}</p>
                            </div>
                          )}
                          {selectedProduct.brand && (
                            <div className="col-md-6 mb-1">
                              <strong className="small">Бренд:</strong>
                              <p className="mb-0 small">{selectedProduct.brand}</p>
                            </div>
                          )}
                          {selectedProduct.createdAt && (
                            <div className="col-md-6 mb-1">
                              <strong className="small">Дата добавления:</strong>
                              <p className="mb-0 small">{new Date(selectedProduct.createdAt).toLocaleDateString()}</p>
                            </div>
                          )}
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              </div>
              <div className="modal-footer py-2">
                <button
                  type="button"
                  className="btn btn-outline-secondary btn-sm"
                  onClick={() => setShowProductModal(false)}
                >
                  <i className="bi bi-x-circle me-1"></i>
                  Закрыть
                </button>
                <button
                  type="button"
                  className="btn btn-primary btn-sm"
                  onClick={() => {
                    addToCart(selectedProduct);
                    setShowProductModal(false);
                  }}
                  disabled={selectedProduct.count <= 0}
                >
                  <i className="bi bi-cart-plus me-1"></i>
                  Добавить в корзину
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {showProductModal && (
        <div className="modal-backdrop show fade" onClick={() => setShowProductModal(false)}></div>
      )}

      {/* Использование отдельного компонента модального окна */}
<PaymentModal
  show={showPaymentModal}
  onClose={() => {
    setShowPaymentModal(false);
    setCurrentOrderDetails(null);
  }}
  orderDetails={currentOrderDetails}
  onConfirm={handlePaymentSuccess}
  onClearCart={() => setCart([])}
  authToken={getAuthToken()} // Передаем токен для авторизации
/>
    </div>
  );
};

export default ClientPortal;