package com.example.sklad;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SkladRep extends JpaRepository<UserSkld, Integer> {

    // Поиск по артикулу
    Optional<UserSkld> findByAkticul(String akticul);

    // Поиск по категории
    List<UserSkld> findByCategory(String category);

    // Поиск по поставщику
    List<UserSkld> findBySupplier(String supplier);

    // Поиск по имени (частичное совпадение, без учета регистра)
    List<UserSkld> findByNameContainingIgnoreCase(String name);

    // Поиск товаров с низким запасом
    List<UserSkld> findByCountLessThan(Integer count);

    // Проверка существования по артикулу
    boolean existsByAkticul(String akticul);
}