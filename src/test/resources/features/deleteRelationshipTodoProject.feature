Feature: Delete the relationship tasks between a project and a todo
  As a user,
  I want to remove the tasks relationship between a project and a todo,
  so that I can reorganize my todos.

  Background:
    Given the app is running

  # Normal Flow
  Scenario Outline: Delete a tasks relationship with project id and todo id - via todo API
    Given a project with title "<project_title>", description "<project_description>", completed status "<completed_status>", and active_status "<active_status>" exists
    And a todo with title "<todo_title>", description "<todo_description>" and doneStatus "<done_status>" exists
    And there is a tasks relationship between the todo with title "<todo_title>" and the project with title "<project_title>"
    When a user deletes the tasks relationship between the todo with title "<todo_title>" and the project with title "<project_title>" with the todo API
    Then the todo with title "<todo_title>" and the project with title "<project_title>" do not have a relationship
    Examples:
      | project_title | project_description | completed_status | active_status | todo_title | todo_description | done_status |
      | ecse429       | Fall 2025           | False            | True          | read ch.1  | Harry Potter     | False       |
      | ecse223       | Fall 2024           | False            | True          | read ch.2  | Harry Potter     | False       |
      | ecse415       | Fall 2025           | False            | True          | read ch.3  | Harry Potter     | False       |

  # Alternate Flow
  Scenario Outline: Delete a tasks relationship with project id and todo id - via project API
    Given a project with title "<project_title>", description "<project_description>", completed status "<completed_status>", and active_status "<active_status>" exists
    And a todo with title "<todo_title>", description "<todo_description>" and doneStatus "<done_status>" exists
    And there is a tasks relationship between the todo with title "<todo_title>" and the project with title "<project_title>"
    When a user deletes the tasks relationship between the todo with title "<todo_title>" and the project with title "<project_title>" with the project API
    Then the todo with title "<todo_title>" and the project with title "<project_title>" do not have a relationship
    Examples:
      | project_title | project_description | completed_status | active_status | todo_title | todo_description | done_status |
      | ecse429       | Fall 2025           | False            | True          | read ch.1  | Harry Potter     | False       |
      | ecse223       | Fall 2024           | False            | True          | read ch.2  | Harry Potter     | False       |
      | ecse415       | Fall 2025           | False            | True          | read ch.3  | Harry Potter     | False       |

  # Error Flow
  Scenario Outline: Delete an inexistant relationship a project by ID that does not exist
    Given a project with title "<project_title>", description "<project_description>", completed status "<completed_status>", and active_status "<active_status>" exists
    And a todo with title "<todo_title>", description "<todo_description>" and doneStatus "<done_status>" exists
    When a user deletes the tasks relationship between the todo with title "<todo_title>" and the project with title "<project_title>" with the project API
    Then the status code "404" is returned
    Examples:
      | project_title | project_description | completed_status | active_status | todo_title | todo_description | done_status |
      | ecse429       | Fall 2025           | False            | True          | read ch.1  | Harry Potter     | False       |
      | ecse223       | Fall 2024           | False            | True          | read ch.2  | Harry Potter     | False       |
      | ecse415       | Fall 2025           | False            | True          | read ch.3  | Harry Potter     | False       |