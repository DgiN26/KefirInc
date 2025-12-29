import React from 'react';
import { Chip, Menu, MenuItem, Box } from '@mui/material';

const EditableSelectChip = ({
  value,
  options = [],
  onChange,
  disabled = false,
  size = 'small',
  variant = 'outlined',
  getLabel = (option) => option.label,
  getColor = (option) => option.color || 'default',
  renderOption = (option) => getLabel(option),
  renderChip = (option) => getLabel(option)
}) => {
  const [anchorEl, setAnchorEl] = React.useState(null);
  
  const currentOption = options.find(opt => opt.value === value) || options[0];

  const handleClick = (event) => {
    if (!disabled) {
      setAnchorEl(event.currentTarget);
    }
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  const handleSelect = (newValue) => {
    if (onChange) {
      onChange(newValue);
    }
    handleClose();
  };

  return (
    <>
      <Chip
        label={renderChip(currentOption)}
        color={getColor(currentOption)}
        size={size}
        variant={variant}
        onClick={handleClick}
        style={{ cursor: disabled ? 'default' : 'pointer' }}
        disabled={disabled}
      />
      
      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={handleClose}
        PaperProps={{ style: { width: 200 } }}
      >
        {options.map((option) => (
          <MenuItem
            key={option.value}
            onClick={() => handleSelect(option.value)}
            selected={option.value === value}
          >
            <Box display="flex" alignItems="center" gap={1}>
              {renderOption(option)}
            </Box>
          </MenuItem>
        ))}
      </Menu>
    </>
  );
};

export default EditableSelectChip;