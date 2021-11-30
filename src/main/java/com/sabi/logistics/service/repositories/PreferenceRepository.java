package com.sabi.logistics.service.repositories;

import com.sabi.logistics.core.models.Preference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PreferenceRepository extends JpaRepository<Preference, Long> {

    Preference findByPartnerId(Long partnerId);
}
