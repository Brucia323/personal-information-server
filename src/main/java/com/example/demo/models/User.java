package com.example.demo.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.util.Collection;

/**
 * 这是一个代表用户的类
 */
@Getter
@Setter
@Accessors(chain = true)
@Entity
@JsonIgnoreProperties(value = {"hibernateLazyInitializer"})
public class User {
    
    @Id
    @GeneratedValue()
    private Integer id;
    
    @Column(nullable = false, unique = true)
    private String username;
    
    @Column(nullable = false)
    @JsonIgnore
    private String passwordHash;
    
    private String nickname;
    
    private String email;
    
    private String phone;
    
    private String address;
    
    @OneToMany(mappedBy = "user")
    @JsonIgnore
    private Collection<File> file;
}
