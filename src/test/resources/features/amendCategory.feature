Feature: Amend a category
  As a user,
  I want to modify information related to a category,
  so that I can maintain consistency and coherence in my organization of todos and projects.

  Background:
    Given the app is running

    # Normal Flow
  Scenario Outline: Change a non-empty field of a category - via PUT API call
    Given a category with title "<category_title>", and description "<category_description>"
    When a user changes the description of category with title "<category_title>" to "<new_category_description>" by using the PUT API call
    Then the category with title "<category_title>" should have a description of "<new_category_description>"
    Examples:
      | category_title | category_description | new_category_description |
      | HIGH           | High priority        | Due this week            |
      | MEDIUM         | Medium priority      | Due next week            |
      | LOW            | Low priority         | Due later                |

    # Alternate Flow
  Scenario Outline: Change an empty field of a category - via POST API call
    Given a category with title "<category_title>", and description "<category_description>"
    When a user changes the description of category with title "<category_title>" to "<new_category_description>" by using the POST API call
    Then the category with title "<category_title>" should have a description of "<new_category_description>"
    Examples:
      | category_title | category_description | new_category_description |
      | HIGH           | High priority        | Due this week            |
      | MEDIUM         | Medium priority      | Due next week            |
      | LOW            | Low priority         | Due later                |

    # Error Flow
  Scenario Outline: Change a field of a category that does not exist
    Given a category with title "<category_title>", and description "<category_description>"
    When a user changes the description of category "<attempted_category_id>"
    Then the status code "404" is returned
    Examples:
      | category_title | category_description | attempted_category_id |
      | HIGH           | High priority        | -1                    |
      | MEDIUM         | Medium priority      | 0                     |
      | LOW            | Low priority         | -99                   |