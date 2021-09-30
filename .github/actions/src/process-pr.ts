import * as core from "@actions/core";
import { context, getOctokit } from "@actions/github";

(async () => {
  try {
    const token = core.getInput("github-token", { required: true });
    const github = getOctokit(token);

    const prNumber = context.payload.pull_request?.number;

    if (prNumber == null) {
      core.setFailed("Couldn't get the pull request number");
      return;
    }

    const { data: pullRequest } = await github.rest.pulls.get({
      ...context.repo,
      pull_number: prNumber,
    });

    const { data: comments } = await github.rest.issues.listComments({
      ...context.repo,
      issue_number: pullRequest.number,
    });

    let comment = comments.find((c) => c.user?.login === "github-actions[bot]");

    if (comment == null) {
      const response = await github.rest.issues.createComment({
        ...context.repo,
        issue_number: pullRequest.number,
        body: "Running...",
      });
      comment = response.data;
    }

    await github.rest.issues.updateComment({
      ...context.repo,
      comment_id: comment.id,
      body: "updated",
    });
  } catch (error) {
    core.setFailed(error as Error);
  }
})();
