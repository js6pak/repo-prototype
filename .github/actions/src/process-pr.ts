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

    // const { data: comments } = await github.rest.issues.listComments({
    //   ...context.repo,
    //   issue_number: pullRequest.number,
    // });

    // let comment = comments.filter(comment => comment.)

    await github.rest.issues.createComment({
      ...context.repo,
      issue_number: pullRequest.number,
      body: "hello",
    });
  } catch (error) {
    core.setFailed(error as Error);
  }
})();
