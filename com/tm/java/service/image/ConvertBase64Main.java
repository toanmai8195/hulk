package com.tm.java.service.image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConvertBase64Main {
    private static final String IMG_BASE64_REGEX = "(?s)data:(.*);base64,(.*)";

    public static void main(String[] args) throws IOException {
        String runfiles = "";
        if (System.getenv("RUNFILES_DIR") == null) {
            runfiles = System.getenv("JAVA_RUNFILES");
        } else {
            runfiles = ".";
        }
        String base64Input = Files.readString(Paths.get(runfiles + "/_main/com/tm/java/service/image/image.txt"));
        toPNGBase64(base64Input);
    }

    public static void toPNGBase64(String base64Input) throws IOException {
        Matcher matcher = Pattern.compile(IMG_BASE64_REGEX).matcher(base64Input);

        if (!matcher.find()) {
            throw new IllegalArgumentException("Invalid base64 image format");
        }

        String base64Data = matcher.group(2);
        byte[] imageBytes = Base64.getDecoder().decode(base64Data);

        InputStream is = new ByteArrayInputStream(imageBytes);
        BufferedImage originalImage = ImageIO.read(is);
        if (originalImage == null) {
            throw new IllegalArgumentException("Invalid image content");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(originalImage, "png", baos);
        byte[] pngBytes = baos.toByteArray();

        String pngBase64 = Base64.getEncoder().encodeToString(pngBytes);

        System.out.println("data:image/png;base64," + pngBase64);
    }
}


