package com.sprint.mission.discodeit.storage.s3;

import com.sprint.mission.discodeit.util.EnvLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Properties;

public class AWSS3Test {

    static Properties env;
    static S3Client s3;
    static S3Presigner presigner;
    static String bucket;

    @BeforeAll
    public static void setup() {
        // Load .env
        EnvLoader.loadToSystemProperties(".env");
        String accessKey = System.getProperty("AWS_S3_ACCESS_KEY");
        String secretKey = System.getProperty("AWS_S3_SECRET_KEY");

        AwsBasicCredentials creds = AwsBasicCredentials.create(accessKey, secretKey);

        bucket = System.getProperty("AWS_S3_BUCKET");

        s3 = S3Client.builder()
                .region(Region.of(System.getProperty("AWS_S3_REGION")))
                .credentialsProvider(StaticCredentialsProvider.create(creds))
                .build();

        presigner = S3Presigner.builder()
                .region(Region.of(System.getProperty("AWS_S3_REGION")))
                .credentialsProvider(StaticCredentialsProvider.create(creds))
                .build();
    }

    @Test
    public void uploadFile() {
        String key = "test/test-upload.txt";
        File file = new File("src/test/resources/sample.txt");

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        s3.putObject(request, file.toPath());

        System.out.println("업로드 성공: " + key);
    }

    @Test
    public void generatePresignedUrl() {
        String key = "test/test-upload.txt";

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);

        URL url = presignedRequest.url();
        System.out.println("Presigned URL: " + url);
    }

    @Test
    void downloadFile() throws IOException {
        // S3 내 저장된 경로
        String key = "test/test-upload.txt";

        // 다운로드 요청
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        // 응답 스트림
        try (ResponseInputStream<GetObjectResponse> response = s3.getObject(getRequest);
             FileOutputStream output = new FileOutputStream("downloaded_" + Paths.get(key).getFileName())) {

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = response.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }

            System.out.println("다운로드 완료: downloaded_" + Paths.get(key).getFileName());
        }
    }
}
