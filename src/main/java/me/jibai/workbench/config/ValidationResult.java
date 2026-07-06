package me.jibai.workbench.config;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置校验结果，收集错误与警告。
 *
 * <p>错误（error）：会导致按钮/菜单无法正常工作的问题，如 Material 不存在、slot 越界。
 * 警告（warning）：不影响加载但值得注意的问题，如依赖未启用、目标菜单暂不存在。</p>
 *
 * @author 即白
 */
public class ValidationResult {

    /** 单条问题记录。 */
    public static class Issue {
        public final String file;    // 文件名
        public final String menuId;  // 菜单 ID
        public final String buttonId; // 按钮 ID，可空
        public final String message; // 问题描述
        public final String suggest; // 建议修复

        public Issue(String file, String menuId, String buttonId, String message, String suggest) {
            this.file = file;
            this.menuId = menuId;
            this.buttonId = buttonId;
            this.message = message;
            this.suggest = suggest;
        }
    }

    private final List<Issue> errors = new ArrayList<>();
    private final List<Issue> warnings = new ArrayList<>();

    public void error(String file, String menuId, String buttonId, String message, String suggest) {
        errors.add(new Issue(file, menuId, buttonId, message, suggest));
    }

    public void warn(String file, String menuId, String buttonId, String message, String suggest) {
        warnings.add(new Issue(file, menuId, buttonId, message, suggest));
    }

    public void merge(ValidationResult other) {
        if (other == null) {
            return;
        }
        errors.addAll(other.errors);
        warnings.addAll(other.warnings);
    }

    public List<Issue> getErrors() {
        return errors;
    }

    public List<Issue> getWarnings() {
        return warnings;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public boolean isClean() {
        return errors.isEmpty() && warnings.isEmpty();
    }
}
