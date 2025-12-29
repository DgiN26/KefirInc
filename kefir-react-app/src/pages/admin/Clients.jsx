// pages/admin/Clients.jsx
import React, { useState, useEffect, useMemo } from 'react';
import { 
  Table, TableBody, TableCell, TableContainer, TableHead, TableRow, 
  Paper, Button, Dialog, DialogActions, DialogContent, 
  DialogTitle, IconButton, Typography, Box, Alert, Snackbar,
  InputAdornment, TextField, TableSortLabel
} from '@mui/material';
import { 
  Edit, Delete, Add, Search, Visibility, VisibilityOff,
  ArrowUpward, ArrowDownward
} from '@mui/icons-material';
import api from '../../services/api';

// Импорт компонентов
import RoleChip from '../../components/common/RoleChip';
import StatusChip from '../../components/common/StatusChip';
import { normalizeRole } from '../../utils/constants/roles';

import { 
  filterClients, 
  sortClients, 
  updateClientField,
  validateClientData,
  prepareClientData 
} from '../../utils/helpers/clientHelpers';

const Clients = () => {
  const [clients, setClients] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'info' });
  const [isSaving, setIsSaving] = useState(false);
  
  // Сортировка
  const [orderBy, setOrderBy] = useState(() => {
    return localStorage.getItem('clients_orderBy') || 'id';
  });
  const [order, setOrder] = useState(() => {
    return localStorage.getItem('clients_order') || 'asc';
  });

  // Диалог
  const [openDialog, setOpenDialog] = useState(false);
  const [currentClient, setCurrentClient] = useState(null);
  const [dialogMode, setDialogMode] = useState('create');
  const [showPassword, setShowPassword] = useState(false);
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    firstname: '',
    city: '',
    status: 'active',
    role: 'CLIENT',
    password: ''
  });

  const loadClients = async () => {
    try {
      setLoading(true);
      setError('');
      const response = await api.clientsAPI.getAll();
      setClients(response);
    } catch (err) {
      console.error('Error loading clients:', err);
      setError('Ошибка при загрузке клиентов');
      setSnackbar({
        open: true,
        message: 'Ошибка при загрузке клиентов',
        severity: 'error'
      });
      setClients([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadClients();
  }, []);

  // Сохраняем настройки сортировки
  useEffect(() => {
    localStorage.setItem('clients_orderBy', orderBy);
    localStorage.setItem('clients_order', order);
  }, [orderBy, order]);

  // Сохраняем поисковый запрос
  useEffect(() => {
    const savedSearchTerm = localStorage.getItem('clients_searchTerm');
    if (savedSearchTerm !== null) {
      setSearchTerm(savedSearchTerm);
    }
  }, []);

  useEffect(() => {
    localStorage.setItem('clients_searchTerm', searchTerm);
  }, [searchTerm]);

  // Фильтрация и сортировка
  const sortedAndFilteredClients = useMemo(() => {
    const filtered = filterClients(clients, searchTerm);
    return sortClients(filtered, orderBy, order, normalizeRole);
  }, [clients, searchTerm, orderBy, order]);

  // Обработчик изменения роли/статуса
  const handleFieldChange = async (clientId, field, value) => {
    setIsSaving(true);
    try {
      await updateClientField(clientId, field, value);
      
      // Обновляем локальное состояние
      setClients(prevClients =>
        prevClients.map(client =>
          client.id === clientId ? { ...client, [field]: value } : client
        )
      );
      
      const successMsg = field === 'role' 
        ? 'Роль успешно изменена'
        : 'Статус успешно изменен';
      
      setSnackbar({
        open: true,
        message: successMsg,
        severity: 'success'
      });
      
    } catch (err) {
      console.error(`Error changing ${field}:`, err);
      setSnackbar({
        open: true,
        message: `Ошибка при изменении ${field === 'role' ? 'роли' : 'статуса'}`,
        severity: 'error'
      });
    } finally {
      setIsSaving(false);
    }
  };

  // Диалоги
  const handleOpenCreateDialog = () => {
    setDialogMode('create');
    setFormData({
      username: '',
      email: '',
      firstname: '',
      city: '',
      status: 'active',
      role: 'CLIENT',
      password: ''
    });
    setShowPassword(false);
    setOpenDialog(true);
  };

  const handleOpenEditDialog = (client) => {
    setDialogMode('edit');
    setCurrentClient(client);
    setFormData({
      username: client.username,
      email: client.email,
      firstname: client.firstname || '',
      city: client.city || '',
      status: client.status || 'active',
      role: normalizeRole(client.role),
      password: ''
    });
    setShowPassword(false);
    setOpenDialog(true);
  };

  const handleCloseDialog = () => {
    setOpenDialog(false);
    setCurrentClient(null);
    setShowPassword(false);
  };

  const handleFormChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
  };

  const handleTogglePasswordVisibility = () => {
    setShowPassword(!showPassword);
  };

  // Обработчик сортировки
  const handleSort = (property) => {
    const isAsc = orderBy === property && order === 'asc';
    const newOrder = isAsc ? 'desc' : 'asc';
    setOrder(newOrder);
    setOrderBy(property);
  };

  const handleSaveClient = async () => {
    try {
      // Валидация
      const errors = validateClientData(formData, dialogMode === 'create');
      if (Object.keys(errors).length > 0) {
        throw new Error(Object.values(errors)[0]);
      }
      
      // Подготовка данных
      const clientData = prepareClientData(formData, dialogMode === 'create');
      
      if (dialogMode === 'create') {
        await api.clientsAPI.create(clientData);
        setSnackbar({
          open: true,
          message: 'Клиент успешно создан',
          severity: 'success'
        });
      } else {
        await api.clientsAPI.update(currentClient.id, clientData);
        setSnackbar({
          open: true,
          message: 'Клиент успешно обновлен',
          severity: 'success'
        });
      }
      
      await loadClients();
      handleCloseDialog();
    } catch (err) {
      console.error('Error saving client:', err);
      setSnackbar({
        open: true,
        message: err.message || 'Ошибка при сохранении клиента',
        severity: 'error'
      });
    }
  };

  const handleDeleteClient = async (id) => {
    if (window.confirm('Вы уверены, что хотите удалить этого клиента?')) {
      try {
        await api.clientsAPI.delete(id);
        await loadClients();
        setSnackbar({
          open: true,
          message: 'Клиент успешно удален',
          severity: 'success'
        });
      } catch (err) {
        console.error('Error deleting client:', err);
        setSnackbar({
          open: true,
          message: 'Ошибка при удалении клиента',
          severity: 'error'
        });
      }
    }
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" height="60vh">
        <Typography>Загрузка клиентов...</Typography>
      </Box>
    );
  }

  // Колонки для таблицы
  const columns = [
    { id: 'id', label: 'ID', sortable: true },
    { id: 'username', label: 'username', sortable: true },
    { id: 'firstname', label: 'Имя', sortable: true },
    { id: 'email', label: 'Email', sortable: true },
    { id: 'city', label: 'Город', sortable: true },
    { id: 'role', label: 'Роль', sortable: true },
    { id: 'status', label: 'Статус', sortable: true },
    { id: 'actions', label: 'Действия', sortable: false }
  ];

  return (
    <div>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h5">Клиенты</Typography>
        <Button
          variant="contained"
          color="primary"
          startIcon={<Add />}
          onClick={handleOpenCreateDialog}
        >
          Добавить клиента
        </Button>
      </Box>

      <TextField
        fullWidth
        variant="outlined"
        placeholder="Поиск клиентов..."
        value={searchTerm}
        onChange={(e) => setSearchTerm(e.target.value)}
        InputProps={{
          startAdornment: <Search style={{ marginRight: 8, color: '#666' }} />
        }}
        style={{ marginBottom: 20 }}
      />

      {error && (
        <Alert severity="error" style={{ marginBottom: 20 }}>
          {error}
        </Alert>
      )}

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              {columns.map((column) => (
                <TableCell
                  key={column.id}
                  sortDirection={orderBy === column.id ? order : false}
                >
                  {column.sortable ? (
                    <TableSortLabel
                      active={orderBy === column.id}
                      direction={orderBy === column.id ? order : 'asc'}
                      onClick={() => handleSort(column.id)}
                      IconComponent={orderBy === column.id ? 
                        (order === 'asc' ? ArrowUpward : ArrowDownward) : 
                        undefined
                      }
                    >
                      {column.label}
                    </TableSortLabel>
                  ) : (
                    column.label
                  )}
                </TableCell>
              ))}
            </TableRow>
          </TableHead>
          <TableBody>
            {sortedAndFilteredClients.length > 0 ? (
              sortedAndFilteredClients.map((client) => {
                return (
                  <TableRow key={client.id} hover>
                    <TableCell>{client.id}</TableCell>
                    <TableCell>{client.username}</TableCell>
                    <TableCell>{client.firstname || '-'}</TableCell>
                    <TableCell>{client.email}</TableCell>
                    <TableCell>{client.city || '-'}</TableCell>
                    
                    {/* Колонка Роль */}
                    <TableCell>
                      <RoleChip
                        role={client.role}
                        clientId={client.id}
                        onChange={handleFieldChange}
                        disabled={isSaving}
                      />
                    </TableCell>
                    
                    {/* Колонка Статус */}
                    <TableCell>
                      <StatusChip
                        status={client.status}
                        clientId={client.id}
                        onChange={handleFieldChange}
                        disabled={isSaving}
                      />
                    </TableCell>
                    
                    <TableCell>
                      <IconButton
                        size="small"
                        onClick={() => handleOpenEditDialog(client)}
                        title="Редактировать"
                        disabled={isSaving}
                      >
                        <Edit />
                      </IconButton>
                      <IconButton
                        size="small"
                        onClick={() => handleDeleteClient(client.id)}
                        title="Удалить"
                        disabled={isSaving}
                      >
                        <Delete />
                      </IconButton>
                    </TableCell>
                  </TableRow>
                );
              })
            ) : (
              <TableRow>
                <TableCell colSpan={8} align="center">
                  {searchTerm ? 'Клиенты не найдены' : 'Нет данных о клиентах'}
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Диалог создания/редактирования */}
      <Dialog open={openDialog} onClose={handleCloseDialog} maxWidth="sm" fullWidth>
        <DialogTitle>
          {dialogMode === 'create' ? 'Создание клиента' : 'Редактирование клиента'}
        </DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            margin="dense"
            name="username"
            label="username"
            type="text"
            fullWidth
            value={formData.username}
            onChange={handleFormChange}
            required
            sx={{ mt: 1 }}
          />
          <TextField
            margin="dense"
            name="email"
            label="Email"
            type="email"
            fullWidth
            value={formData.email}
            onChange={handleFormChange}
            required
          />
          
          {/* Поле пароля */}
          <TextField
            margin="dense"
            name="password"
            label={
              dialogMode === 'create' 
                ? 'Пароль *' 
                : 'Новый пароль (оставьте пустым, чтобы не менять)'
            }
            type={showPassword ? 'text' : 'password'}
            fullWidth
            value={formData.password}
            onChange={handleFormChange}
            required={dialogMode === 'create'}
            helperText={
              dialogMode === 'create' 
                ? 'Минимум 6 символов' 
                : 'Введите новый пароль или оставьте поле пустым'
            }
            InputProps={{
              endAdornment: (
                <InputAdornment position="end">
                  <IconButton
                    onClick={handleTogglePasswordVisibility}
                    edge="end"
                  >
                    {showPassword ? <VisibilityOff /> : <Visibility />}
                  </IconButton>
                </InputAdornment>
              ),
            }}
          />
          
          <TextField
            margin="dense"
            name="firstname"
            label="Имя"
            type="text"
            fullWidth
            value={formData.firstname}
            onChange={handleFormChange}
          />
          <TextField
            margin="dense"
            name="city"
            label="Город"
            type="text"
            fullWidth
            value={formData.city}
            onChange={handleFormChange}
          />
          
          {/* Поле выбора роли */}
          <TextField
            margin="dense"
            name="role"
            label="Роль"
            select
            fullWidth
            value={formData.role}
            onChange={handleFormChange}
            SelectProps={{
              native: true,
            }}
          >
            <option value="CLIENT">Клиент</option>
            <option value="ADMIN">Администратор</option>
            <option value="COURIER">Курьер</option>
            <option value="COLLECTOR">Сборщик</option>
            <option value="OFFICE">Офис</option>
          </TextField>
          
          {/* Поле статуса */}
          <TextField
            margin="dense"
            name="status"
            label="Статус"
            select
            fullWidth
            value={formData.status}
            onChange={handleFormChange}
            sx={{ mb: 1 }}
            SelectProps={{
              native: true,
            }}
          >
            <option value="active">Активен</option>
            <option value="inactive">Неактивен</option>
            <option value="banned">Забанен</option>
          </TextField>
          
          {dialogMode === 'edit' && (
            <Typography variant="caption" color="textSecondary" display="block" sx={{ mt: 1 }}>
              Примечание: При редактировании пароль будет изменён только если вы введёте новое значение.
            </Typography>
          )}
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={handleCloseDialog}>Отмена</Button>
          <Button 
            onClick={handleSaveClient} 
            variant="contained" 
            color="primary"
            disabled={
              !formData.username.trim() || 
              !formData.email.trim() || 
              (dialogMode === 'create' && !formData.password.trim()) ||
              isSaving
            }
          >
            {isSaving ? 'Сохранение...' : 'Сохранить'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Snackbar для уведомлений */}
      <Snackbar
        open={snackbar.open}
        autoHideDuration={6000}
        onClose={() => setSnackbar({ ...snackbar, open: false })}
        message={snackbar.message}
      />
    </div>
  );
};

export default Clients;