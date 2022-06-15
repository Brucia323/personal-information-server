package com.example.demo.controllers;

import com.auth0.jwt.JWT;
import com.example.demo.models.FileRepository;
import com.example.demo.models.User;
import com.example.demo.models.UserRepository;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.demo.utils.MiddleWare.getTokenFrom;

/**
 * 它是一个处理文件上传和下载的控制器
 */
@RestController
@RequestMapping("/api/file")
public class Files {
    
    @Resource
    UserRepository userRepository;
    
    @Resource
    FileRepository fileRepository;
    
    /**
     * 它从授权标头中获取令牌，对其进行解码，并从解码后的令牌中获取用户 ID。如果用户 id
     * 为空，则返回错误。否则，它从数据库中获取用户，从数据库中获取用户拥有或打开下载的所有文件，并返回它们
     *
     * @param authorization 包含 JWT 令牌的授权标头。
     * @return 打开以供下载或由用户拥有的文件列表。
     */
    @GetMapping()
    public ResponseEntity<Object> getFiles(@RequestHeader("authorization") String authorization) {
        String token = getTokenFrom(authorization);
        Map<String, ?> decodedToken = JWT.decode(token).getClaims().get(
                "userForToken").asMap();
        if (decodedToken.get("id") == null) {
            Map<String, String> map = new HashMap<>();
            map.put("error", "token missing or invalid");
            return ResponseEntity.status(401).body(map);
        }
        
        User user = userRepository.getById((Integer) decodedToken.get("id"));
        List<com.example.demo.models.File> files =
                fileRepository.findAllByUserOrOpenDownload(user, true);
        return ResponseEntity.status(200).body(files);
    }
    
    /**
     * 如果用户被授权，则使用给定的 id 更新文件
     *
     * @param id            您要更新的文件的 id
     * @param file          正在更新的文件对象。
     * @param authorization 包含 JWT 令牌的授权标头。
     * @return 正在返回一个 ResponseEntity 对象。
     */
    @PutMapping("/{id}")
    public ResponseEntity<Object> updateFile(@PathVariable("id") int id,
                                             @RequestBody com.example.demo.models.File file,
                                             @RequestHeader("authorization") String authorization) {
        String token = getTokenFrom(authorization);
        Map<String, ?> decodedToken = JWT.decode(token).getClaims().get(
                "userForToken").asMap();
        if (decodedToken.get("id") == null) {
            Map<String, String> map = new HashMap<>();
            map.put("error", "token missing or invalid");
            return ResponseEntity.status(401).body(map);
        }
        
        com.example.demo.models.File file1 = fileRepository.getById(id);
        
        if (file1.getUser().getId().equals(decodedToken.get("id"))) {
            file.setUser(file1.getUser());
            fileRepository.saveAndFlush(file);
            return ResponseEntity.status(200).body(file);
        }
        
        return ResponseEntity.status(403).build();
    }
    
