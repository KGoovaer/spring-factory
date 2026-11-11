# Spring Factory
This repository combines a Java 21 Spring Boot application and a React
frontend with a GitHub-hosted software factory. GitHub Actions orchestrates the
work, while Sandcastle runs specialized AI agents for exploration,
implementation, decomposition, and review.
## How the software factory works
```mermaid
flowchart TD
    Human["Human creates an issue or spec"]
    subgraph GitHub["GitHub: state and orchestration"]
        ExploreLabel["Label: agent:explore"]
        ImplementLabel["Label: agent:implement"]
        SpecLabel["Label: agent:implement-spec<br/>or schedule / manual dispatch"]
        ExploreWorkflow["Explore workflow"]
        ImplementWorkflow["Implement workflow"]
        SpecWorkflow["Implement-spec workflow"]
        ReviewLabel["Label: agent:review"]
        ReviewWorkflow["Review workflow"]
        IssueState[("Issues, labels, comments,<br/>dependencies, and pull requests")]
    end
    subgraph Agents["Sandcastle AI agents"]
        Explorer["Explorer<br/>groups tickets, finds duplicates,<br/>and proposes a plan"]
        Decomposer["Decomposer<br/>turns a spec into ordered<br/>child tickets"]
        Implementer["Implementer<br/>changes code, verifies it,<br/>and commits"]
        Reviewer["Independent reviewer<br/>checks the diff, standards,<br/>and originating ticket"]
    end
    Human --> ExploreLabel
    Human --> ImplementLabel
    Human --> SpecLabel
    ExploreLabel --> ExploreWorkflow --> Explorer
    Explorer -->|"easy call"| ImplementLabel
    Explorer -->|"needs a decision"| HumanCheckpoint["Label: needs-a-human"]
    Explorer -->|"duplicate"| Closed["Close duplicate issue"]
    Explorer --> IssueState
    SpecLabel --> SpecWorkflow
    SpecWorkflow -->|"no queue yet"| Decomposer
    Decomposer -->|"create dependency-ordered queue"| IssueState
    IssueState -->|"next unblocked ticket"| SpecWorkflow
    SpecWorkflow --> Implementer
    ImplementLabel --> ImplementWorkflow --> Implementer
    Implementer -->|"commits and draft PR"| IssueState
    IssueState --> ReviewLabel --> ReviewWorkflow --> Reviewer
    Reviewer -->|"fixes when possible and posts verdict"| IssueState
    Reviewer -->|"mark PR ready"| Merge["Human reviews and merges"]
    Merge -->|"spec queue has more work"| SpecWorkflow
```
The workflows are deliberately memoryless: GitHub issues, labels,
dependencies, comments, branches, and pull requests hold the durable state.
Agents do not receive a GitHub token; separate workflow steps validate their
output and perform repository writes. Pull requests remain the human merge
checkpoint.
The workflow definitions live in `.github/workflows/agent-*.yml`, and their
Sandcastle agents and prompts live in `.sandcastle/agent-workflows/`.
