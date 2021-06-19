package com.techstar.codeanalysis.diff;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;

import com.techstar.codeanalysis.diff.modle.ClassInfo;
import com.techstar.codeanalysis.diff.modle.GitAdapter;
import com.techstar.codeanalysis.diff.modle.MethodInfo;

/**
 * 不同分支进行对比
 *
 * @author 
 * @date 2021/3/1
 */
public class CodeDiff {

    /**
     * 分支和分支之间的比较
     * @param newBranchName 当前分支取最新的commit版本
     * @param oldBranchName 比较分支取最新的commit版本
     * @return
     */
    public static List<ClassInfo> diffBranchToBranch(GitAdapter gitAdapter, String newBranchName, String oldBranchName) {
        return diffBranchToBranch(gitAdapter, newBranchName, oldBranchName,null);
    }

    /**
     * 当前分支与某个分支的某个版本进行比较
     * @param gitAdapter
     * @param newBranchName 当前分支当前分支取最新的commit版本
     * @param oldBranchName 比较分支
     * @param commitId 比较分支的commit版本Id
     * @return
     */
    public static List<ClassInfo> diffBranchToBranch(GitAdapter gitAdapter, String newBranchName, String oldBranchName, String commitId) {
        List<ClassInfo> classInfos = diffMethods(gitAdapter, newBranchName, oldBranchName,commitId);
        return classInfos;
    }

