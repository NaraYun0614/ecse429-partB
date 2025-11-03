Feature: Add category
  As a user,
  I want to add categories,
  so that I can organize my todos and projects.

  Background:
    Given the app is running

  # Normal Flow
  Scenario Outline: Add a category with title and description
    When a user adds a category with title "<title>", and description "<description>"
    Then a new category with title "<title>" and description "<description>" is added
    And a status code "201" is returned
    Examples:
      | title        | description      |
      | school       | uni work         |
      | groceries    | weekly shopping  |

  # Alternate Flow
  Scenario Outline: Add a category with only a title
    When a user adds a category with title "<title>"
    Then a new category with title "<title>" is added
    And a status code "201" is returned
    Examples:
      | title     |
      | errands   |
      | sidejobs  |

  # Error Flow
  Scenario Outline: Fail to add category with empty title
    When a user adds a category with title "<title>"
    Then the category is not added
    And a status code "400" is returned
    Examples:
      | title |
      |       |
