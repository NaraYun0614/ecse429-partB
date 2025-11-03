Feature: Get tasks for a project by ID
  As a user,
  I want to list tasks assigned to a project,
  so that I can review its work items.

  Background:
    Given the app is running

  # Normal Flow
  Scenario Outline: Get tasks assigned to a project
    Given a project item exists with identifier "<project_id>"
    When I request tasks assigned to project with identifier "<project_id>"
    Then the response should include a status code of "200"
    Examples:
      | project_id |
      | 1          |

  # Alternate Flow
  Scenario Outline: Filter tasks for a project by task id
    Given a project item exists with identifier "<project_id>"
    When I request a specific task assigned to project by filtering with the requested task id "<task_id>"
    Then the response should include a status code of "200"
    Examples:
      | project_id | task_id |
      | 1          | 1       |

  # Error Flow  (matches your StepDefinitions’ steps)
  Scenario Outline: Project id missing for tasks lookup
    Given the project list does not contain id needed to check for tasks "<bad_id>"
    When I request the project item with id "<bad_id>"
    Then the response should include a status code of "404"
    Examples:
      | bad_id |
      | -1     |
      | 0      |
