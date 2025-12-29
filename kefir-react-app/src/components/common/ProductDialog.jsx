// components/common/ProductDialog.jsx
import React, { useState, useEffect, useCallback } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  FormControl,
  Select,
  MenuItem,
  Switch,
  FormControlLabel,
  Grid,
  Box,
  Typography,
  CircularProgress,
  Divider,
  Stack
} from '@mui/material';
import { safeString, safeNumber, safeBoolean, safeUnit } from '../../utils/helpers/safeValues';

// Опции для единиц измерения
const UNIT_OPTIONS = [
  { value: 'шт', label: 'Штуки' },
  { value: 'кг', label: 'Килограммы' },
  { value: 'г', label: 'Граммы' },
  { value: 'л', label: 'Литры' },
  { value: 'уп', label: 'Упаковки' },
];

// Опции для категорий
const CATEGORY_OPTIONS = [
  { value: 'Фрукты', label: 'Фрукты' },
  { value: 'Выпечка', label: 'Выпечка' },
  { value: 'Овощи', label: 'Овощи' },
  { value: 'Мясо', label: 'Мясо' },
  { value: 'Напитки', label: 'Напитки' },
  { value: 'Рыба', label: 'Рыба' },
  { value: 'Молочная продукция', label: 'Молочная продукция' },
  { value: 'Бакалея', label: 'Бакалея' },
  { value: 'Замороженные продукты', label: 'Замороженные продукты' },
  { value: 'Консервы', label: 'Консервы' },
  { value: 'Сладости', label: 'Сладости' },
  { value: 'Хлеб', label: 'Хлеб' },
];

