import React, { useState, useEffect, useMemo, useCallback, memo } from 'react';
import {
  Container,
  Paper,
  Typography,
  Box,
  Button,
  TextField,
  IconButton,
  Chip,
  Alert,
  Snackbar,
  CircularProgress,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TableSortLabel,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Tooltip,
  Modal,
  CardContent,
  Divider,
  Grid,
  Select,
  MenuItem,
  FormControl
} from '@mui/material';
import {
  Add,
  Search,
  Edit,
  Delete,
  Warning,
  CheckCircle,
  Error,
  Refresh,
  Visibility
} from '@mui/icons-material';
import api from '../../services/api';
import ProductDialog from '../../components/common/ProductDialog';
import { CATEGORIES } from '../../utils/constants/categories';
import { sortProducts, formatPrice } from '../../utils/helpers/productHelpers';

// Импортируем кастомные Grid компоненты
import { GridContainer, GridItem } from '../../components/common/CustomGrid';

// Утилитарные функции для безопасных значений
const safeString = (value, defaultValue = '') => {
  if (value === null || value === undefined) {
    return defaultValue;
  }
  return String(value);
};

const safeNumber = (value, defaultValue = 0) => {
  if (value === null || value === undefined) {
    return defaultValue;
  }
  const num = Number(value);
  return !isNaN(num) ? num : defaultValue;
};

const safeBoolean = (value, defaultValue = true) => {
  if (value === null || value === undefined) {
    return defaultValue;
  }
  return Boolean(value);
};

// Функция для получения безопасного значения unit
const safeUnit = (value) => {
  const units = ['шт', 'кг', 'г', 'л', 'мл', 'уп', 'пак'];
  const safeValue = safeString(value, 'шт');
  return units.includes(safeValue) ? safeValue : 'шт';
};

// Мемоизированные статические компоненты
const StockIcon = memo(({ stock = 0, minStock = 10 }) => {
  if (stock === 0) return <Error fontSize="small" />;
  if (stock > 0 && stock <= minStock) return <Warning fontSize="small" />;
  return <CheckCircle fontSize="small" />;
});

const StockChip = memo(({ stock = 0, minStock = 10 }) => {
  let status, color;
  if (stock === 0) {
    status = 'Нет в наличии';
    color = 'error';
  } else if (stock > 0 && stock <= minStock) {
    status = 'Мало на складе';
    color = 'warning';
  } else {
    status = 'В наличии';
    color = 'success';
  }
  
  return (
    <Chip
      label={status}
      size="small"
      color={color}
      variant="filled"
    />
  );
});

// Функция для сопоставления русских названий с английскими ключами
const mapCategoryToKey = (category) => {
  const safeCategory = safeString(category);
  if (!safeCategory) return '';
  
  const categoryMap = {
    'Фрукты': 'FRUITS',
    'Выпечка': 'BAKERY',
    'Овощи': 'VEGETABLES',
    'Мясо': 'MEAT',
    'Напитки': 'DRINKS',
    'Рыба': 'FISH',
    'Молочная продукция': 'DAIRY',
    'FRUITS': 'FRUITS',
    'BAKERY': 'BAKERY',
    'VEGETABLES': 'VEGETABLES',
    'MEAT': 'MEAT',
    'DRINKS': 'DRINKS',
    'FISH': 'FISH',
    'DAIRY': 'DAIRY'
  };
  
  return categoryMap[safeCategory] || safeCategory;
};

// Функция для обратного преобразования ключа в русское название
const mapKeyToCategory = (key) => {
  const safeKey = safeString(key);
  if (!safeKey) return '';
  
  const categoryMap = {
    'FRUITS': 'Фрукты',
    'BAKERY': 'Выпечка',
    'VEGETABLES': 'Овощи',
    'MEAT': 'Мясо',
    'DRINKS': 'Напитки',
    'FISH': 'Рыба',
    'DAIRY': 'Молочная продукция'
  };
  
  return categoryMap[safeKey] || safeKey;
};

