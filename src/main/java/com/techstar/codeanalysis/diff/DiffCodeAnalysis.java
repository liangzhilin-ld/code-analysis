package com.techstar.codeanalysis.diff;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.util.io.DisabledOutputStream;
//https://blog.csdn.net/zou90512/article/details/102163415
public class DiffCodeAnalysis {
	static String localPath = "E:\\vbi";

    public static void main( String[] args )
    {
        try (Git git = Git.open(new File(Paths.get(localPath, ".git").toString()))) {

        	RevWalk walk = new RevWalk(git.getRepository());
        	Iterable<RevCommit> commits = git.log().all().call();
        	for(RevCommit commit:commits){
//        		commitList.add(commit);
        		//if(commit.getParentCount()==2)continue;
        		System.out.println(commit.toObjectId()+commit.getFullMessage()+"-"+commit.getAuthorIdent().getWhen());
        	}
        	AbstractTreeIterator oldTreeIterator = new FileTreeIterator( git.getRepository() );
        	AbstractTreeIterator newTreeIterator = new FileTreeIterator( git.getRepository() );
//        	List<DiffEntry> diffEntrys=git.diff().setOldTree(oldTreeIterator)
//        									.setNewTree(newTreeIterator)
//        									.call();
//        	for (DiffEntry diffEntry : diffEntrys) {
//        		//ADD  MODIFY  DELETE  RENAME
//        		diffEntry.getChangeType();
//			}
        	
        	ByteArrayOutputStream  outputStream =new ByteArrayOutputStream();
        	try( DiffFormatter formatter = new DiffFormatter( outputStream ) ) {
        	  formatter.setRepository( git.getRepository() );
        	  List<DiffEntry> entries = formatter.scan( oldTreeIterator, newTreeIterator );
        	  for (DiffEntry diffEntry : entries) {
        		  formatter.format(diffEntry);
        		  String diffText = outputStream.toString("UTF-8");
 		          System.out.println(diffText);
        	  }
        	  FileHeader fileHeader = formatter.toFileHeader( entries.get( 0 ) );
        	  //return fileHeader.toEditList();
        	}
        	
        	
//            Stream<DiffEntry> stream = getDifferentBetweenTwoCommit(git, oldCommit, newCommit);
//            if (null == stream)
//                return null;
//            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
//                DiffFormatter df = new DiffFormatter(out);
//                // ignores all whitespace
//                df.setDiffComparator(RawTextComparator.WS_IGNORE_ALL);
//                df.setRepository(git.getRepository());
//
//                List<SourceCodeFile> map = stream.map(diffEntry -> {
//                    try {
//                        FileHeader header = df.toFileHeader(diffEntry);
//                        //  analysis new add code block.
//                        List<SourceCodeBlock> list = header.getHunks().stream()
//                                .flatMap((Function<HunkHeader, Stream<Edit>>) hunk -> hunk.toEditList().stream())
//                                .filter(edit -> edit.getEndB() - edit.getBeginB() > 0)
//                                .map(edit -> SourceCodeBlock.of(edit.getBeginB(), edit.getEndB()))
//                                .collect(Collectors.toList());
//                        if (list.isEmpty())
//                            return null;
//                        return new SourceCodeFile(diffEntry.getNewPath(), list);
//                    } catch (Exception e) {
//                        throw new RuntimeException(e);
//                    } finally {
//                        out.reset();
//                    }
//                })
//                        .filter(Objects::nonNull)
//                        .collect(Collectors.toList());
//                CACHE_MAP.put(cacheKey, map);
//                return map;
//            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
}
