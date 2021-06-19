package com.techstar.codeanalysis.diff.modle;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class CommitMessage {
	private String commitId;
	private String commitIdent;
	private String commitMessage;
	private String commitDate;
	private String lastCommitId;
	private String mergeBranchCommitId;

}
