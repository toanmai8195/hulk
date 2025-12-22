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
    public static int genPartition(String key, int numberInstant) {
        int hash = key.hashCode();
        int partition = (hash == Integer.MIN_VALUE) ? 0 : Math.abs(hash) % numberInstant;

        return partition;
    }

    public static void main(String[] args) {
        System.out.println(genPartition("ea+Rewards_segment_lotusmiles_dam_xu_2025+700194",3));
    }
}


