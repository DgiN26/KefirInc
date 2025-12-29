import React from 'react';
import {
  Card,
  CardContent,
  CardActions,
  Typography,
  Chip,
  IconButton,
  Box,
  Badge
} from '@mui/material';
import { Edit, Delete, ShoppingCart, Inventory } from '@mui/icons-material';
import { getCategoryData } from '../../utils/constants/categories';
import { getUnitData } from '../../utils/constants/units';
import { formatPrice, getStockStatusColor, getStockStatusText } from '../../utils/helpers/productHelpers';

const ProductCard = ({
  product,
  onEdit,
  onDelete,
  onAddToCart,
  showActions = true,
  variant = 'outlined'
}) => {
  const categoryData = getCategoryData(product.category);
  const unitData = getUnitData(product.unit);
  
  const stockStatusColor = getStockStatusColor(product.stock, product.minStock);
  const stockStatusText = getStockStatusText(product.stock, product.minStock);

  return (
    <Card variant={variant} sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <CardContent sx={{ flexGrow: 1 }}>
        <Box display="flex" justifyContent="space-between" alignItems="start" mb={1}>
          <Typography variant="h6" component="div" noWrap>
            {product.name}
          </Typography>
          <Chip
            label={categoryData.label}
            size="small"
            color={categoryData.color}
            variant="outlined"
          />
        </Box>
        
        {product.description && (
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            {product.description}
          </Typography>
        )}
        
        <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
          <Typography variant="h5" color="primary">
            {formatPrice(product.price)}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            за {unitData.short}
          </Typography>
        </Box>
        
        <Box display="flex" justifyContent="space-between" alignItems="center">
          <Box display="flex" alignItems="center" gap={1}>
            <Inventory fontSize="small" color="action" />
            <Typography variant="body2">
              На складе: {product.stock} {unitData.short}
            </Typography>
          </Box>
          
          <Badge
            color={stockStatusColor}
            variant="dot"
            sx={{ '& .MuiBadge-dot': { width: 10, height: 10 } }}
          >
            <Typography variant="caption" color="text.secondary">
              {stockStatusText}
            </Typography>
          </Badge>
        </Box>
        
        {product.barcode && (
          <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 1 }}>
            Штрих-код: {product.barcode}
          </Typography>
        )}
        
        {product.supplier && (
          <Typography variant="caption" color="text.secondary" display="block">
            Поставщик: {product.supplier}
          </Typography>
        )}
      </CardContent>
      
      {showActions && (
        <CardActions sx={{ justifyContent: 'space-between', px: 2, pb: 2 }}>
          <Box>
            <IconButton
              size="small"
              onClick={() => onEdit && onEdit(product)}
              title="Редактировать"
              color="primary"
            >
              <Edit />
            </IconButton>
            <IconButton
              size="small"
              onClick={() => onDelete && onDelete(product.id)}
              title="Удалить"
              color="error"
            >
              <Delete />
            </IconButton>
          </Box>
          
          {onAddToCart && (
            <IconButton
              size="small"
              onClick={() => onAddToCart(product)}
              title="Добавить в корзину"
              color="success"
              disabled={product.stock === 0}
            >
              <ShoppingCart />
            </IconButton>
          )}
        </CardActions>
      )}
    </Card>
  );
};

export default ProductCard;