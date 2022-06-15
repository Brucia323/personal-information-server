package com.example.demo.controllers;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.example.demo.models.User;
import com.example.demo.models.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * 它接受用户名和密码，检查密码是否正确，如果正确，则返回 JWT 令牌
 */
@RestController
@RequestMapping("/api/login")
public class Login {
    
    @Resource
    UserRepository userRepository;
    
    @Autowired
    Environment env;
    
    /**
     * 如果用户名和密码正确，则创建一个令牌并将其返回给用户
     *
     * @param body 请求的正文。
     * @return 带有令牌、用户名和昵称的地图。
     */
    @PostMapping()
    public ResponseEntity<Object> login(@RequestBody Map<String, ?> body) {
        User user = userRepository.findByUsername((String) body.get("username"));
        boolean passwordCorrect = user != null && BCrypt.checkpw((String) body.get(
                "password"), user.getPasswordHash());
        
        if (user == null || !passwordCorrect) {
            Map<String, String> map = new HashMap<>();
            map.put("error", "invalid username or password");
            return ResponseEntity.status(401).body(map);
        }
        
        Map<String, Object> map = new HashMap<>();
        map.put("username", user.getUsername());
        map.put("id", user.getId());
        
        String token =
                JWT.create().withClaim("userForToken", map).sign(Algorithm.HMAC256(env.getProperty("SECRET")));
        
        map.clear();
        map.put("token", token);
        map.put("username", user.getUsername());
        map.put("nickname", user.getNickname());
        
        return ResponseEntity.status(200).body(map);
    }
}
