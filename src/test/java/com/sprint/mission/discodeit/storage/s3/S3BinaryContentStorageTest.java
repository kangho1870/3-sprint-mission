package com.sprint.mission.discodeit.storage.s3;

import com.sprint.mission.discodeit.dto.data.BinaryContentDto;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.storage.S3BinaryContentStorage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.StreamUtils;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class S3BinaryContentStorageTest {

    private static S3BinaryContentStorage storage;

    @BeforeAll
    public static void setup() {
        String accessKey = System.getenv("AWS_S3_ACCESS_KEY");
        String secretKey = System.getenv("AWS_S3_SECRET_KEY");
        String region = System.getenv("AWS_S3_REGION");
        String bucket = System.getenv("AWS_S3_BUCKET");
        int presignedUrlExpiration = 600; // 초 단위

        BinaryContentRepository mockRepository = mock(BinaryContentRepository.class);

        storage = new S3BinaryContentStorage(accessKey, secretKey, region, bucket, presignedUrlExpiration, mockRepository);
    }

    @Test
    public void testUploadImageFile() throws Exception {
        // given
        String filePath = "src/test/resources/KakaoTalk_Photo_2025-06-16-10-27-10.jpeg";
        try (InputStream inputStream = new FileInputStream(filePath)) {

            MockMultipartFile mockFile = new MockMultipartFile(
                    "file",
                    "KakaoTalk_Photo_2025-06-16-10-27-10.jpeg",
                    "image/jpeg",
                    inputStream
            );

            UUID id = UUID.randomUUID();

            // when
            UUID result = storage.put(id, mockFile.getBytes());

            // then
            assertNotNull(result);
        }
    }

    @Test
    public void testGet() throws Exception {
        // given
        UUID testId = UUID.fromString("8a36f250-6e52-4ec6-9e3c-e47046d66028");

        // when & then
        try (InputStream inputStream = storage.get(testId)) {
            assertNotNull(inputStream);
            byte[] buffer = inputStream.readNBytes(100); // 일부만 읽기
            assertTrue(buffer.length > 0);
        }
    }

    @Test
    public void testDownload() throws Exception {
        // given
        BinaryContentDto dto = new BinaryContentDto(UUID.fromString("8a36f250-6e52-4ec6-9e3c-e47046d66028"), "file.jpg", null, "image/jpeg", null);

        // when
        ResponseEntity<?> response = storage.download(dto);

        // then
        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody() instanceof InputStreamResource);

        InputStreamResource resource = (InputStreamResource) response.getBody();

        byte[] bytes = resource.getInputStream().readAllBytes();

        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }
}
