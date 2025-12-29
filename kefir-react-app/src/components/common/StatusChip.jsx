import React from 'react';
import { Chip, Menu, MenuItem, ListItemIcon, Box } from '@mui/material';
import { 
  CheckCircle, 
  PauseCircle,
  Block
} from '@mui/icons-material';

const STATUSES = {
  active: { 
    label: 'Активен', 
    color: 'success',
    icon: <CheckCircle fontSize="small" color="success" />
  },
  inactive: { 
    label: 'Неактивен', 
    color: 'warning',
    icon: <PauseCircle fontSize="small" color="warning" />
  },
  banned: { 
    label: 'Забанен', 
    color: 'error',
    icon: <Block fontSize="small" color="error" />
  }
};

const StatusChip = ({ 
  status, 
  clientId, 
  onChange, 
  disabled = false,
  size = 'small'
}) => {
  const [anchorEl, setAnchorEl] = React.useState(null);
  const statusData = STATUSES[status] || STATUSES.active;

  const handleClick = (event) => {
    if (!disabled) {
      setAnchorEl(event.currentTarget);
    }
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  const handleSelect = (newStatus) => {
    if (onChange && clientId) {
      onChange(clientId, 'status', newStatus);
    }
    handleClose();
  };

  return (
    <>
      <Chip
        label={
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
            {statusData.icon}
            {statusData.label}
          </Box>
        }
        color={statusData.color}
        size={size}
        onClick={handleClick}
        sx={{ 
          cursor: disabled ? 'default' : 'pointer',
          minWidth: 120,
          '& .MuiChip-label': {
            display: 'flex',
            alignItems: 'center',
            gap: 0.5
          }
        }}
        disabled={disabled}
        variant="outlined"
      />
      
      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={handleClose}
        PaperProps={{ style: { width: 180 } }}
      >
        {Object.entries(STATUSES).map(([key, statusOption]) => (
          <MenuItem
            key={key}
            onClick={() => handleSelect(key)}
            selected={key === status}
            sx={{
              backgroundColor: key === status ? 'rgba(0, 0, 0, 0.04)' : 'transparent'
            }}
          >
            <ListItemIcon>
              {statusOption.icon}
            </ListItemIcon>
            {statusOption.label}
          </MenuItem>
        ))}
      </Menu>
    </>
  );
};

export default StatusChip;