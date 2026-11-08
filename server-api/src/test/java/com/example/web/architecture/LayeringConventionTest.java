package com.example.web.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * 分层机械约定测试。
 *
 * <p>只扫描后端 main Java 源码，前端工程不在本测试范围内；
 * 测试工作目录为 server-api，因此使用相对路径 src/main/java。</p>
 */
class LayeringConventionTest {

    /**
     * 后端 main 源码根目录。
     */
    private static final Path MAIN_SOURCE_DIRECTORY = Paths.get("src/main/java");

    /**
     * Controller 直接 import mapper 的非法写法。
     */
    private static final Pattern CONTROLLER_MAPPER_IMPORT =
            Pattern.compile("import\\s+com\\.example\\.web\\.module\\..*\\.mapper\\.\\w+;");

    /**
     * Wrapper 注入原始 SQL 的非法写法；Files.exists 属于合法文件存在性判断，单独放行。
     */
    private static final Pattern RAW_SQL_WRAPPER_METHOD =
            Pattern.compile("\\.setSql\\(|\\.apply\\(|\\.inSql\\(|(?<!Files)\\.exists\\(");

    /**
     * Controller 只能使用 GET/POST，不允许出现 PUT/DELETE/PATCH 映射。
     */
    @Test
    void controllersMustNotImportMapperOrUseNonGetPostMappings() throws IOException {
        List<Path> controllerFiles = collectJavaFiles().stream()
                .filter(path -> path.getFileName().toString().endsWith("Controller.java"))
                .toList();
        assertFalse(controllerFiles.isEmpty(), "未找到任何 Controller，请确认测试工作目录为 server-api");

        for (Path controllerFile : controllerFiles) {
            String source = readSource(controllerFile);
            assertFalse(CONTROLLER_MAPPER_IMPORT.matcher(source).find(),
                    controllerFile + " 不允许直接 import mapper");
            assertFalse(source.contains("@PutMapping"), controllerFile + " 不允许使用 @PutMapping");
            assertFalse(source.contains("@DeleteMapping"), controllerFile + " 不允许使用 @DeleteMapping");
            assertFalse(source.contains("@PatchMapping"), controllerFile + " 不允许使用 @PatchMapping");
        }
    }

    /**
     * 所有 main Java 源码不得通过 Wrapper 注入原始 SQL。
     */
    @Test
    void javaSourcesMustNotInjectRawSqlIntoWrappers() throws IOException {
        List<Path> sourceFiles = collectJavaFiles();
        assertTrue(sourceFiles.size() > 0, "未找到任何 Java 源码，请确认测试工作目录为 server-api");

        for (Path sourceFile : sourceFiles) {
            String source = readSource(sourceFile);
            assertFalse(RAW_SQL_WRAPPER_METHOD.matcher(source).find(),
                    sourceFile + " 不允许使用 setSql/apply/inSql/exists 注入原始 SQL");
        }
    }

    /**
     * 收集 main Java 源码文件。
     *
     * @return Java 源文件列表
     * @throws IOException 目录不可读时抛出
     */
    private List<Path> collectJavaFiles() throws IOException {
        try (Stream<Path> stream = Files.walk(MAIN_SOURCE_DIRECTORY)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .toList();
        }
    }

    /**
     * 使用 UTF-8 读取源码文本。
     *
     * @param sourceFile 源码文件
     * @return 文件内容
     * @throws IOException 文件不可读时抛出
     */
    private String readSource(Path sourceFile) throws IOException {
        return Files.readString(sourceFile, StandardCharsets.UTF_8);
    }
}
