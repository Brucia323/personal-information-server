package com.example.demo.models;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

// 为 File 类创建存储库。
public interface FileRepository extends JpaRepository<File, Integer> {
    
    /**
     * 查找由给定用户拥有或可供下载的所有文件。
     *
     * @param user 上传文件的用户。
     * @param openDownload 如果文件已打开以供下载，则为 true，否则为 false。
     * @return 由用户拥有或可供下载的文件列表。
     */
    List<File> findAllByUserOrOpenDownload(User user, boolean openDownload);
    
    /**
     * 它返回一个字符串列表，这些字符串是数据库中所有文件的文件类型
     *
     * @return 字符串列表
     */
    @Query("select filetype from File group by filetype")
    List<String> getFiletype();
}
