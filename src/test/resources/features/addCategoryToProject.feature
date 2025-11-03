Feature: Adding Category to Project
  As a user,
  I want to add a relationship tasks with id of a project,
  so that I can group related projects.

  Background:
    Given the app is running


    #normal_flow
  Scenario Outline: Add a relationship between a specific project and already defined category
    Given I have a project with ID "<project_id>"
    And I have a category with ID "<category_id>"
    When I request to add a relationship categories between project "<project_id>" and categories "<category_id>"
    Then the relationship between project "<project_id>" category "<category_id>" should be created

    Examples:
      | project_id | category_id |
      | 1          | 1           |


    #alternative_flow
  Scenario Outline: Add a relationship between a specific project and a created category
    Given I have a project with ID "<project_id>"
    Given I create a category with title "<title>", description "<description>"
    When I request to add a relationship categories between project "<project_id>" and categories "<category_id>"
    Then the relationship between project "<project_id>" category "<category_id>" should be created

    Examples:
      | project_id | title   | description | category_id |
      | 1          | testing | hero        | 1           |

    #error_flow
  Scenario Outline: Add a relationship categories between a project and a non existent category
    Given I have a project with ID "<project_id>"
    When I request to add a relationship categories between project "<project_id>" and a non existent category with id "<category>"
    Then I get an error code "404"

    Examples:
      | project_id | category_id |
      | 1          | 999         |











