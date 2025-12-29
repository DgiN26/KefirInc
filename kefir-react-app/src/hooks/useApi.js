import { useState, useEffect, useCallback } from 'react';

const useApi = (apiFunction, initialData = null, immediate = true) => {
  const [data, setData] = useState(initialData);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [called, setCalled] = useState(false);

  const execute = useCallback(async (...args) => {
    try {
      setLoading(true);
      setError(null);
      const result = await apiFunction(...args);
      setData(result);
      setCalled(true);
      return result;
    } catch (err) {
      setError(err);
      throw err;
    } finally {
      setLoading(false);
    }
  }, [apiFunction]);

  useEffect(() => {
    if (immediate && !called) {
      execute();
    }
  }, [execute, immediate, called]);

  return {
    data,
    loading,
    error,
    execute,
    called
  };
};

export default useApi;