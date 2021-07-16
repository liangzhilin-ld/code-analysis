package com.techstar.codeanalysis.diff;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.tomcat.util.codec.binary.Base64;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import com.github.pagehelper.util.StringUtil;
import com.techstar.codeanalysis.diff.modle.ClassInfo;
import com.techstar.codeanalysis.diff.modle.MethodInfo;

/**
 * Demo class
 *
 * @author 
 * @date 2021/3/1
 */
public class ASTGenerator {
    private String javaText;
    private CompilationUnit compilationUnit;

    public ASTGenerator(String javaText) {
        this.javaText = javaText;
        this.initCompilationUnit();
    }

    /**
     * 获取AST编译单元,首次加载很慢
     */
    private void initCompilationUnit() {
        //  AST编译
        final ASTParser astParser = ASTParser.newParser(8);
        final Map<String, String> options = JavaCore.getOptions();
        JavaCore.setComplianceOptions(JavaCore.VERSION_1_8, options);
        astParser.setCompilerOptions(options);
        astParser.setKind(ASTParser.K_COMPILATION_UNIT);
        astParser.setResolveBindings(true);
        astParser.setBindingsRecovery(true);
        astParser.setStatementsRecovery(true);
        astParser.setSource(javaText.toCharArray());
        compilationUnit = (CompilationUnit) astParser.createAST(null);
    }

    /**
     * 获取java类包名
     * @return
     */
    public String getPackageName() {
        if (compilationUnit == null) {
            return "";
        }
        PackageDeclaration packageDeclaration = compilationUnit.getPackage();
        if (packageDeclaration == null){
            return "";
        }
        String packageName = packageDeclaration.getName().toString();
        return packageName;
    }

    /**
     * 获取普通类单元
     * @return
     */
    public TypeDeclaration getJavaClass() {
        if (compilationUnit == null) {
            return null;
        }
        TypeDeclaration typeDeclaration = null;
        final List<?> types = compilationUnit.types();
        for (final Object type : types) {
            if (type instanceof TypeDeclaration) {
                typeDeclaration = (TypeDeclaration) type;
                break;
            }
        }
        return typeDeclaration;
    }

    /**
     * 获取java类中所有方法
     * @return 类中所有方法
     */
    public MethodDeclaration[] getMethods() {
        TypeDeclaration typeDec = getJavaClass();
        if (typeDec == null) {
            return new MethodDeclaration[]{};
        }
        MethodDeclaration[] methodDec = typeDec.getMethods();
        return methodDec;
    }

    /**
     * 获取新增类中的所有方法信息
     * @return
     */
    public List<MethodInfo> getMethodInfoList() {
        MethodDeclaration[] methodDeclarations = getMethods();
        List<MethodInfo> methodInfoList = new ArrayList<MethodInfo>();
        for (MethodDeclaration method: methodDeclarations) {
            MethodInfo methodInfo = new MethodInfo();
            setMethodInfo(methodInfo, method);
            methodInfoList.add(methodInfo);
        }
        return methodInfoList;
    }

    /**
     * 获取修改类型的类的信息以及其中的所有方法，排除接口类
     * @param methodInfos
     * @param addLines
     * @param delLines
     * @return
     */
    public ClassInfo getClassInfo(List<MethodInfo> methodInfos, List<Integer> addLines, List<Integer> delLines) {
        TypeDeclaration typeDec = getJavaClass();
        if (typeDec == null || typeDec.isInterface()) {
            return null;
        }
        ClassInfo classInfo = new ClassInfo();
        classInfo.setClassFile(getJavaClass().getName().toString());
        classInfo.setClassName(getJavaClass().getName().toString());
        classInfo.setPackages(getPackageName());
        classInfo.setMethodInfos(methodInfos);
        classInfo.setAddLines(addLines);
        classInfo.setDelLines(delLines);
        classInfo.setType("REPLACE");
        return classInfo;
    }

