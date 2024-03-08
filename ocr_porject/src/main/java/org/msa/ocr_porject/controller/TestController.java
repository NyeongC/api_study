package org.msa.ocr_porject.controller;


import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Controller
@Slf4j
public class TestController {

    @Value("${ocr.api.url}")
    private String apiURL;

    @Value("${ocr.secret.key}")
    private String secretKey;

    private String imageFile ="http://img.dt.co.kr/images/K-VaRam_300x140.png";

    @GetMapping("/test")
    public String testPage() {
        return "index";
    }

    @PostMapping("/test")
    public Map<String, Object> handleImageUpload(@RequestPart("imageFile") MultipartFile multipartFile) {
        Map<String, Object> rtnMap = new HashMap<>();

        if (multipartFile != null) {

            BufferedReader br = null;
            DataOutputStream wr = null;
            HttpURLConnection con = null;

            try {

                String orgFileName = multipartFile.getOriginalFilename();
                int index = orgFileName.lastIndexOf(".");
                String fileExt = orgFileName.substring(index + 1);
                String fileName = multipartFile.getName();

                URL url = new URL(apiURL);

                con = fnTempHttpsSsl(url.getProtocol());    // LOCAL 에서 테스트 할 경우 주석해제

                con = (HttpURLConnection)url.openConnection();
                con.setUseCaches(false);
                con.setDoInput(true);
                con.setDoOutput(true);
                con.setReadTimeout(10000);
                con.setRequestMethod("POST");

                String boundary = "----" + UUID.randomUUID().toString().replaceAll("-", "");
                con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                con.setRequestProperty("X-OCR-SECRET", secretKey);

                // NAVER OCR 요청 파라미터
                JSONObject json = new JSONObject();
                json.put("version", "V2");
                json.put("requestId", UUID.randomUUID().toString());
                json.put("timestamp", System.currentTimeMillis());
                JSONObject image = new JSONObject();
                image.put("format", fileExt);
                image.put("name", fileName);
                JSONArray images = new JSONArray();
                images.put(image);
                json.put("images", images);
                String postParams = json.toString();

                // NAVER OCR API 호출
                con.connect();
                wr = new DataOutputStream(con.getOutputStream());

                StringBuilder sb = new StringBuilder();
                sb.append("--").append(boundary).append("\r\n");
                sb.append("Content-Disposition:form-data; name=\"message\"\r\n\r\n");
                sb.append(postParams);
                sb.append("\r\n");

                wr.write(sb.toString().getBytes("UTF-8"));

                // NAVER OCR 사업자등록증 파일 전송
                wr.write(("--" + boundary + "\r\n").getBytes("UTF-8"));
                StringBuilder fileString = new StringBuilder();
                fileString.append("Content-Disposition:form-data; name=\"file\"; filename=");
                fileString.append("\"" + fileName + "." + fileExt + "\"\r\n");
                fileString.append("Content-Type: application/octet-stream\r\n\r\n");
                wr.write(fileString.toString().getBytes("UTF-8"));

                byte[] buffer = new byte[8192];
                int count;
                InputStream inputStream = new BufferedInputStream(multipartFile.getInputStream());

                while ((count = inputStream.read(buffer)) != -1) {
                    wr.write(buffer);
                }

                wr.write("\r\n".getBytes());
                wr.write(("--" + boundary + "--\r\n").getBytes("UTF-8"));
                wr.flush();

                // NAVER OCR 응답 처리
                int responseCode = con.getResponseCode();

                if (responseCode == 200) {
                    br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                } else {
                    rtnMap.put("msg", responseCode);
                    return rtnMap;
                }

                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = br.readLine()) != null) {
                    response.append(inputLine);
                }


                rtnMap.put("msg", "SUCCESS");
                rtnMap.put("bizString", response.toString());

                // 사업자 구분 코드 추가 처리 (처리 못하면 빈값 return)
                //Map<String, Object> bizerMap = fnBizerSctPrc(response.toString());

                //rtnMap.put("bizerSctCd", bizerMap);

            } catch (Exception e) {
                log.error(e.getMessage());
                rtnMap.put("msg", e.getMessage());
            } finally {
                try {
                    if (br != null) br.close();
                    if (wr != null) wr.close();
                    if (con != null) con.disconnect();
                } catch (Exception e) {
                    log.error(e.getMessage());
                    rtnMap.put("msg", e.getMessage());
                }
            }

        } else {
            rtnMap.put("msg", "사업자등록증을 확인해주세요.");
        }

        return rtnMap;
    }

    public static HttpURLConnection fnTempHttpsSsl(String protocol) throws Exception{
        HttpURLConnection connection = null;

        try{
            if(protocol.toLowerCase().equals("https")){
                TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    @SuppressWarnings("unused")
                    public void checkClientTrusted1(X509Certificate[] certs, String authType) {
                    }
                    @SuppressWarnings("unused")
                    public void checkServerTrusted1(X509Certificate[] certs,String authType) {
                    }
                    @Override
                    public void checkClientTrusted(
                            java.security.cert.X509Certificate[] arg0,
                            String arg1) throws CertificateException {
                    }
                    @Override
                    public void checkServerTrusted(
                            java.security.cert.X509Certificate[] arg0,
                            String arg1) throws CertificateException {
                    }
                } };

                SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null, trustAllCerts, new java.security.SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            }
        } catch (Exception e){
            throw new Exception(e.getMessage());
        }

        return connection;
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
