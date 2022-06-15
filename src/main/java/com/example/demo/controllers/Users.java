package com.example.demo.controllers;

import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.auth0.jwt.JWT;
import com.example.demo.models.User;
import com.example.demo.models.UserRepository;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.demo.utils.MiddleWare.getTokenFrom;

/**
 * 这是一个用于用户管理的 RESTful API
 */
@RestController
@RequestMapping("/api/user")
public class Users {
    
    @Resource
    UserRepository userRepository;
    
    /**
     * 它返回数据库中所有用户的列表
     *
     * @return 用户列表
     */
    @GetMapping()
    public ResponseEntity<Object> getUsers() {
        List<User> users = userRepository.findAll();
        return ResponseEntity.status(200).body(users);
    }
    
    /**
     * 该函数在数据库中创建一个新用户
     *
     * @param body          请求正文，它是一个 JSON 对象。
     * @param authorization 用户发送到服务器的令牌。
     * @return 正在返回用户对象。
     */
    @PostMapping()
    public ResponseEntity<Object> createUser(@RequestBody Map<String, ?> body,
                                             @RequestHeader("authorization") String authorization) {
        String token = getTokenFrom(authorization);
        Map<String, ?> decodedToken = JWT.decode(token).getClaims().get(
                "userForToken").asMap();
        if (decodedToken.get("id") == null) {
            Map<String, String> map = new HashMap<>();
            map.put("error", "token missing or invalid");
            return ResponseEntity.status(401).body(map);
        }
        
        String salt = BCrypt.gensalt();
        String passwordHash =
                BCrypt.hashpw(body.get("password") == null ? "123456" : (String) body.get("password"),
                        salt);
        
        User user = new User()
                .setUsername((String) body.get("username"))
                .setPasswordHash(passwordHash)
                .setNickname((String) body.get("nickname"))
                .setEmail((String) body.get("email"))
                .setPhone((String) body.get("phone"))
                .setAddress((String) body.get("address"));
        
        userRepository.saveAndFlush(user);
        return ResponseEntity.status(201).body(user);
    }
    
    /**
     * 如果用户通过身份验证，则更新用户信息
     *
     * @param id            要更新的用户id
     * @param user          在请求正文中传递的用户对象。
     * @param authorization 授权标头是标准的 HTTP 标头。它用于传递身份验证信息。
     * @return 状态码为 200 和用户对象的响应实体。
     */
    @PutMapping("/{id}")
    public ResponseEntity<Object> updateUser(@PathVariable("id") int id,
                                             @RequestBody User user,
                                             @RequestHeader("authorization") String authorization) {
        String token = getTokenFrom(authorization);
        Map<String, ?> decodedToken = JWT.decode(token).getClaims().get(
                "userForToken").asMap();
        if (decodedToken.get("id") == null) {
            Map<String, String> map = new HashMap<>();
            map.put("error", "token missing or invalid");
            return ResponseEntity.status(401).body(map);
        }
        
        User user1 = userRepository.getById(id);
        user.setPasswordHash(user1.getPasswordHash());
        
        userRepository.saveAndFlush(user);
        return ResponseEntity.status(200).body(user);
        
    }
    
