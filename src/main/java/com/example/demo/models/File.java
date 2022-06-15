package com.example.demo.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.*;

/**
 * 文件是属于用户的文件
 */
@Getter
@Setter
@Accessors(chain = true)
@Entity
public class File {
    
    @Id
    @GeneratedValue
    private Integer id;
    
    @Column(nullable = false)
    private String filename;
    
    private String filetype;
    
    private Long filesize;
    
    private boolean openDownload;
    
    @ManyToOne(cascade = CascadeType.MERGE)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;
}