    /**
     * 如果用户被授权删除文件，删除它
     *
     * @param id            要删除的文件的id
     * @param authorization 包含 JWT 令牌的授权标头。
     * @return 正在返回一个 ResponseEntity 对象。
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteUser(@PathVariable("id") int id,
                                             @RequestHeader("authorization") String authorization) {
        String token = getTokenFrom(authorization);
        Map<String, ?> decodedToken = JWT.decode(token).getClaims().get(
                "userForToken").asMap();
        if (decodedToken.get("id") == null) {
            Map<String, String> map = new HashMap<>();
            map.put("error", "token missing or invalid");
            return ResponseEntity.status(401).body(map);
        }
        
        com.example.demo.models.File file = fileRepository.getById(id);
        
        if (file.getUser() == decodedToken.get("id")) {
            fileRepository.deleteById(id);
            return ResponseEntity.status(204).build();
        }
        
        return ResponseEntity.status(403).build();
    }
    
    /**
     * 它接受一个文件、一个请求和一个授权标头，并返回一个带有文件的响应实体
     *
     * @param file          要上传的文件。
     * @param request       请求对象
     * @param authorization 在请求标头中发送的令牌
     * @return 正在返回文件。
     */
    @PostMapping()
    public ResponseEntity<Object> uploadFile(MultipartFile file,
                                             HttpServletRequest request,
                                             @RequestHeader("authorization") String authorization) {
        String token = getTokenFrom(authorization);
        Map<String, ?> decodedToken = JWT.decode(token).getClaims().get(
                "userForToken").asMap();
        if (decodedToken.get("id") == null) {
            Map<String, String> map = new HashMap<>();
            map.put("error", "token missing or invalid");
            return ResponseEntity.status(401).body(map);
        }
        
        File filePath = new File(this.getClass().getResource("/").getPath(), "/file");
        
        if (!filePath.exists()) {
            filePath.mkdirs();
        }
        System.out.println(filePath);
        String filename = file.getOriginalFilename();
        assert filename != null;
        User user = userRepository.getById((Integer) decodedToken.get("id"));
        com.example.demo.models.File file1 = new com.example.demo.models.File()
                .setFilename(filename)
                .setFiletype(file.getContentType())
                .setFilesize(file.getSize())
                .setUser(user);
        fileRepository.saveAndFlush(file1);
        filename = file1.getId() + "_" + filename;
        File dest = new File(filePath, filename);
        
        try {
            file.transferTo(dest);
        } catch (IOException e) {
            e.printStackTrace();
            fileRepository.deleteById(file1.getId());
            return ResponseEntity.status(500).build();
        }
        
        return ResponseEntity.status(201).body(file1);
    }
    
    /**
     * 它获取文件id，在数据库中找到文件，然后将文件返回给用户
     *
     * @param id 你要下载的文件的id
     * @return 一份文件
     */
    @GetMapping("/{id}")
    public ResponseEntity<Object> downloadFile(@PathVariable("id") int id
    ) throws FileNotFoundException {
        File filePath = new File(this.getClass().getResource("/").getPath(), "/file");
        
        com.example.demo.models.File file = fileRepository.getById(id);
        
        String filename = file.getId() + "_" + file.getFilename();
        
        File file1 = new File(filePath, filename);
        filename = file.getFilename();
        InputStreamResource resource = new InputStreamResource(new FileInputStream(file1));
        
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.add("Content-Disposition", String.format("attachment; filename=\"%s\"", filename));
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");
        headers.add("Access-Control-Expose-Headers", "Content-Disposition");
        headers.add("content-type", "application/vnd.ms-excel;charset=UTF-8");
        
        return ResponseEntity
                .ok()
                .headers(headers)
                .contentLength(file1.length())
                .contentType(MediaType.parseMediaType("application/octet-stream"))
                .body(resource);
    }
    
    /**
     * 它从请求中获取授权标头，提取令牌，对其进行解码，然后检查令牌是否有效。如果是，则返回数据库中的文件数
     *
     * @param authorization 授权标头是标准的 HTTP 标头。它用于将用户凭据（用户名和密码）发送到服务器。
     * @return 数据库中的文件数。
     */
    @GetMapping("/count")
    public ResponseEntity<Object> getCount(@RequestHeader("authorization") String authorization) {
        String token = getTokenFrom(authorization);
        Map<String, ?> decodedToken = JWT.decode(token).getClaims().get(
                "userForToken").asMap();
        if (decodedToken.get("id") == null) {
            Map<String, String> map = new HashMap<>();
            map.put("error", "token missing or invalid");
            return ResponseEntity.status(401).body(map);
        }
        
        long count = fileRepository.count();
        return ResponseEntity.status(200).body(count);
    }
    
    /**
     * 该函数用于获取用户上传的文件的文件类型
     *
     * @param authorization 从前端发送的令牌。
     * @return 文件类型列表
     */
    @GetMapping("/type")
    public ResponseEntity<Object> getType(@RequestHeader("authorization") String authorization) {
        String token = getTokenFrom(authorization);
        Map<String, ?> decodedToken = JWT.decode(token).getClaims().get(
                "userForToken").asMap();
        if (decodedToken.get("id") == null) {
            Map<String, String> map = new HashMap<>();
            map.put("error", "token missing or invalid");
            return ResponseEntity.status(401).body(map);
        }
        
        Map<String, List<String>> map = new HashMap<>();
        List<String> list = fileRepository.getFiletype();
        map.put("filetype", list);
        return ResponseEntity.status(200).body(map);
    }
}
