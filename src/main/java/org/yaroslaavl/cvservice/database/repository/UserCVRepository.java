package org.yaroslaavl.cvservice.database.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.yaroslaavl.cvservice.database.entity.UserCV;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserCVRepository extends JpaRepository<UserCV, UUID> {

    long countByUserId(String userId);

    Optional<UserCV> findByFilePath(String filePath);

    Optional<UserCV> findByIsMainAndUserId(Boolean isMain, String userId);

    List<UserCV> findAllByUserId(String userId);
}
