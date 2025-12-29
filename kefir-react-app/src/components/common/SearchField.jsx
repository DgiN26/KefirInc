import React from 'react';
import { TextField } from '@mui/material';
import { Search } from '@mui/icons-material';

const SearchField = ({
  value,
  onChange,
  placeholder = 'Поиск...',
  fullWidth = true,
  variant = 'outlined',
  size = 'medium',
  style = {},
  ...props
}) => {
  return (
    <TextField
      fullWidth={fullWidth}
      variant={variant}
      placeholder={placeholder}
      value={value}
      onChange={onChange}
      size={size}
      InputProps={{
        startAdornment: <Search style={{ marginRight: 8, color: '#666' }} />
      }}
      style={{ marginBottom: 20, ...style }}
      {...props}
    />
  );
};

export default SearchField;