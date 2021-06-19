package com.techstar.codeanalysis.diff.modle;
import java.util.List;

import lombok.Data;

@Data
public class ClassInfo {
    /**
     * java文件
     */
    private String classFile;
    /**
     * 类名
     */
    private String className;
    /**
     * 包名
     */
    private String packages;

    /**
     * 类中的方法
     */
    private List<MethodInfo> methodInfos;

    /**
     * 新增的行数
     */
    private List<Integer> addLines;

    /**
     * 删除的行数
     */
    private List<Integer> delLines;

    /**
     * 修改类型
     */
    private String type;
}

