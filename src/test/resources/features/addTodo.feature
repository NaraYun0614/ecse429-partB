Feature: Add todo
  As a user,
  I want to add todos,
  so that I can track my tasks.

  Background:
    Given the app is running

  # Normal Flow
  Scenario Outline: Add a todo with title and description
    When a user adds a todo with title "<title>" and description "<description>"
    Then a new todo with title "<title>" and description "<description>"
    And a status code "201" is returned
    Examples:
      | title         | description         |
      | read paper    | ML survey section   |
      | buy bananas   | 6 pack              |

  # Alternate Flow
  Scenario Outline: Add a todo with only a title
    When a user adds a todo with title "<title>"
    Then a new todo with title "<title>"
    And a status code "201" is returned
    Examples:
      | title         |
      | submit hw     |
      | laundry       |

  # Error Flow
  Scenario Outline: Fail to add todo with empty title
    When a user adds a todo with title "<title>"
    Then the todo is not added
    And a status code "400" is returned
    Examples:
      | title |
      |       |
