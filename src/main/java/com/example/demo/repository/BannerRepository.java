package com.example.demo.repository;

import com.example.demo.model.Banner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BannerRepository extends JpaRepository<Banner, Integer> {
    Page<Banner> findByPositionIgnoreCase(String position, Pageable pageable);

    List<Banner> findByIsActiveTrueOrderByPositionAscDisplayOrderAscCreatedAtDesc();

    List<Banner> findByPositionIgnoreCaseAndIsActiveTrueOrderByDisplayOrderAscCreatedAtDesc(String position);
}
