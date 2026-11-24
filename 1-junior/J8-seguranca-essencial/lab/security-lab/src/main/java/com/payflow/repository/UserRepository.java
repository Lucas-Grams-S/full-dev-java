package com.payflow.repository;

import com.payflow.domain.User;
import java.util.Optional;

public interface UserRepository {
    Optional<User> findByEmail(String email);
}
