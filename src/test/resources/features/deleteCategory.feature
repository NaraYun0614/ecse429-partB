Feature: Delete Category By Id
  As a user,
  I want to delete a category by id,
  so that I can delete all similar items at once.

  Background:
    Given the app is running

  #Normal Flow
  Scenario Outline: Delete a category by id
    Given the category with id "<categoryId>" exists
    When a user deletes the category with id "<categoryId>"
    And a status code "200" with response phrase "OK" is returned

    Examples:
      |categoryId|
      |1     |
      |2     |

  #Alternative Flow
  Scenario Outline: Delete a category by filtering endpoint with id
    Given the category with id "<categoryId>" exists
    When user filters the endpoint to delete category with id "<categoryId>"
    And a status code "200" with response phrase "OK" is returned
    Examples:
      |categoryId|
      |1     |

  #Error Flow
  Scenario: Delete a category that does not exist
    Given the category with id "<categoryId>" does not exist
    When a user deletes the category with id "<categoryId>"
    Then a status code "404" with response phrase "Not Found" is returned

