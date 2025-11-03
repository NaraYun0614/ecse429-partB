Feature: Get category by ID
  As a user,
  I want to get a category by its ID,
  so that I can see its details.

  Background:
    Given the app is running

  # Normal Flow
  Scenario Outline: Get an existing category item by identifier
    Given a category item exists with identifier "<id>"
    When I request the category item with identifier "<id>"
    Then the output should include a status code of "200"
    Examples:
      | id |
      | 1  |

  # Alternate Flow
  Scenario Outline: Filter categories list by ID
    When I filter the category list to get the todo with id "<id>"
    Then a output code "200" is returned
    Examples:
      | id |
      | 1  |

  # Error Flow
  Scenario Outline: Request non-existent category by ID
    Given the category list does not contain id "<id>"
    When I request the category item with id "<id>"
    Then a output code of "404" is returned
    Examples:
      | id  |
      | -1  |
      | 0   |