// Получение опций категорий для Select
const getCategoryOptions = () => {
  const options = Object.entries(CATEGORIES).map(([key, value]) => ({
    value: key,
    label: value.label || key
  }));
  
  return [
    { value: '', label: 'Без категории' },
    ...options
  ];
};

// Стиль для модального окна
const modalStyle = {
  position: 'absolute',
  top: '50%',
  left: '50%',
  transform: 'translate(-50%, -50%)',
  width: 600,
  maxWidth: '90%',
  maxHeight: '90vh',
  overflowY: 'auto',
  bgcolor: 'background.paper',
  boxShadow: 24,
  borderRadius: 2,
  p: 0,
};

// Оптимизированный компонент строки
const ProductRow = memo(({ 
  product, 
  onShowDetails,
  onEdit,
  onDelete,
  onCategoryChange,
  isSavingProduct
}) => {
  
  const [localCategory, setLocalCategory] = useState(() => 
    mapCategoryToKey(safeString(product.category))
  );
  
  // Обновляем локальное состояние при изменении продукта
  useEffect(() => {
    setLocalCategory(mapCategoryToKey(safeString(product.category)));
  }, [product.category]);
  
  const handleShowDetails = () => {
    onShowDetails(product);
  };
  
  const handleEdit = () => {
    onEdit(product);
  };
  
  const handleDelete = () => {
    onDelete(product.id);
  };
  
  const handleCategoryChange = (e) => {
    const newCategoryKey = safeString(e.target.value);
    setLocalCategory(newCategoryKey);
    onCategoryChange(product.id, newCategoryKey);
  };

  // Безопасные значения
  const safeProductName = safeString(product.name, 'Без названия');
  const safeDescription = safeString(product.description);
  const safeAkticul = safeString(product.akticul);
  const safeUnitValue = safeUnit(product.unit);
  const safeStock = safeNumber(product.stock);
  const safePrice = safeNumber(product.price);
  const safeIsActive = safeBoolean(product.isActive);

  return (
    <TableRow hover>
      <TableCell>
        <Box>
          <Typography fontWeight="medium">
            {safeProductName}
          </Typography>
          {safeDescription && (
            <Typography variant="caption" color="text.secondary" display="block">
              {safeDescription.substring(0, 50)}
              {safeDescription.length > 50 ? '...' : ''}
            </Typography>
          )}
          {safeAkticul && (
            <Typography variant="caption" color="text.secondary" display="block">
              Арт: {safeAkticul}
            </Typography>
          )}
          {!safeIsActive && (
            <Typography variant="caption" color="error" display="block">
              (Неактивен)
            </Typography>
          )}
        </Box>
      </TableCell>
      
      {/* Категория с выпадающим меню */}
      <TableCell>
        <FormControl fullWidth size="small">
          <Select
            value={localCategory}
            onChange={handleCategoryChange}
            disabled={isSavingProduct}
            sx={{ 
              minWidth: 120,
              '& .MuiSelect-select': {
                py: 0.5,
                px: 1
              }
            }}
            displayEmpty
          >
            {getCategoryOptions().map((option) => (
              <MenuItem key={option.value} value={option.value}>
                {option.label}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      </TableCell>
      
      {/* Цена */}
      <TableCell>
        <Typography fontWeight="medium">
          {formatPrice(safePrice)}
        </Typography>
      </TableCell>
      
      {/* Количество */}
      <TableCell>
        <Box display="flex" alignItems="center" gap={1}>
          <StockIcon stock={safeStock} minStock={product.minStock} />
          <Typography fontWeight="medium">
            {safeStock} {safeUnitValue}
          </Typography>
        </Box>
      </TableCell>
      
      <TableCell>
        <StockChip stock={safeStock} minStock={product.minStock} />
      </TableCell>
      
      <TableCell>
        <Box display="flex" gap={1}>
          <Tooltip title="Просмотреть детали">
            <IconButton
              size="small"
              onClick={handleShowDetails}
              color="info"
              disabled={isSavingProduct}
            >
              <Visibility fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip title="Редактировать">
            <IconButton
              size="small"
              onClick={handleEdit}
              disabled={isSavingProduct}
            >
              <Edit fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip title="Удалить">
            <IconButton
              size="small"
              onClick={handleDelete}
              color="error"
              disabled={isSavingProduct}
            >
              <Delete fontSize="small" />
            </IconButton>
          </Tooltip>
        </Box>
      </TableCell>
    </TableRow>
  );
});

ProductRow.displayName = 'ProductRow';

const Products = () => {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [snackbar, setSnackbar] = useState({ 
    open: false, 
    message: '', 
    severity: 'info' 
  });
  const [savingProducts, setSavingProducts] = useState({});
  
  // Фильтрация и поиск
  const [searchTerm, setSearchTerm] = useState('');
  
  // Сортировка
  const [orderBy, setOrderBy] = useState('name');
  const [order, setOrder] = useState('asc');
  
  // Диалоги
  const [dialogOpen, setDialogOpen] = useState(false);
  const [dialogMode, setDialogMode] = useState('create');
  const [selectedProduct, setSelectedProduct] = useState(null);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [productToDelete, setProductToDelete] = useState(null);
  
  // Модальное окно деталей продукта
  const [detailModalOpen, setDetailModalOpen] = useState(false);
  const [selectedProductDetail, setSelectedProductDetail] = useState(null);

  // Загрузка продуктов
  const loadProducts = useCallback(async () => {
    try {
      setLoading(true);
      setError('');
      
      const response = await api.productsAPI.getAll();
      
      if (response && Array.isArray(response)) {
        const formattedProducts = response.map(item => ({
          id: safeNumber(item.id),
          name: safeString(item.name, 'Без названия'),
          category: safeString(item.category),
          price: safeNumber(item.price),
          stock: safeNumber(item.count || item.stock),
          minStock: 10,
          unit: safeUnit(item.unit), // Используем safeUnit
          isActive: safeBoolean(item.isActive, true),
          akticul: safeString(item.akticul),
          description: safeString(item.description),
          supplier: safeString(item.supplier, 'Не указан'),
          manufacturer: safeString(item.manufacturer, 'Не указан'),
          barcode: safeString(item.barcode),
          weight: safeNumber(item.weight),
          dimensions: safeString(item.dimensions),
          location: safeString(item.location, 'Склад А'),
          createdDate: item.createdDate || new Date().toISOString().split('T')[0]
        }));
        
        setProducts(formattedProducts);
      } else {
        setProducts([]);
      }
      
    } catch (err) {
      console.error('Error loading products:', err);
      setError('Ошибка при загрузке товаров');
      setSnackbar({
        open: true,
        message: 'Ошибка при загрузке товаров',
        severity: 'error'
      });
      setProducts([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadProducts();
  }, [loadProducts]);

  // Фильтрация и сортировка
  const filteredAndSortedProducts = useMemo(() => {
    if (!searchTerm.trim()) {
      return sortProducts(products, orderBy, order);
    }
    
    const term = searchTerm.toLowerCase();
    const filtered = products.filter(product => {
      const name = safeString(product.name).toLowerCase();
      const description = safeString(product.description).toLowerCase();
      const akticul = safeString(product.akticul).toLowerCase();
      const category = safeString(product.category).toLowerCase();
      const barcode = safeString(product.barcode).toLowerCase();
      
      return name.includes(term) ||
             description.includes(term) ||
             akticul.includes(term) ||
             category.includes(term) ||
             barcode.includes(term);
    });
    
    return sortProducts(filtered, orderBy, order);
  }, [products, searchTerm, orderBy, order]);

  // Обработчики
  const handleOpenCreateDialog = useCallback(() => {
    setDialogMode('create');
    setSelectedProduct(null);
    setDialogOpen(true);
  }, []);

  const handleOpenEditDialog = useCallback((product) => {
    setDialogMode('edit');
    setSelectedProduct(product);
    setDialogOpen(true);
  }, []);

  const handleCloseDialog = useCallback(() => {
    setDialogOpen(false);
    setSelectedProduct(null);
  }, []);

  const handleShowProductDetails = useCallback((product) => {
    setSelectedProductDetail(product);
    setDetailModalOpen(true);
  }, []);

  const handleCloseDetailModal = useCallback(() => {
    setDetailModalOpen(false);
    setSelectedProductDetail(null);
  }, []);

  const handleSubmitProduct = useCallback(async (productData) => {
    try {
      setLoading(true);
      
      // Подготовка данных для API с безопасными значениями
      const apiData = {
        name: safeString(productData.name),
        price: safeNumber(productData.price),
        count: safeNumber(productData.stock),
        akticul: productData.akticul || null,
        description: productData.description || null,
        category: productData.category || null,
        supplier: productData.supplier || null,
        unit: safeUnit(productData.unit), // Добавляем unit
        isActive: safeBoolean(productData.isActive, true) // Добавляем isActive
      };
      
      console.log('Submitting product data:', apiData);
      
      if (dialogMode === 'create') {
        await api.productsAPI.create(apiData);
        setSnackbar({
          open: true,
          message: 'Товар успешно добавлен',
          severity: 'success'
        });
      } else {
        await api.productsAPI.update(selectedProduct.id, apiData);
        setSnackbar({
          open: true,
          message: 'Товар успешно обновлен',
          severity: 'success'
        });
      }
      
      await loadProducts();
      handleCloseDialog();
      
    } catch (err) {
      console.error('Error saving product:', err);
      const errorMessage = err.response?.data?.message || 
                          err.response?.data?.error || 
                          err.message || 
                          'Ошибка при сохранении товара';
      setSnackbar({
        open: true,
        message: errorMessage,
        severity: 'error'
      });
    } finally {
      setLoading(false);
    }
  }, [dialogMode, selectedProduct, loadProducts, handleCloseDialog]);

  // Оптимизированный обработчик изменения категории
  const handleCategoryChange = useCallback(async (productId, newCategoryKey) => {
    try {
      // Устанавливаем состояние сохранения только для этого продукта
      setSavingProducts(prev => ({ ...prev, [productId]: true }));
      
      // Находим продукт
      const product = products.find(p => p.id === productId);
      if (!product) return;
      
      // Конвертируем английский ключ обратно в русское название для API
      let categoryForApi = safeString(newCategoryKey) ? mapKeyToCategory(newCategoryKey) : '';
      
      // Подготавливаем данные для обновления
      const updatedData = {
        name: safeString(product.name),
        price: safeNumber(product.price),
        count: safeNumber(product.stock),
        akticul: product.akticul || null,
        description: product.description || null,
        category: categoryForApi,
        supplier: product.supplier || null,
        unit: safeUnit(product.unit),
        isActive: safeBoolean(product.isActive, true)
      };
      
      // Отправляем запрос на обновление
      await api.productsAPI.update(productId, updatedData);
      
      // Обновляем локальное состояние
      setProducts(prev => 
        prev.map(p => {
          if (p.id === productId) {
            return { 
              ...p, 
              category: categoryForApi 
            };
          }
          return p;
        })
      );
      
      setSnackbar({
        open: true,
        message: 'Категория успешно изменена',
        severity: 'success'
      });
      
    } catch (err) {
      console.error('Error changing category:', err);
      setSnackbar({
        open: true,
        message: 'Ошибка при изменении категории',
        severity: 'error'
      });
    } finally {
      // Сбрасываем состояние сохранения для этого продукта
      setSavingProducts(prev => ({ ...prev, [productId]: false }));
    }
  }, [products]);

  const handleDeleteClick = useCallback((productId) => {
    setProductToDelete(productId);
    setDeleteDialogOpen(true);
  }, []);

  const handleDeleteConfirm = useCallback(async () => {
    try {
      setLoading(true);
      await api.productsAPI.delete(productToDelete);
      
      setSnackbar({
        open: true,
        message: 'Товар успешно удален',
        severity: 'success'
      });
      
      await loadProducts();
    } catch (err) {
      console.error('Error deleting product:', err);
      setSnackbar({
        open: true,
        message: err.response?.data?.message || 'Ошибка при удалении товара',
        severity: 'error'
      });
    } finally {
      setLoading(false);
      setDeleteDialogOpen(false);
      setProductToDelete(null);
    }
  }, [productToDelete, loadProducts]);

  const handleSort = useCallback((property) => {
    const isAsc = orderBy === property && order === 'asc';
    setOrder(isAsc ? 'desc' : 'asc');
    setOrderBy(property);
  }, [orderBy, order]);

  const handleRefresh = useCallback(() => {
    loadProducts();
    setSnackbar({
      open: true,
      message: 'Данные обновлены',
      severity: 'info'
    });
  }, [loadProducts]);

  const handleClearFilters = useCallback(() => {
    setSearchTerm('');
  }, []);

  // Если грузим и нет товаров
  if (loading && products.length === 0) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" height="60vh">
        <CircularProgress />
        <Typography ml={2}>Загрузка товаров...</Typography>
      </Box>
    );
  }

  return (
    <Container maxWidth="xl" sx={{ py: 3 }}>
      {/* Заголовок и кнопки */}
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Box>
          <Typography variant="h4" fontWeight="bold">
            Управление товарами
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {filteredAndSortedProducts.length} из {products.length} товаров
          </Typography>
        </Box>
        <Box display="flex" gap={2}>
          <Button
            variant="outlined"
            startIcon={<Refresh />}
            onClick={handleRefresh}
            disabled={loading}
          >
            Обновить
          </Button>
          <Button
            variant="contained"
            color="primary"
            startIcon={<Add />}
            onClick={handleOpenCreateDialog}
            disabled={loading}
          >
            Добавить товар
          </Button>
        </Box>
      </Box>

      {/* Панель поиска */}
      <Paper sx={{ p: 3, mb: 3 }}>
        <GridContainer spacing={2}>
          <GridItem xs={12}>
            <TextField
              fullWidth
              variant="outlined"
              placeholder="Поиск по названию, описанию, артикулу, штрих-коду..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              InputProps={{
                startAdornment: <Search style={{ marginRight: 8, color: '#666' }} />,
                sx: { height: 56 }
              }}
              size="medium"
              sx={{
                '& .MuiOutlinedInput-root': {
                  borderRadius: 2,
                }
              }}
              disabled={Object.keys(savingProducts).length > 0}
            />
          </GridItem>
          {searchTerm && (
            <GridItem xs={12}>
              <Box display="flex" justifyContent="space-between" alignItems="center">
                <Typography variant="body2" color="text.secondary">
                  Найдено: {filteredAndSortedProducts.length} товаров
                </Typography>
                <Button 
                  onClick={handleClearFilters}
                  variant="outlined"
                  size="small"
                  disabled={Object.keys(savingProducts).length > 0}
                >
                  Очистить фильтр
                </Button>
              </Box>
            </GridItem>
          )}
        </GridContainer>
      </Paper>

      {/* Сообщение об ошибке */}
      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}

      {/* Таблица товаров */}
      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>
                <TableSortLabel
                  active={orderBy === 'name'}
                  direction={orderBy === 'name' ? order : 'asc'}
                  onClick={() => handleSort('name')}
                  disabled={Object.keys(savingProducts).length > 0}
                >
                  Название
                </TableSortLabel>
              </TableCell>
              <TableCell>
                <TableSortLabel
                  active={orderBy === 'category'}
                  direction={orderBy === 'category' ? order : 'asc'}
                  onClick={() => handleSort('category')}
                  disabled={Object.keys(savingProducts).length > 0}
                >
                  Категория
                </TableSortLabel>
              </TableCell>
              <TableCell>
                <TableSortLabel
                  active={orderBy === 'price'}
                  direction={orderBy === 'price' ? order : 'asc'}
                  onClick={() => handleSort('price')}
                  disabled={Object.keys(savingProducts).length > 0}
                >
                  Цена
                </TableSortLabel>
              </TableCell>
              <TableCell>
                <TableSortLabel
                  active={orderBy === 'stock'}
                  direction={orderBy === 'stock' ? order : 'asc'}
                  onClick={() => handleSort('stock')}
                  disabled={Object.keys(savingProducts).length > 0}
                >
                  На складе
                </TableSortLabel>
              </TableCell>
              <TableCell>Статус</TableCell>
              <TableCell>Действия</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {filteredAndSortedProducts.length > 0 ? (
              filteredAndSortedProducts.map((product) => (
                <ProductRow
                  key={product.id}
                  product={product}
                  onShowDetails={handleShowProductDetails}
                  onEdit={handleOpenEditDialog}
                  onDelete={handleDeleteClick}
                  onCategoryChange={handleCategoryChange}
                  isSavingProduct={savingProducts[product.id] || false}
                />
              ))
            ) : (
              <TableRow>
                <TableCell colSpan={6} align="center" sx={{ py: 4 }}>
                  <Typography color="text.secondary">
                    {loading ? 'Загрузка...' : 'Товары не найдены'}
                  </Typography>
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Диалог создания/редактирования */}
      <ProductDialog
        open={dialogOpen}
        onClose={handleCloseDialog}
        product={selectedProduct}
        mode={dialogMode}
        onSubmit={handleSubmitProduct}
        loading={loading}
      />

      {/* Диалог подтверждения удаления */}
      <Dialog
        open={deleteDialogOpen}
        onClose={() => setDeleteDialogOpen(false)}
      >
        <DialogTitle>Подтверждение удаления</DialogTitle>
        <DialogContent>
          <Typography>
            Вы уверены, что хотите удалить этот товар? Это действие нельзя отменить.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteDialogOpen(false)} disabled={loading}>
            Отмена
          </Button>
          <Button onClick={handleDeleteConfirm} color="error" variant="contained" disabled={loading}>
            Удалить
          </Button>
        </DialogActions>
      </Dialog>

      {/* Модальное окно деталей продукта */}
      <Modal
        open={detailModalOpen}
        onClose={handleCloseDetailModal}
        aria-labelledby="product-detail-modal"
      >
        <Box sx={modalStyle}>
          {selectedProductDetail && (
            <>
              <Box sx={{ 
                p: 3, 
                bgcolor: 'primary.main', 
                color: 'white', 
                borderRadius: '8px 8px 0 0' 
              }}>
                <Typography variant="h5" fontWeight="bold">
                  {safeString(selectedProductDetail.name, 'Без названия')}
                </Typography>
                <Typography variant="subtitle1">
                  Артикул: {safeString(selectedProductDetail.akticul) || 'Не указан'}
                </Typography>
              </Box>
              
              <CardContent sx={{ p: 3 }}>
                <Grid container spacing={2}>
                  <Grid item xs={12}>
                    <Typography variant="h6" gutterBottom color="primary">
                      Основная информация
                    </Typography>
                  </Grid>
                  
                  <Grid item xs={12} sm={6}>
                    <Box sx={{ mb: 2 }}>
                      <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                        Категория
                      </Typography>
                      <Typography variant="body1" fontWeight="medium">
                        {safeString(selectedProductDetail.category) || 'Не указана'}
                      </Typography>
                    </Box>
                  </Grid>
                  
                  <Grid item xs={12} sm={6}>
                    <Box sx={{ mb: 2 }}>
                      <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                        Цена
                      </Typography>
                      <Typography variant="body1" fontWeight="medium" color="primary">
                        {formatPrice(safeNumber(selectedProductDetail.price))}
                      </Typography>
                    </Box>
                  </Grid>
                  
                  <Grid item xs={12} sm={6}>
                    <Box sx={{ mb: 2 }}>
                      <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                        Количество на складе
                      </Typography>
                      <Box display="flex" alignItems="center" gap={1}>
                        <StockIcon 
                          stock={safeNumber(selectedProductDetail.stock)} 
                          minStock={selectedProductDetail.minStock} 
                        />
                        <Typography variant="body1" fontWeight="medium">
                          {safeNumber(selectedProductDetail.stock)} {safeUnit(selectedProductDetail.unit)}
                        </Typography>
                      </Box>
                    </Box>
                  </Grid>
                  
                  <Grid item xs={12} sm={6}>
                    <Box sx={{ mb: 2 }}>
                      <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                        Минимальный запас
                      </Typography>
                      <Typography variant="body1" fontWeight="medium">
                        {selectedProductDetail.minStock || 10} шт
                      </Typography>
                    </Box>
                  </Grid>
                  
                  <Grid item xs={12}>
                    <Divider sx={{ my: 2 }} />
                    <Typography variant="h6" gutterBottom color="primary">
                      Дополнительная информация
                    </Typography>
                  </Grid>
                  
                  <Grid item xs={12}>
                    <Box sx={{ mb: 3 }}>
                      <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                        Описание
                      </Typography>
                      <Paper variant="outlined" sx={{ p: 2, bgcolor: 'grey.50', borderRadius: 1 }}>
                        <Typography variant="body1">
                          {safeString(selectedProductDetail.description) || 'Описание отсутствует'}
                        </Typography>
                      </Paper>
                    </Box>
                  </Grid>
                  
                  <Grid item xs={12} sm={6}>
                    <Box sx={{ mb: 2 }}>
                      <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                        Местоположение
                      </Typography>
                      <Typography variant="body1" fontWeight="medium">
                        {safeString(selectedProductDetail.location) || 'Не указано'}
                      </Typography>
                    </Box>
                  </Grid>
                  
                  <Grid item xs={12} sm={6}>
                    <Box sx={{ mb: 2 }}>
                      <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                        Добавлен
                      </Typography>
                      <Typography variant="body1" fontWeight="medium">
                        {selectedProductDetail.createdDate || 'Не указана'}
                      </Typography>
                    </Box>
                  </Grid>
                  
                  {selectedProductDetail.barcode && (
                    <Grid item xs={12} sm={6}>
                      <Box sx={{ mb: 2 }}>
                        <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                          Штрих-код
                        </Typography>
                        <Typography variant="body1" fontWeight="medium">
                          {safeString(selectedProductDetail.barcode)}
                        </Typography>
                      </Box>
                    </Grid>
                  )}
                </Grid>
              </CardContent>
              
              <Box sx={{ 
                p: 2, 
                bgcolor: 'grey.50', 
                borderTop: 1, 
                borderColor: 'grey.200', 
                borderRadius: '0 0 8px 8px',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center'
              }}>
                <Typography variant="caption" color="text.secondary">
                  ID: {selectedProductDetail.id}
                </Typography>
                <Box display="flex" gap={2}>
                  <Button
                    variant="outlined"
                    onClick={handleCloseDetailModal}
                    sx={{ minWidth: 120 }}
                    disabled={loading}
                  >
                    Закрыть
                  </Button>
                  <Button
                    variant="contained"
                    startIcon={<Edit />}
                    onClick={() => {
                      handleCloseDetailModal();
                      handleOpenEditDialog(selectedProductDetail);
                    }}
                    sx={{ minWidth: 140 }}
                    disabled={loading}
                  >
                    Редактировать
                  </Button>
                </Box>
              </Box>
            </>
          )}
        </Box>
      </Modal>

      {/* Snackbar для уведомлений */}
      <Snackbar
        open={snackbar.open}
        autoHideDuration={6000}
        onClose={() => setSnackbar({ ...snackbar, open: false })}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
      >
        <Alert 
          onClose={() => setSnackbar({ ...snackbar, open: false })}
          severity={snackbar.severity}
          sx={{ width: '100%' }}
        >
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Container>
  );
};

export default Products;