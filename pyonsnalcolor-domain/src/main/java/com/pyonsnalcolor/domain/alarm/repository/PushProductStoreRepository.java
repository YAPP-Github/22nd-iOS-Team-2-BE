package com.pyonsnalcolor.domain.alarm.repository;

import com.pyonsnalcolor.domain.alarm.PushProductStore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PushProductStoreRepository extends JpaRepository<PushProductStore, Long>{
}