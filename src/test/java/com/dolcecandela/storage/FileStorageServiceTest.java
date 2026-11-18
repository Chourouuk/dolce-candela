package com.dolcecandela.storage;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class FileStorageServiceTest {

    @Test
    void detectsRealImageTypesFromMagicBytes() {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
        byte[] jpg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0};
        byte[] webp = "RIFF\0\0\0\0WEBP".getBytes(StandardCharsets.ISO_8859_1);
        assertThat(FileStorageService.detectImageExtension(png)).isEqualTo("png");
        assertThat(FileStorageService.detectImageExtension(jpg)).isEqualTo("jpg");
        assertThat(FileStorageService.detectImageExtension(webp)).isEqualTo("webp");
    }

    @Test
    void rejectsScriptsDisguisedAsImages() {
        byte[] html = "<script>alert".getBytes(StandardCharsets.UTF_8);
        byte[] php = "<?php system(".getBytes(StandardCharsets.UTF_8);
        assertThat(FileStorageService.detectImageExtension(html)).isNull();
        assertThat(FileStorageService.detectImageExtension(php)).isNull();
    }
}
