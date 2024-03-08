package org.msa.ocr_porject.controller;


import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

@Controller
public class TestController {

    @Value("${ocr.api.url}")
    private String apiURL;

    @Value("${ocr.secret.key}")
    private String secretKey;

    @GetMapping("/test")
    public String testPage() {
        return "index";
    }

    @PostMapping("/test")
    public String handleImageUpload(@RequestPart("imageFile") MultipartFile image2) {
        // 여기서 이미지를 처리하는 로직을 추가하면 됩니다.
        if (image2 != null) {
            try {
                URL url = new URL(apiURL);
                HttpURLConnection con = (HttpURLConnection)url.openConnection();
                con.setUseCaches(false);
                con.setDoInput(true);
                con.setDoOutput(true);
                con.setReadTimeout(30000);
                con.setRequestMethod("POST");
                String boundary = "----" + UUID.randomUUID().toString().replaceAll("-", "");
                con.setRequestProperty("Content-Type", "application/json");
                con.setRequestProperty("X-OCR-SECRET", secretKey);

                JSONObject json = new JSONObject();
                json.put("version", "V2");
                json.put("requestId", UUID.randomUUID().toString());
                json.put("timestamp", System.currentTimeMillis());
                JSONObject image = new JSONObject();
                image.put("format", "jpg");
                image.put("name", "demo");
                JSONArray images = new JSONArray();
                images.put(image);
                json.put("images", images);
                String postParams = json.toString();

                con.connect();
                DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                long start = System.currentTimeMillis();
                File file = new File("http://img.dt.co.kr/images/Antbot_300x140.png");
                writeMultiPart(wr, postParams, file, boundary);
                wr.close();

                int responseCode = con.getResponseCode();
                BufferedReader br;
                if (responseCode == 200) {
                    br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                } else {
                    br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
                }
                String inputLine;
                StringBuffer response = new StringBuffer();
                while ((inputLine = br.readLine()) != null) {
                    response.append(inputLine);
                }
                br.close();

                System.out.println(response);
            } catch (Exception e) {
                System.out.println(e);
            }

        }
        // 예를 들어, 이미지를 저장하거나 처리하는 비즈니스 로직을 수행할 수 있습니다.
        // 이 예제에서는 단순히 콘솔에 파일 이름을 출력하도록 했습니다.
        System.out.println("Received image: " + image2.getOriginalFilename());
        return "index"; // 업로드 후 다시 index 페이지로 리다이렉션
    }

    private static void writeMultiPart(OutputStream out, String jsonMessage, File file, String boundary) throws
            IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("--").append(boundary).append("\r\n");
        sb.append("Content-Disposition:form-data; name=\"message\"\r\n\r\n");
        sb.append(jsonMessage);
        sb.append("\r\n");

        out.write(sb.toString().getBytes("UTF-8"));
        out.flush();

        if (file != null && file.isFile()) {
            out.write(("--" + boundary + "\r\n").getBytes("UTF-8"));
            StringBuilder fileString = new StringBuilder();
            fileString
                    .append("Content-Disposition:form-data; name=\"file\"; filename=");
            fileString.append("\"" + file.getName() + "\"\r\n");
            fileString.append("Content-Type: application/octet-stream\r\n\r\n");
            out.write(fileString.toString().getBytes("UTF-8"));
            out.flush();

            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = fis.read(buffer)) != -1) {
                    out.write(buffer, 0, count);
                }
                out.write("\r\n".getBytes());
            }

            out.write(("--" + boundary + "--\r\n").getBytes("UTF-8"));
        }
        out.flush();
    }

    @GetMapping("/hi")
    public String hi() {
        return "hi";
    }
}
