Feature: Delete Todo By Id
  As a user,
  I want to delete a todo by id,
  so that I can delete the completed todo item.

  Background:
    Given the app is running

  #Normal Flow
  Scenario Outline: Delete a todo by id
    Given the todo with id "<todoId>" exists
    When a user deletes the todo with id "<todoId>"
    And a status code "200" with response phrase "OK" is returned

    Examples:
      |todoId|
      |1     |
      |2     |

  #Alternative Flow
  Scenario Outline: Delete a todo by filtering endpoint with id
    Given the todo with id "<todoId>" exists
    When user filters the endpoint to delete todo with id "<todoId>"
    And a status code "200" with response phrase "OK" is returned
    Examples:
      |todoId|
      |1     |

  #Error Flow
  Scenario: Delete a todo that does not exist
    Given the todo with id "<todoId>" does not exist
    When a user deletes the todo with id "<todoId>"
    Then a status code "404" with response phrase "Not Found" is returned

