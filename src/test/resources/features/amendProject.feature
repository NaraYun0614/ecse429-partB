Feature: Amend a project
  As a user,
  I want to modify the details of a project,
  so that I can keep maintain keep my organization of work up to date.

  Background:
    Given the app is running

  # Normal Flow
  Scenario Outline: Change a field of a project
    Given a project with title "<project_title>", description "<project_description>", completed status "<completed_status>", and active_status "<active_status>" exists
    When a user marks the project "<project_title>" as complete by using the PUT API call
    Then the project with title "<project_title>" should have a completed status of "<new_completed_status>"
    Examples:
      | project_title | project_description | completed_status | active_status | new_completed_status |
      | ecse429       | Fall 2025           | false            | true          | true                 |
      | ecse223       | Fall 2024           | false            | true          | true                 |
      | ecse415       | Fall 2025           | false            | true          | true                 |

    # Alternate Flow
  Scenario Outline: Change an empty field of a project
    Given a project with title "<project_title>", description "<project_description>", completed status "<completed_status>", and active_status "<active_status>" exists
    When a user marks the project "<project_title>" as complete by using the POST API call
    Then the project with title "<project_title>" should have a completed status of "<new_completed_status>"
    Examples:
      | project_title | project_description | completed_status | active_status | new_completed_status |
      | comp429       | Fall 2025           | false            | true          | true                 |
      | comp223       | Fall 2024           | false            | true          | true                 |
      | comp415       | Fall 2025           | false            | true          | true                 |

    # Error Flow
  Scenario Outline: Change a non-empty field of a project that does not exist
    Given a project with title "<project_title>", description "<project_description>", completed status "<completed_status>", and active_status "<active_status>" exists
    When a user marks the project "<attempted_project_id>" as complete
    Then the status code "404" is returned
    Examples:
      | project_title | project_description | completed_status | active_status | attempted_project_id |
      | psyc429       | Fall 2025           | false            | true          | -1                   |
      | psyc223       | Fall 2024           | false            | true          | 0                    |
      | psyc415       | Fall 2025           | false            | true          | 999                  |