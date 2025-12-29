import React from 'react';
import { Box } from '@mui/material';

export const CustomGrid = ({ 
  container = false, 
  item = false, 
  children, 
  spacing = 2,
  xs, sm, md, lg, xl,
  ...props 
}) => {
  // Контейнер Grid
  if (container) {
    return (
      <Box 
        display="flex"
        flexWrap="wrap"
        sx={{ 
          width: '100%',
          margin: `-${spacing * 4}px`,
          '& > *': {
            padding: `${spacing * 4}px`,
          }
        }}
        {...props}
      >
        {children}
      </Box>
    );
  }
  
  // Элемент Grid
  if (item) {
    const size = xl || lg || md || sm || xs || 12;
    const flexBasis = `${(size / 12) * 100}%`;
    const maxWidth = `${(size / 12) * 100}%`;
    
    return (
      <Box
        sx={{
          flexGrow: 0,
          flexShrink: 0,
          flexBasis: flexBasis,
          maxWidth: maxWidth,
          boxSizing: 'border-box'
        }}
        {...props}
      >
        {children}
      </Box>
    );
  }
  
  // Просто Box
  return <Box {...props}>{children}</Box>;
};

// Компоненты для удобства
export const GridContainer = (props) => <CustomGrid container {...props} />;
export const GridItem = (props) => <CustomGrid item {...props} />;