import { 
  realClientsAPI, 
  realProductsAPI,
  realOrdersAPI,
  realCartAPI,
  realDeliveriesAPI,
  realCouriersAPI,
  realCollectorsAPI,
  realOfficeAPI,
  realWarehouseAPI,
  realAuthAPI 
} from './realApi';

class ApiService {
  // Auth
  static login = realAuthAPI.login;
  static logout = realAuthAPI.logout;
  static register = realAuthAPI.register;
  static getProfile = realAuthAPI.getProfile;

  // Clients
  static getClients = realClientsAPI.getAll;
  static getClient = realClientsAPI.getById;
  static createClient = realClientsAPI.create;
  static updateClient = realClientsAPI.update;
  static deleteClient = realClientsAPI.delete;
  static searchClients = realClientsAPI.search;

  // Products
  static getProducts = realProductsAPI.getAll;
  static getProduct = realProductsAPI.getById;
  static createProduct = realProductsAPI.create;
  static updateProduct = realProductsAPI.update;
  static deleteProduct = realProductsAPI.delete;
  static searchProducts = realProductsAPI.search;

  // Orders
  static getOrders = realOrdersAPI.getAll;
  static getOrder = realOrdersAPI.getById;
  static createOrder = realOrdersAPI.create;
  static updateOrderStatus = realOrdersAPI.updateStatus;
  static getOrdersByUser = realOrdersAPI.getByUser;

  // Cart
  static getCart = realCartAPI.getCart;
  static addToCart = realCartAPI.addToCart;
  static updateCartItem = realCartAPI.updateQuantity;
  static removeFromCart = realCartAPI.removeFromCart;
  static clearCart = realCartAPI.clearCart;

  // Delivery
  static getDeliveries = realDeliveriesAPI.getAll;
  static getDelivery = realDeliveriesAPI.getById;
  static createDelivery = realDeliveriesAPI.create;
  static updateDeliveryStatus = realDeliveriesAPI.updateStatus;
  static assignCourier = realDeliveriesAPI.assignCourier;

  // Couriers
  static getCouriers = realCouriersAPI.getAll;
  static getCourier = realCouriersAPI.getById;
  static createCourier = realCouriersAPI.create;
  static updateCourier = realCouriersAPI.update;
  static getAvailableCouriers = realCouriersAPI.getAvailable;

  // Collectors
  static getCollectors = realCollectorsAPI.getAll;
  static getCollector = realCollectorsAPI.getById;
  static createCollector = realCollectorsAPI.create;
  static assignCollectorTask = realCollectorsAPI.assignTask;

  // Warehouse
  static getInventory = realWarehouseAPI.getInventory;
  static updateStock = realWarehouseAPI.updateStock;
  static getLowStock = realWarehouseAPI.getLowStock;

  // Office
  static getStats = realOfficeAPI.getStats;
  static getSystemHealth = realOfficeAPI.getSystemHealth;
  static generateReport = realOfficeAPI.generateReport;
}

export default ApiService;