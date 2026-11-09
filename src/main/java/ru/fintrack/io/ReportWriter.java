package ru.fintrack.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Вывод текстового отчёта в терминал или в файл.
 */
public class ReportWriter {

    public void writeToConsole(String report) {
        System.out.println(report);
    }

    public Path writeToFile(String report, Path filePath) {
        try {
            Path parent = filePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(filePath, report + System.lineSeparator(), StandardCharsets.UTF_8);
            return filePath.toAbsolutePath().normalize();
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось сохранить отчёт в файл: " + e.getMessage(), e);
        }
    }
}
