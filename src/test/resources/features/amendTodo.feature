Feature: Amend a todo
  As a user,
  I want to modify information related to a todo,
  so that I can maintain an up to date record of work.

  Background:
    Given the app is running

  # Normal Flow
  Scenario Outline: Change a field of a todo - via PUT API call
    Given a todo with title "<todo_title>", description "<todo_description>" and doneStatus "<done_status>" exists
    When a user marks the todo with title "<todo_title>" as done by using the PUT API call
    Then the todo with title "<todo_title>" should have a doneStatus of "<new_done_status>"
    Examples:
      | todo_title | todo_description | done_status | new_done_status |
      | read ch.1  | Harry Potter     | False       | True            |
      | read ch.2  | Harry Potter     | False       | True            |
      | read ch.3  | Harry Potter     | False       | True            |

  # Alternate Flow
  Scenario Outline: Change a field of a todo - via POST API call
    Given a todo with title "<todo_title>", description "<todo_description>" and doneStatus "<done_status>" exists
    When a user marks the todo with title "<todo_title>" as done by using the POST API call
    Then the todo with title "<todo_title>" should have a doneStatus of "<new_done_status>"
    Examples:
      | todo_title | todo_description | done_status | new_done_status |
      | read ch.1  | Harry Potter     | False       | True            |
      | read ch.2  | Harry Potter     | False       | True            |
      | read ch.3  | Harry Potter     | False       | True            |

  # Error Flow
  Scenario Outline: Change a non-empty field of a todo that does not exist
    Given a todo with title "<todo_title>", description "<todo_description>" and doneStatus "<done_status>" exists
    When a user marks the todo "<attempted_todo_id>" as done
    Then the status code "404" is returned
    Examples:
      | todo_title | todo_description | done_status | attempted_todo_id |
      | read ch.1  | Harry Potter     | False       | -1                |
      | read ch.2  | Harry Potter     | False       | 0                 |
      | read ch.3  | Harry Potter     | False       | 999               |