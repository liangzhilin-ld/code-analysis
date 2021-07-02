package com.techstar.codeanalysis;

import java.io.IOException;
import java.util.List;

import org.eclipse.jgit.api.errors.GitAPIException;

import com.techstar.codeanalysis.diff.CodeDiff;
import com.techstar.codeanalysis.diff.CodeDiffCommit;
import com.techstar.codeanalysis.diff.modle.ClassInfo;
import com.techstar.codeanalysis.diff.modle.CommitMessage;
import com.techstar.codeanalysis.diff.modle.GitAdapter;

/**
 * Demo class
 *
 * @author bigman
 * @date 2021/3/1
 */
public class TestDemo {
	//远程库路径
    public static String remotePath = "http://114.255.128.177:82/VBI/vbiServer.git";
    //下载已有仓库到本地路径
    public static String localPath = "E:/test";
    public static String branchName = "security-huawei";
    public static String projectName = "vbi";
    public static void main(String[] args) throws GitAPIException, IOException {
        String filePath = TestDemo.localPath+"/"+TestDemo.projectName;
        // 初始化adapter
        GitAdapter gitAdapter = new GitAdapter(TestDemo.remotePath,filePath,TestDemo.branchName);

        // 获取当前分支的所有commit提交记录
        gitAdapter.initGit();
        List<CommitMessage> commitMessages = gitAdapter.getCommitMessages();
        int i=1;
        for(CommitMessage commitMessage : commitMessages) {
            System.out.println("===================================================================================================================================");
            System.out.println(i+"  "+commitMessage.toString());
            i++;
        }
        gitAdapter.checkOut("security-huawei");
        // 对比master分支的commitId版本的代码差异
//        String commitId = "ce52c68b8bed4b6f76d469e951bbb509b29e9662";
        String commitId = "bc6fbc2f905fd7341ed1dd9a2772ccf5fc226758";
        String commitId2 = "18f40e8be52771c0f26ce558c0aceab29324d3be";
        System.out.println(gitAdapter.getObjectId("18f40e8be52771c0f26ce558c0aceab29324d3be"));
        List<ClassInfo> classInfos=CodeDiffCommit.getSameBranchBetweenTwoCommit(gitAdapter,null,commitId2);
        for (ClassInfo classInfo : classInfos) {
        	System.out.println(classInfo.toString());
		}
//        List<ClassInfo> classInfos = CodeDiff.diffBranchToBranch(gitUtil,"f20210126","master",commitId);
//        for(ClassInfo classInfo:classInfos)
//            System.out.println(classInfo.toString());


//        Ref branchRef = gitAdapter.checkOutAndPull("master","1cf7615d811a0e92d53cb672fcec245eaec4506e");
//        GitAdapter gitAdapterA = new GitAdapter(TestMain.remotePath,filePath,"f20210126");
//        Ref ref = gitAdapter.getBranchRef();
//        gitAdapterA.getMergeMessage(ref);

    }
}

