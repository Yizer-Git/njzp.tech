package com.example.Kcsj.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.example.Kcsj.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/files")
@CrossOrigin(
        origins = "*",
        allowedHeaders = "*",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS},
        maxAge = 3600
)
public class FileController {

    @Value("${server.port}")
    private String port;

    @Value("${file.ip}")
    private String ip;

    /**
     * Upload image or other binary file.
     */
    @PostMapping("/upload")
    public Result<?> upload(MultipartFile file, HttpServletRequest request) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String flag = IdUtil.fastSimpleUUID();
        String relativeName = flag + "_" + originalFilename;
        String rootFilePath = System.getProperty("user.dir") + "/files/" + relativeName;
        File saveFile = new File(rootFilePath);
        if (!saveFile.getParentFile().exists()) {
            saveFile.getParentFile().mkdirs();
        }
        FileUtil.writeBytes(file.getBytes(), rootFilePath);

        String fileUrl = buildFileUrl(request, relativeName);
        return Result.success(fileUrl);
    }

    /**
     * Upload entry for rich text editor.
     */
    @PostMapping("/editor/upload")
    public JSON editorUpload(MultipartFile file, HttpServletRequest request) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String flag = IdUtil.fastSimpleUUID();
        String relativeName = flag + "_" + originalFilename;
        String rootFilePath = System.getProperty("user.dir") + "/files/" + relativeName;
        File saveFile = new File(rootFilePath);
        if (!saveFile.getParentFile().exists()) {
            saveFile.getParentFile().mkdirs();
        }
        FileUtil.writeBytes(file.getBytes(), rootFilePath);

        String url = buildFileUrl(request, flag);
        JSONObject json = new JSONObject();
        json.set("errno", 0);
        JSONArray arr = new JSONArray();
        JSONObject data = new JSONObject();
        arr.add(data);
        data.set("url", url);
        json.set("data", arr);
        return json;
    }

    /**
     * Serve stored file by flag or filename.
     */
    @GetMapping("/{flag}")
    public void getFiles(@PathVariable String flag, HttpServletResponse response) {
        OutputStream os;
        String basePath = System.getProperty("user.dir") + "/files/";
        List<String> fileNames = FileUtil.listFileNames(basePath);
        String fileName = fileNames.stream().filter(name -> name.contains(flag)).findAny().orElse("");
        try {
            if (StrUtil.isNotEmpty(fileName)) {
                Path filePath = Path.of(basePath + fileName);
                String contentType = Files.probeContentType(filePath);
                if (StrUtil.isEmpty(contentType)) {
                    contentType = "application/octet-stream";
                }
                response.addHeader("Content-Disposition", "inline;filename=" + URLEncoder.encode(fileName, "UTF-8"));
                response.setContentType(contentType);
                byte[] bytes = FileUtil.readBytes(filePath.toFile());
                os = response.getOutputStream();
                os.write(bytes);
                os.flush();
                os.close();
            }
        } catch (Exception e) {
            System.out.println("File download failed");
        }
    }

    private String buildFileUrl(HttpServletRequest request, String fileName) {
        String scheme = StrUtil.blankToDefault(request.getHeader("X-Forwarded-Proto"), request.getScheme());
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        String hostPort;
        if (StrUtil.isNotBlank(forwardedHost)) {
            hostPort = forwardedHost;
        } else {
            String serverName = request.getServerName();
            int serverPort = request.getServerPort();
            boolean isStandardPort = ("http".equalsIgnoreCase(scheme) && serverPort == 80)
                    || ("https".equalsIgnoreCase(scheme) && serverPort == 443);
            hostPort = isStandardPort ? serverName : serverName + ":" + serverPort;
        }

        if (StrUtil.isEmpty(hostPort)) {
            boolean isStandardPort = ("http".equalsIgnoreCase(scheme) && "80".equals(port))
                    || ("https".equalsIgnoreCase(scheme) && "443".equals(port));
            hostPort = isStandardPort ? ip : ip + ":" + port;
        }

        String contextPath = StrUtil.blankToDefault(request.getContextPath(), "");
        return scheme + "://" + hostPort + contextPath + "/files/" + fileName;
    }
}