    private static List<ClassInfo> diffMethods(GitAdapter gitAdapter,String newBranchName, String oldBranchName,String commitId) {
        try {
            //  获取Git
            Git git = gitAdapter.initGit();

            //  获取两个分支的最新代码信息
            Ref newBranchRef = gitAdapter.checkOutAndPull(newBranchName);
            Ref oldBranchRef = gitAdapter.checkOutAndPull(oldBranchName);

            //  获取当前分支信息
            AbstractTreeIterator newTreeParser = gitAdapter.prepareTreeParser(newBranchRef.getObjectId().getName());

            //  获取比较分支的版本信息 如果commit为null，则默认取比较分支的最新版本信息
            if(null == commitId && oldBranchRef!=null) {
                commitId = oldBranchRef.getObjectId().getName();
            }
            AbstractTreeIterator oldTreeParser = gitAdapter.prepareTreeParser(commitId);

            //  对比差异
            List<DiffEntry> diffs = git.diff().setOldTree(oldTreeParser).setNewTree(newTreeParser).setShowNameAndStatusOnly(true).call();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DiffFormatter df = new DiffFormatter(out);
            //  设置比较器为忽略空白字符对比（Ignores all whitespace）
            df.setDiffComparator(RawTextComparator.WS_IGNORE_ALL);
            df.setRepository(git.getRepository());
            List<ClassInfo> allClassInfos = batchPrepareDiffMethod(gitAdapter, newBranchName, oldBranchName, df, diffs);
            return allClassInfos;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<ClassInfo>();
    }

    /**
     * 多线程执行对比
     *
     * @return
     */
    private static List<ClassInfo> batchPrepareDiffMethod(final GitAdapter gitAdapter, final String branchName, final String oldBranchName, final DiffFormatter df, List<DiffEntry> diffs) {
        int threadSize = 100;
        int dataSize = diffs.size();
        int threadNum = dataSize / threadSize + 1;
        boolean special = dataSize % threadSize == 0;
        ExecutorService executorService = Executors.newFixedThreadPool(threadNum);

        List<Callable<List<ClassInfo>>> tasks = new ArrayList<Callable<List<ClassInfo>>>();
        Callable<List<ClassInfo>> task = null;
        List<DiffEntry> cutList = null;
        //  分解每条线程的数据
        for (int i = 0; i < threadNum; i++) {
            if (i == threadNum - 1) {
                if (special) {
                    break;
                }
                cutList = diffs.subList(threadSize * i, dataSize);
            } else {
                cutList = diffs.subList(threadSize * i, threadSize * (i + 1));
            }
            final List<DiffEntry> diffEntryList = cutList;
            task = new Callable<List<ClassInfo>>() {
                @Override
                public List<ClassInfo> call() throws Exception {
                    List<ClassInfo> allList = new ArrayList<ClassInfo>();
                    for (DiffEntry diffEntry : diffEntryList) {
                        ClassInfo classInfo = prepareDiffMethod(gitAdapter, branchName, oldBranchName, df, diffEntry);
                        if (classInfo != null) {
                            allList.add(classInfo);
                        }
                    }
                    return allList;
                }
            };
            // 这里提交的任务容器列表和返回的Future列表存在顺序对应的关系
            tasks.add(task);
        }
        List<ClassInfo> allClassInfoList = new ArrayList<ClassInfo>();
        try {
            List<Future<List<ClassInfo>>> results = executorService.invokeAll(tasks);
            //结果汇总
            for (Future<List<ClassInfo>> future : results) {
                allClassInfoList.addAll(future.get());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 关闭线程池
            executorService.shutdown();
        }
        return allClassInfoList;
    }

    /**
     * 单个差异文件对比
     *
     * @param gitAdapter
     * @param branchName
     * @param oldBranchName
     * @param df
     * @param diffEntry
     * @return
     */
    private synchronized static ClassInfo prepareDiffMethod(GitAdapter gitAdapter, String branchName, String oldBranchName, DiffFormatter df, DiffEntry diffEntry) {
        List<MethodInfo> methodInfoList = new ArrayList<MethodInfo>();
        try {
            String newJavaPath = diffEntry.getNewPath();
//            if(newJavaPath.equals("vbiServer/license-authentication/src/main/java/com/techstar/license/ex/FileUtils.java")) {
//            	boolean fls=true;
//            }
            //  排除测试类
            if (newJavaPath.contains("/src/test/java/")) {
                return null;
            }
            //  非java文件 和 删除类型不记录
            if (!newJavaPath.endsWith(".java") || diffEntry.getChangeType() == DiffEntry.ChangeType.DELETE) {
                return null;
            }
            String newClassContent = gitAdapter.getBranchSpecificFileContent(branchName, newJavaPath);
            
            ASTGenerator newAstGenerator = new ASTGenerator(newClassContent);
            /*  新增类型   */
            if (diffEntry.getChangeType() == DiffEntry.ChangeType.ADD) {
                return newAstGenerator.getClassInfo();
            }
            /*  修改类型  */
            //  获取文件差异位置，从而统计差异的行数，如增加行数，减少行数
            FileHeader fileHeader = df.toFileHeader(diffEntry);
            List<int[]> addLines = new ArrayList<int[]>();
            List<int[]> delLines = new ArrayList<int[]>();
            EditList editList = fileHeader.toEditList();
            for (Edit edit : editList) {
                if (edit.getLengthA() > 0) {
                    delLines.add(new int[]{edit.getBeginA(), edit.getEndA()});
                }
                if (edit.getLengthB() > 0) {
                    addLines.add(new int[]{edit.getBeginB(), edit.getEndB()});
                }
            }
            String oldJavaPath = diffEntry.getOldPath();
            String oldClassContent = gitAdapter.getBranchSpecificFileContent(oldBranchName, oldJavaPath);
            ASTGenerator oldAstGenerator = new ASTGenerator(oldClassContent);
            MethodDeclaration[] newMethods = newAstGenerator.getMethods();
            MethodDeclaration[] oldMethods = oldAstGenerator.getMethods();
            Map<String, MethodDeclaration> methodsMap = new HashMap<String, MethodDeclaration>();
            for (int i = 0; i < oldMethods.length; i++) {
                methodsMap.put(oldMethods[i].getName().toString() + oldMethods[i].parameters().toString(), oldMethods[i]);
            }
            for (final MethodDeclaration method : newMethods) {
                // 如果方法名是新增的,则直接将方法加入List
                if (!ASTGenerator.isMethodExist(method, methodsMap)) {
                    MethodInfo methodInfo = newAstGenerator.getMethodInfo(method);
                    methodInfoList.add(methodInfo);
                    continue;
                }
                // 如果两个版本都有这个方法,则根据MD5判断方法是否一致
                if (!ASTGenerator.isMethodTheSame(method, methodsMap.get(method.getName().toString() + method.parameters().toString()))) {
                    MethodInfo methodInfo = newAstGenerator.getMethodInfo(method);
                    methodInfoList.add(methodInfo);
                }
            }
            return newAstGenerator.getClassInfo(methodInfoList, translate(addLines), translate(delLines));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 将修改文件的行数信息 int[] 转化为list类型
     * diff返回的int[] 表示修改的区间，修改区间是左开右闭（ 】
     * @param listInt
     * @return
     */
    public static List<Integer> translate(List<int[]> listInt) {
        List<Integer> list = new ArrayList<>();
        for(int[] ints:listInt) {
            for(int i=ints[0];i<ints[1];i++) {
                list.add(i+1);
            }
        }
        return list;
    }
}


