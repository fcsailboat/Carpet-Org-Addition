package org.docs;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * 统计代码总行数
 */
public class CountLines {
    public static void main(String[] args) throws IOException {
        File file = new File("src/main/java/org/carpetorgaddition");
        // 直接使用System.out.println()输出可能导致控制台乱码
        PrintWriter print = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);
        print.println("总行数：" + count(file));
        print.close();
    }

    // 统计代码总行数
    private static int count(File file) throws IOException {
        if (file.isFile()) {
            // 统计文件中的代码行数
            int count = 0;
            BufferedReader reader = new BufferedReader(new FileReader(file));
            try (reader) {
                while (reader.readLine() != null) {
                    count++;
                }
            }
            return count;
        } else if (file.isDirectory()) {
            // 统计文件夹中每个文件的代码行数
            File[] files = file.listFiles();
            if (files != null) {
                int count = 0;
                for (File f : files) {
                    count += count(f);
                }
                return count;
            }
        }
        return 0;
    }
}