const ProductDialog = ({ 
  open, 
  onClose, 
  product, 
  mode, 
  onSubmit, 
  loading 
}) => {
  const [formData, setFormData] = useState({
    name: '',
    price: '',
    stock: '',
    unit: '',
    category: '',
    akticul: '',
    description: '',
    supplier: '',
    isActive: true
  });
  
  const [errors, setErrors] = useState({});

  // Инициализация формы
  useEffect(() => {
    if (product && mode === 'edit') {
      setFormData({
        name: safeString(product.name),
        price: safeString(product.price),
        stock: safeString(product.stock),
        unit: safeUnit(product.unit),
        category: safeString(product.category),
        akticul: safeString(product.akticul),
        description: safeString(product.description),
        supplier: safeString(product.supplier),
        isActive: safeBoolean(product.isActive, true)
      });
    } else {
      // Сброс к значениям по умолчанию
      setFormData({
        name: '',
        price: '',
        stock: '',
        unit: '',
        category: '',
        akticul: '',
        description: '',
        supplier: '',
        isActive: true
      });
    }
    setErrors({});
  }, [product, mode, open]);

  const handleChange = useCallback((e) => {
    const { name, value, type, checked } = e.target;
    
    setFormData(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }));
    
    // Очищаем ошибку при изменении поля
    if (errors[name]) {
      setErrors(prev => ({ ...prev, [name]: '' }));
    }
  }, [errors]);

  const handleSelectChange = useCallback((e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: safeString(value)
    }));
    
    if (errors[name]) {
      setErrors(prev => ({ ...prev, [name]: '' }));
    }
  }, [errors]);

  const validateForm = useCallback(() => {
    const newErrors = {};
    
    if (!formData.name.trim()) {
      newErrors.name = 'Введите название товара';
    } else if (formData.name.trim().length < 2) {
      newErrors.name = 'Название должно содержать минимум 2 символа';
    }
    
    if (!formData.price.trim()) {
      newErrors.price = 'Введите цену товара';
    } else if (isNaN(Number(formData.price))) {
      newErrors.price = 'Цена должна быть числом';
    } else if (Number(formData.price) <= 0) {
      newErrors.price = 'Цена должна быть больше 0';
    }
    
    if (formData.stock.trim() !== '' && (isNaN(Number(formData.stock)) || Number(formData.stock) < 0)) {
      newErrors.stock = 'Количество должно быть неотрицательным числом';
    }
    
    if (!formData.category.trim()) {
      newErrors.category = 'Выберите категорию товара';
    }
    
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  }, [formData]);

  const handleSubmit = useCallback(() => {
    if (!validateForm()) {
      return;
    }
    
    const productData = {
      name: safeString(formData.name),
      price: safeNumber(formData.price),
      stock: safeNumber(formData.stock, 0),
      unit: safeUnit(formData.unit),
      category: safeString(formData.category),
      akticul: formData.akticul.trim() || null,
      description: formData.description.trim() || null,
      supplier: formData.supplier.trim() || null,
      isActive: safeBoolean(formData.isActive, true)
    };
    
    onSubmit(productData);
  }, [formData, validateForm, onSubmit]);

  const handleClose = () => {
    onClose();
    setFormData({
      name: '',
      price: '',
      stock: '',
      unit: 'шт',
      category: '',
      akticul: '',
      description: '',
      supplier: '',
      isActive: true
    });
    setErrors({});
  };

  // Обработка нажатия Enter для отправки формы
  const handleKeyPress = useCallback((e) => {
    if (e.key === 'Enter' && !loading) {
      handleSubmit();
    }
  }, [loading, handleSubmit]);

  // Установка обработчика нажатия клавиш
  useEffect(() => {
    if (open) {
      window.addEventListener('keydown', handleKeyPress);
      return () => {
        window.removeEventListener('keydown', handleKeyPress);
      };
    }
  }, [open, handleKeyPress]);

  return (
    <Dialog 
      open={open} 
      onClose={handleClose}
      maxWidth="lg"  // Изменено на lg для большей ширины
      fullWidth
      PaperProps={{
        sx: {
          borderRadius: 1,
          minHeight: 'auto',
          width: '900px', // Фиксированная ширина для трех полей
          maxWidth: '900px'
        }
      }}
    >
      <DialogTitle sx={{ 
        bgcolor: 'primary.main', 
        color: 'white',
        py: 2,
        px: 3
      }}>
        <Typography variant="h6" component="div" fontWeight="600">
          {mode === 'create' ? 'Добавление нового товара' : 'Редактирование товара'}
        </Typography>
      </DialogTitle>
      
      <DialogContent sx={{ py: 3, px: 3 }}>
        {loading ? (
          <Box 
            display="flex" 
            justifyContent="center" 
            alignItems="center" 
            height={400}
          >
            <Box textAlign="center">
              <CircularProgress size={60} />
              <Typography variant="body1" sx={{ mt: 2 }}>
                {mode === 'create' ? 'Добавление товара...' : 'Сохранение изменений...'}
              </Typography>
            </Box>
          </Box>
        ) : (
          <Stack spacing={3}>
            {/* Основная информация */}
            <Box>
              <Typography variant="subtitle1" fontWeight="600" gutterBottom sx={{ mb: 2, fontSize: '1.1rem' }}>
                Основная информация
              </Typography>
              <Grid container spacing={2} alignItems="flex-start">
                {/* Название товара */}
                <Grid item xs={12} md={6}>
                  <Box>
                    <Typography variant="body2" color="text.secondary" sx={{ mb: 0.5, fontWeight: 500 }}>
                      Название товара *
                    </Typography>
                    <TextField
                      fullWidth
                      name="name"
                      placeholder="Введите название товара"
                      value={formData.name}
                      onChange={handleChange}
                      error={!!errors.name}
                      disabled={loading}
                      autoFocus
                      required
                      size="small"
                      helperText={errors.name || "Обязательное поле"}
                    />
                  </Box>
                </Grid>
                
                {/* Цена */}
                <Grid item xs={12} md={3}>
                  <Box>
                    <Typography variant="body2" color="text.secondary" sx={{ mb: 0.5, fontWeight: 500 }}>
                      Цена *
                    </Typography>
                    <TextField
                      fullWidth
                      name="price"
                      placeholder="0.00"
                      type="number"
                      value={formData.price}
                      onChange={handleChange}
                      error={!!errors.price}
                      disabled={loading}
                      required
                      size="small"
                      InputProps={{
                        startAdornment: <Typography sx={{ mr: 1, color: 'text.secondary' }}>₽</Typography>,
                        inputProps: { 
                          min: 0,
                          step: 0.01
                        }
                      }}
                      helperText={errors.price || "Обязательное поле"}
                    />
                  </Box>
                </Grid>
                
                {/* Количество на складе */}
                <Grid item xs={12} md={3}>
                  <Box>
                    <Typography variant="body2" color="text.secondary" sx={{ mb: 0.5, fontWeight: 500 }}>
                      Количество на складе
                    </Typography>
                    <TextField
                      fullWidth
                      name="stock"
                      placeholder="0"
                      type="number"
                      value={formData.stock}
                      onChange={handleChange}
                      error={!!errors.stock}
                      disabled={loading}
                      size="small"
                      InputProps={{
                        inputProps: { 
                          min: 0
                        }
                      }}
                    />
                    <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5, display: 'block' }}>
                      Оставьте пустым если нет на складе
                    </Typography>
                  </Box>
                </Grid>
              </Grid>
            </Box>

            <Divider />

            {/* Характеристики товара */}
            <Box>
              <Typography variant="subtitle1" fontWeight="600" gutterBottom sx={{ mb: 2, fontSize: '1.1rem' }}>
                Характеристики товара
              </Typography>
              <Grid container spacing={2}>
                <Grid item xs={12} md={6}>
                  <Box>
                    <Typography variant="body2" color="text.secondary" sx={{ mb: 0.5, fontWeight: 500 }}>
                      Категория
                    </Typography>
                    <FormControl 
                      fullWidth 
                      error={!!errors.category}
                      size="small"
                    >
                      <Select
                        name="category"
                        value={formData.category}
                        onChange={handleSelectChange}
                        disabled={loading}
                        displayEmpty
                        sx={{ 
                          bgcolor: 'background.paper',
                          '& .MuiSelect-select': {
                            color: formData.category ? 'text.primary' : 'text.disabled'
                          }
                        }}
                      >
                        <MenuItem value="" disabled>
                          <em>Выберите категорию</em>
                        </MenuItem>
                        {CATEGORY_OPTIONS.map((category) => (
                          <MenuItem key={category.value} value={category.value}>
                            {category.label}
                          </MenuItem>
                        ))}
                      </Select>
                      {errors.category && (
                        <Typography variant="caption" color="error" sx={{ mt: 0.5, display: 'block' }}>
                          {errors.category}
                        </Typography>
                      )}
                    </FormControl>
                  </Box>
                </Grid>
                
                <Grid item xs={12} md={6}>
                  <Box>
                    <Typography variant="body2" color="text.secondary" sx={{ mb: 0.5, fontWeight: 500 }}>
                      Единица измерения
                    </Typography>
                    <FormControl fullWidth size="small">
                      <Select
                        name="unit"
                        value={formData.unit}
                        onChange={handleSelectChange}
                        disabled={loading}
                        sx={{ bgcolor: 'background.paper' }}
                      >
                        {UNIT_OPTIONS.map((option) => (
                          <MenuItem key={option.value} value={option.value}>
                            {option.label}
                          </MenuItem>
                        ))}
                      </Select>
                    </FormControl>
                  </Box>
                </Grid>
                
                <Grid item xs={12} md={6}>
                  <Box>
                    <Typography variant="body2" color="text.secondary" sx={{ mb: 0.5, fontWeight: 500 }}>
                      Артикул
                    </Typography>
                    <TextField
                      fullWidth
                      name="akticul"
                      placeholder="Введите артикул"
                      value={formData.akticul}
                      onChange={handleChange}
                      disabled={loading}
                      size="small"
                    />
                    <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5, display: 'block' }}>
                      Уникальный идентификатор товара
                    </Typography>
                  </Box>
                </Grid>
                
                <Grid item xs={12} md={6}>
                  <Box>
                    <Typography variant="body2" color="text.secondary" sx={{ mb: 0.5, fontWeight: 500 }}>
                      Поставщик
                    </Typography>
                    <TextField
                      fullWidth
                      name="supplier"
                      placeholder="Введите поставщика"
                      value={formData.supplier}
                      onChange={handleChange}
                      disabled={loading}
                      size="small"
                    />
                  </Box>
                </Grid>
              </Grid>
            </Box>

            <Divider />

            {/* Описание товара */}
            <Box>
              <Typography variant="subtitle1" fontWeight="600" gutterBottom sx={{ mb: 2, fontSize: '1.1rem' }}>
                Описание товара
              </Typography>
              <Box>
                <TextField
                  fullWidth
                  name="description"
                  placeholder="Введите описание товара"
                  value={formData.description}
                  onChange={handleChange}
                  multiline
                  rows={4}
                  disabled={loading}
                  inputProps={{ maxLength: 500 }}
                  sx={{
                    '& .MuiOutlinedInput-root': {
                      bgcolor: 'background.paper'
                    }
                  }}
                />
                <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
                  Максимум 500 символов ({formData.description.length}/500)
                </Typography>
              </Box>
            </Box>
          </Stack>
        )}
      </DialogContent>
      
      <DialogActions sx={{ 
        px: 3, 
        py: 2, 
        bgcolor: 'grey.50',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        borderTop: '1px solid',
        borderColor: 'divider'
      }}>
        {/* Переключатель "Товар активен" */}
        <FormControlLabel
          control={
            <Switch
              checked={formData.isActive}
              onChange={handleChange}
              name="isActive"
              disabled={loading}
              color="primary"
              size="medium"
            />
          }
          label={
            <Typography variant="body1" fontWeight="500">
              Товар активен
            </Typography>
          }
          sx={{ 
            m: 0,
            ml: 1
          }}
        />

        {/* Кнопки действий */}
        <Box display="flex" gap={2}>
          <Button 
            onClick={handleClose} 
            disabled={loading}
            variant="outlined"
            sx={{ 
              minWidth: 110,
              fontWeight: '500',
              color: 'text.primary',
              borderColor: 'grey.400'
            }}
          >
            Отмена
          </Button>
          <Button 
            onClick={handleSubmit} 
            variant="contained" 
            color="primary"
            disabled={loading}
            sx={{ 
              minWidth: 150,
              fontWeight: '500',
              px: 3,
              py: 1
            }}
          >
            {mode === 'create' ? 'Добавить товар' : 'Сохранить'}
          </Button>
        </Box>
      </DialogActions>
    </Dialog>
  );
};

export default ProductDialog;