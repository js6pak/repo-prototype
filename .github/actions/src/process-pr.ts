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
      owner: context.repo.owner,
      repo: context.repo.repo,
      pull_number: prNumber,
    });

    core.info(JSON.stringify(pullRequest));
  } catch (error) {
    core.setFailed(error as Error);
  }
})();
