package com.example.demo.repo;

import com.example.demo.model.UserTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RestController;

@Repository
public interface UserTestRepo extends JpaRepository<UserTest,Long> {
}
