import MainLayout from './components/layout/MainLayout';

function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [userRole, setUserRole] = useState('');

  useEffect(() => {
    // Проверяем авторизацию при загрузке
    const token = localStorage.getItem('authToken');
    const userData = localStorage.getItem('userData');
    
    if (token && userData) {
      setIsAuthenticated(true);
      const user = JSON.parse(userData);
      setUserRole(user.role);
    }
  }, []);

  const handleLogin = (userData) => {
    setIsAuthenticated(true);
    setUserRole(userData.role);
    localStorage.setItem('authToken', 'demo-token-' + Date.now());
    localStorage.setItem('userData', JSON.stringify(userData));
  };

  const handleLogout = () => {
    setIsAuthenticated(false);
    setUserRole('');
    localStorage.removeItem('authToken');
    localStorage.removeItem('userData');
  };

  // Защищенный роут компонент
  const ProtectedRoute = ({ children, allowedRoles = [] }) => {
    if (!isAuthenticated) {
      return <Navigate to="/login" />;
    }
    
    if (allowedRoles.length > 0 && !allowedRoles.includes(userRole)) {
      // Если роль не подходит, перенаправляем на соответствующую страницу
      switch(userRole) {
        case 'admin':
          return <Navigate to="/admin" />;
        case 'client':
          return <Navigate to="/client" />;
        case 'courier':
          return <Navigate to="/courier" />;
        case 'collector':
          return <Navigate to="/collector" />;
        default:
          return <Navigate to="/login" />;
      }
    }
    
    return <MainLayout userRole={userRole} onLogout={handleLogout}>{children}</MainLayout>;
  };

  return (
    <Router>
      <Routes>
        {/* Public Routes */}
        <Route path="/login" element={
          isAuthenticated ? 
            <Navigate to={userRole === 'admin' ? '/admin' : '/client'} /> : 
            <Login onLogin={handleLogin} />
        } />
        
        {/* Client Routes */}
        <Route path="/client" element={
          <ProtectedRoute allowedRoles={['client', 'admin']}>
            <ClientPortal />
          </ProtectedRoute>
        } />
        
        <Route path="/client/cart" element={
          <ProtectedRoute allowedRoles={['client', 'admin']}>
            <ClientCart />
          </ProtectedRoute>
        } />
        
        <Route path="/client/profile" element={
          <ProtectedRoute allowedRoles={['client', 'admin']}>
            <ClientProfile />
          </ProtectedRoute>
        } />
        
        {/* Admin Routes */}
        <Route path="/admin" element={
          <ProtectedRoute allowedRoles={['admin']}>
            <Dashboard />
          </ProtectedRoute>
        } />
        
        <Route path="/admin/clients" element={
          <ProtectedRoute allowedRoles={['admin']}>
            <Clients />
          </ProtectedRoute>
        } />
        
        <Route path="/admin/products" element={
          <ProtectedRoute allowedRoles={['admin']}>
            <Products />
          </ProtectedRoute>
        } />
        
        <Route path="/admin/carts" element={
          <ProtectedRoute allowedRoles={['admin']}>
            <Carts />
          </ProtectedRoute>
        } />
        
        <Route path="/admin/warehouse" element={
          <ProtectedRoute allowedRoles={['admin']}>
            <Warehouse />
          </ProtectedRoute>
        } />
        
        <Route path="/admin/couriers" element={
          <ProtectedRoute allowedRoles={['admin']}>
            <Couriers />
          </ProtectedRoute>
        } />
        
        <Route path="/admin/office" element={
          <ProtectedRoute allowedRoles={['admin']}>
            <Office />
          </ProtectedRoute>
        } />
        
        <Route path="/admin/deliveries" element={
          <ProtectedRoute allowedRoles={['admin']}>
            <Deliveries />
          </ProtectedRoute>
        } />
        
        {/* Worker Routes */}
        <Route path="/courier" element={
          <ProtectedRoute allowedRoles={['courier', 'admin']}>
            <CourierApp />
          </ProtectedRoute>
        } />
        
        <Route path="/collector" element={
          <ProtectedRoute allowedRoles={['collector', 'admin']}>
            <CollectorApp />
          </ProtectedRoute>
        } />
        
        {/* Default Route */}
        <Route path="/" element={
          isAuthenticated ? 
            <Navigate to={userRole === 'admin' ? '/admin' : '/client'} /> : 
            <Navigate to="/login" />
        } />
      </Routes>
    </Router>
  );
}

export default App;
