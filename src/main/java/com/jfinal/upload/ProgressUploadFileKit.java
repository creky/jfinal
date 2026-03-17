package com.jfinal.upload;

import com.jfinal.kit.StrKit;
import org.apache.commons.fileupload2.core.FileItem;
import org.apache.commons.fileupload2.core.FileItemFactory;
import org.apache.commons.fileupload2.core.ProgressListener;
import org.apache.commons.fileupload2.core.DiskFileItemFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.fileupload2.jakarta.servlet6.JakartaServletFileUpload;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * 获取上传文件 进度显示工具类
 * @author 山东小木
 */
public class ProgressUploadFileKit {
    public static UploadFile get(HttpServletRequest request,String parameterName, String uploadPath, Consumer<UploadProgress> callback){
        // 检查请求是否包含文件上传
        if (!JakartaServletFileUpload.isMultipartContent(request)) {
            return null;
        }
        String finalUploadPath = getFinalPath(uploadPath);
        //创建不存在的文件夹
        createNotExistsFolder(finalUploadPath);
        UploadFile progressFile = null;
        // 创建文件项工厂
        FileItemFactory factory = DiskFileItemFactory.builder().get();
        // 创建上传处理器
        JakartaServletFileUpload upload = new JakartaServletFileUpload(factory);
        if(callback != null) {
            // 创建进度监听器
            ProgressListener progressListener = new ProgressListener() {
                @Override
                public void update(long bytesRead, long contentLength, int items) {
                    callback.accept(new UploadProgress(items, contentLength, bytesRead));
                }
            };
            // 将进度监听器添加到上传处理器
            upload.setProgressListener(progressListener);
        }
        try {
            List<FileItem> formItems = upload.parseRequest(request);
            if (formItems != null && !formItems.isEmpty()) {
                FileItem fileItem = null;
                if (StrKit.isBlank(parameterName)) {
                    fileItem = formItems.stream().filter(item -> !item.isFormField()).findFirst().orElse(null);
                } else {
                    fileItem = formItems.stream().filter(item -> (!item.isFormField() && parameterName.equals(item.getFieldName()))).findFirst().orElse(null);
                }
                if (fileItem != null) {
                    // 处理上传的文件
                    String originFileName = fileItem.getName();
                    //判断如果是安全文件 才写入磁盘
                    if(isSafeFile(originFileName)){
                        String newFileName = ProgressUploadFileConfig.getRenameFunc().call(finalUploadPath, originFileName);
                        Path filePath = FileSystems.getDefault().getPath(finalUploadPath, newFileName);
                        fileItem.write(filePath);
                        progressFile = new UploadFile(parameterName, finalUploadPath, newFileName, originFileName, fileItem.getContentType());
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return progressFile;
    }

    /**
     * 判断是否是安全文件
     * @param fileName
     * @return
     */
    private static boolean isSafeFile(String fileName) {
        fileName = fileName.trim().toLowerCase();
        return !fileName.endsWith(".jsp") && !fileName.endsWith(".jspx");
    }

    /**
     * 创建出不存在的路径
     * @param finalUploadPath
     */
    private static void createNotExistsFolder(String finalUploadPath) {
        File dir = new File(finalUploadPath);
        if ( !dir.exists()) {
            if (!dir.mkdirs()) {
                throw new RuntimeException("Directory " + finalUploadPath + " not exists and can not create directory.");
            }
        }
    }

    /**
     * 路径允许为 "" 值，表示直接使用基础路径 baseUploadPath
     */
    private static String getFinalPath(String uploadPath) {
        if (uploadPath == null) {
            return UploadConfig.baseUploadPath;
        }

        uploadPath = uploadPath.trim();
        if (uploadPath.startsWith("/") || uploadPath.startsWith("\\")) {
            if (UploadConfig.baseUploadPath.equals("/")) {
                return uploadPath;
            } else {
                return UploadConfig.baseUploadPath + uploadPath;
            }
        } else {
            return UploadConfig.baseUploadPath + File.separator + uploadPath;
        }
    }
}
