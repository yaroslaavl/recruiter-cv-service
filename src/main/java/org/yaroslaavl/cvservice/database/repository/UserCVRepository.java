package org.yaroslaavl.cvservice.database.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.yaroslaavl.cvservice.database.entity.UserCV;

import java.util.UUID;

@Repository
public interface UserCVRepository extends JpaRepository<UserCV, UUID> {
}