    /**
     * 如果用户被授权，则删除具有给定 id 的用户
     *
     * @param id            要删除的用户id
     * @param authorization 授权标头是标准的 HTTP 标头。它用于将用户凭据（用户名和密码）发送到服务器。
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
        
        userRepository.deleteById(id);
        return ResponseEntity.status(204).build();
    }
    
    /**
     * 获取文件，保存到服务器，读取文件，保存数据到数据库，删除文件
     *
     * @param file    要上传的文件。
     * @param request 请求对象用于获取文件的路径。
     * @return 正在返回一个 ResponseEntity 对象。
     */
    @PostMapping("/import")
    public ResponseEntity<Object> importUsers(MultipartFile file, HttpServletRequest request) {
        String path = request.getServletContext().getRealPath("/");
        File filePath = new File(path + "/upload");
        
        if (!filePath.exists()) {
            filePath.mkdirs();
        }
        
        String filename = file.getOriginalFilename();
        assert filename != null;
        File dest = new File(filePath, filename);
        
        try {
            file.transferTo(dest);
            ExcelReader reader = ExcelUtil.getReader(dest);
            List<User> users = reader.readAll(User.class);
            userRepository.saveAll(users);
            dest.delete();
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
        return ResponseEntity.status(201).build();
    }
    
    /**
     * 它将用户导出到一个excel文件。
     *
     * @param request 请求对象用于获取应用程序的路径。
     * @return 响应实体
     * @deprecated
     */
    @GetMapping("/deprecated/export")
    public ResponseEntity<Object> exportUsers(HttpServletRequest request) throws FileNotFoundException {
        String path = request.getServletContext().getRealPath("/");
        File filePath = new File(path + "/download");
        
        if (!filePath.exists()) {
            filePath.mkdirs();
        }
        
        String filename = "users.xls";
        File dest = new File(filePath, filename);
        List<User> users = userRepository.findAll();
        ExcelWriter writer = ExcelUtil.getWriter(dest);
        writer.write(users, true);
        writer.close();
        File file = new File(filePath, filename);
        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
        
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.add("Content-Disposition", String.format("attachment; filename=\"%s\"", filename));
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");
        headers.add("Access-Control-Expose-Headers", "Content-Disposition");
        headers.add("content-type", "application/vnd.ms-excel;charset=UTF-8");
        
        ResponseEntity<Object> responseEntity = ResponseEntity.ok().headers(headers).contentLength(file.length()).contentType(MediaType.parseMediaType("application/octet-stream")).body(resource);
        file.delete();
        return responseEntity;
    }
    
    /**
     * 它将用户信息导出到 Excel 文件。
     *
     * @param response HttpServlet响应
     */
    @GetMapping("/export")
    public void exportUsers(HttpServletResponse response) throws IOException {
        List<User> users = userRepository.findAll();
        ExcelWriter writer = ExcelUtil.getWriter(true);
        writer.write(users, true);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
        String filename = URLEncoder.encode("用户信息", StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment;filename=" + filename + ".xlsx");
        ServletOutputStream out = response.getOutputStream();
        writer.flush(out, true);
        out.close();
        writer.close();
    }
    
    /**
     * 如果令牌有效，则返回数据库中的用户数
     *
     * @param authorization 授权标头用于对用户进行身份验证。
     * @return 数据库中的用户数。
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
        
        long count = userRepository.count();
        return ResponseEntity.status(200).body(count);
    }
    
    /**
     * 如果用户密码与新密码相同，则返回错误信息。否则，更新用户密码
     *
     * @param authorization 用户登录后获得的token
     * @param body          请求正文，它是一个包含新密码的 JSON 对象
     * @return 正在返回一个 ResponseEntity 对象。
     */
    @PutMapping("/password")
    public ResponseEntity<Object> updatePassword(@RequestHeader("authorization") String authorization,
                                                 @RequestBody Map<String, ?> body) {
        String token = getTokenFrom(authorization);
        Map<String, ?> decodedToken = JWT.decode(token).getClaims().get(
                "userForToken").asMap();
        if (decodedToken.get("id") == null) {
            Map<String, String> map = new HashMap<>();
            map.put("error", "token missing or invalid");
            return ResponseEntity.status(401).body(map);
        }
        
        User user = userRepository.getById((Integer) decodedToken.get("id"));
        boolean passwordCorrect = BCrypt.checkpw((String) body.get("password"),
                user.getPasswordHash());
        if (passwordCorrect) {
            Map<String, String> map = new HashMap<>();
            map.put("error", "新密码和旧密码相同");
            return ResponseEntity.status(200).body(map);
        } else {
            String salt = BCrypt.gensalt();
            String passwordHash = BCrypt.hashpw((String) body.get("password"), salt);
            user.setPasswordHash(passwordHash);
            userRepository.save(user);
            return ResponseEntity.status(200).build();
        }
    }
    
    /**
     * 如果令牌有效，则返回用户对象
     *
     * @param authorization 在请求标头中发送的令牌
     * @return 一个用户对象
     */
    @GetMapping("/one")
    public ResponseEntity<Object> getUser(@RequestHeader("authorization") String authorization) {
        String token = getTokenFrom(authorization);
        Map<String, ?> decodedToken = JWT.decode(token).getClaims().get(
                "userForToken").asMap();
        if (decodedToken.get("id") == null) {
            Map<String, String> map = new HashMap<>();
            map.put("error", "token missing or invalid");
            return ResponseEntity.status(401).body(map);
        }
        
        User user = userRepository.getById((Integer) decodedToken.get("id"));
        return ResponseEntity.status(200).body(user);
    }
}