    /**
     * 获取新增类型的类的信息以及其中的所有方法，排除接口类
     * @return
     */
    public ClassInfo getClassInfo() {
        TypeDeclaration typeDec = getJavaClass();
        if (typeDec == null || typeDec.isInterface()) {
            return null;
        }
        MethodDeclaration[] methodDeclarations = getMethods();
        ClassInfo classInfo = new ClassInfo();
        classInfo.setClassName(getJavaClass().getName().toString());
        classInfo.setPackages(getPackageName());
        classInfo.setType("ADD");
        List<MethodInfo> methodInfoList = new ArrayList<>();
        for (MethodDeclaration method: methodDeclarations) {
            MethodInfo methodInfo = new MethodInfo();
            setMethodInfo(methodInfo, method);
            methodInfoList.add(methodInfo);
        }
        classInfo.setMethodInfos(methodInfoList);
        return classInfo;
    }

    /**
     * 获取修改中的方法
     * @param methodDeclaration
     * @return
     */
    public MethodInfo getMethodInfo(MethodDeclaration methodDeclaration) {
        MethodInfo methodInfo = new MethodInfo();
        setMethodInfo(methodInfo, methodDeclaration);
        return methodInfo;
    }

    private void setMethodInfo(MethodInfo methodInfo,MethodDeclaration methodDeclaration) {
        methodInfo.setMd5(MD5Encode(methodDeclaration.toString()));
        methodInfo.setMethodName(methodDeclaration.getName().toString());
        methodInfo.setParameters(methodDeclaration.parameters().toString());
    }

    /**
     * 计算方法的MD5的值
     * @param s
     * @return
     */
    public static String MD5Encode(String s) {
        String MD5String = "";
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            MD5String = Base64.encodeBase64String(md5.digest(s.getBytes("utf-8")));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return MD5String;
    }

    /**
     * 判断方法是否存在
     * @param method        新分支的方法
     * @param methodsMap    master分支的方法
     * @return
     */
    public static boolean isMethodExist(final MethodDeclaration method, final Map<String, MethodDeclaration> methodsMap) {
        // 方法名+参数一致才一致
        if (!methodsMap.containsKey(method.getName().toString() + method.parameters().toString())) {
            return false;
        }
        return true;
    }

    /**
     * 判断方法是否一致
     * @param method1
     * @param method2
     * @return
     */
    public static boolean isMethodTheSame(final MethodDeclaration method1,final MethodDeclaration method2) {
//        if (MD5Encode(method1.toString()).equals(MD5Encode(method2.toString()))) {
//            return true;
//        }
        if (MD5Encode(getValidCode(method1.toString())).equals(MD5Encode(getValidCode(method2.toString())))) {
            return true;
        }
        return false;
    }
    /**
     * 去除方法空行与注释后再计算MD5值进行对比
     * @param source
     * @return
     */
    private static String getValidCode(String source){
    	Assert.isNotNull(source,"source must not be null");
    	String tempString = null;
    	StringBuffer builder = new StringBuffer();
    	ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(source.getBytes());
        InputStream inputStream = byteArrayInputStream;
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
			while((tempString = reader.readLine()) != null){
				String s=tempString;
				s=delKongHang(s);
				s=delHangHouZhuShi(s);
				if(s.isBlank())continue;
				builder.append(s);
				builder.append("\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
        return builder.toString();
    }
	/**
	 * 去掉空行
	 */
    private static String delKongHang(String str){
		if(str.trim()!= null && !str.trim().equals("")){
			return str;
		}else{
			return "";
		}
	}
	
	/**
	 * 去掉行后注释
	 * //和/*和<!--
	 */
	private static String delHangHouZhuShi(String str){
		if(str.contains("//")){
			return str.substring(0,str.indexOf("//"));
		}else if(str.contains("/*"))
		{
			return str.substring(0,str.indexOf("/*"));
		}else if(str.contains("<!--"))
		{
			return str.substring(0,str.indexOf("<!--"));
		}
		else if(str.contains("* "))
		{
			return str.substring(0,str.indexOf("* "));
		}
		else if(str.contains("*/"))
		{
			return str.substring(0,str.indexOf("*/"));
		}
		else{
			return str;
		}
	}
     
}


