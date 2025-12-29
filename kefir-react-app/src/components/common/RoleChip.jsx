import React from 'react';
import { Chip, Menu, MenuItem, Box } from '@mui/material';
import { 
  Person, 
  AdminPanelSettings, 
  LocalShipping, 
  Inventory 
} from '@mui/icons-material';

const ROLES = {
  CLIENT: { 
    label: 'Клиент', 
    color: 'primary', 
    icon: <Person fontSize="small" /> 
  },
  ADMIN: { 
    label: 'Админ', 
    color: 'error', 
    icon: <AdminPanelSettings fontSize="small" /> 
  },
  COURIER: { 
    label: 'Курьер', 
    color: 'warning', 
    icon: <LocalShipping fontSize="small" /> 
  },
  COLLECTOR: { 
    label: 'Сборщик', 
    color: 'info', 
    icon: <Inventory fontSize="small" /> 
  },
  OFFICE: { 
    label: 'Офис', 
    color: 'info', 
    icon: <Inventory fontSize="small" /> 
  }
};

const normalizeRole = (role) => {
  if (!role) return 'CLIENT';
  const upperRole = role.toUpperCase();
  return Object.keys(ROLES).includes(upperRole) ? upperRole : 'CLIENT';
};

const RoleChip = ({ 
  role, 
  clientId, 
  onChange, 
  disabled = false,
  size = 'small',
  variant = 'outlined'
}) => {
  const [anchorEl, setAnchorEl] = React.useState(null);
  const normalizedRole = normalizeRole(role);
  const roleData = ROLES[normalizedRole] || ROLES.CLIENT;

  const handleClick = (event) => {
    if (!disabled) {
      setAnchorEl(event.currentTarget);
    }
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  const handleSelect = (newRole) => {
    if (onChange && clientId) {
      onChange(clientId, 'role', newRole);
    }
    handleClose();
  };

  return (
    <>
      <Chip
        icon={roleData.icon}
        label={roleData.label}
        color={roleData.color}
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
        {Object.entries(ROLES).map(([key, roleOption]) => (
          <MenuItem
            key={key}
            onClick={() => handleSelect(key)}
            selected={key === normalizedRole}
          >
            <Box display="flex" alignItems="center" gap={1}>
              {roleOption.icon}
              {roleOption.label}
            </Box>
          </MenuItem>
        ))}
      </Menu>
    </>
  );
};

export default RoleChip;