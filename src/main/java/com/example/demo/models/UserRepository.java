package com.example.demo.models;

import org.springframework.data.jpa.repository.JpaRepository;

// 为 User 类创建存储库。
public interface UserRepository extends JpaRepository<User, Integer> {
    
    /**
     * 通过用户名查找用户。
     *
     * @param username 要查找的用户的用户名。
     * @return 一个用户对象
     */
    User findByUsername(String username);
}
