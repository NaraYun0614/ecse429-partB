Feature: Add tasks relationship (todo -> project)
  As a user,
  I want to relate a todo to a project (tasksof),
  so that the todo appears under that project.

  Background:
    Given the app is running

  # Normal Flow
  Scenario Outline: Create a tasksof relationship
    Given I have a todo with ID "<todo_id>"
    And I have a project with ID "<project_id>"
    When I request to add a relationship tasksof between todo "<todo_id>" and projects "<project_id>"
    Then the relationship between todo "<todo_id>" and project "<project_id>" should be created
    Examples:
      | todo_id | project_id |
      | 1       | 1          |
      | 2       | 1          |

  # Alternate Flow
  Scenario Outline: Create project via API first, then relate
    Given I create a project with title "<title>", completed "<completed>", description "<description>", active "<active>"
    And I have a todo with ID "<todo_id>"
    # afterwards, use the known/new project id from the system (e.g., 1) in the call
    When I request to add a relationship tasksof between todo "<todo_id>" and projects "<project_id>"
    Then the relationship between todo "<todo_id>" and project "<project_id>" should be created
    Examples:
      | title     | completed | description | active | todo_id | project_id |
      | demoProj  | false     | demo desc   | true   | 1       | 1          |

  # Error Flow
  Scenario Outline: Try to relate to a non-existent project
    Given I have a todo with ID "<todo_id>"
    When I request to add a relationship tasksof between todo "<todo_id>" and a non existent project with id "<missing_project_id>"
    Then I get an error code "404"
    Examples:
      | todo_id | missing_project_id |
      | 1       | -1                 |
      | 2       | 0                  |
