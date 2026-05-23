package com.arthur.gitgud.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.arthur.gitgud.domain.user.User;
import java.util.Optional;


@Repository
public interface UserRepository  extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);
}
