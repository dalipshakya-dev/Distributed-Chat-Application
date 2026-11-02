package com.project.Chat_Application.Repositories;

import com.project.Chat_Application.Entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsersRepository extends JpaRepository<User,Long> {
    Optional<User> findByUsername(String userName);
}
