Feature: Get project by ID
  As a user,
  I want to view my projects,
  so that I can assess the work I have to do.

  Background:
    Given the app is running

  # Normal Flow
  Scenario Outline: Get a project by its ID (title lookup)
    Given a project with title "<project_title>", description "<project_description>", completed status "<completed_status>", and active_status "<active_status>" exists
    When a user gets the project with title "<project_title>" by id
    Then the project with title "<project_title>" is returned
    And the status code "200" is returned
    Examples:
      | project_title | project_description | completed_status | active_status |
      | ecse429       | Fall 2025           | false            | true          |
      | ecse223       | Fall 2021           | false            | true          |

  # Alternate Flow
  Scenario Outline: Get all projects
    Given there are a number of projects "<num_of_projects>" in the system
    When a user gets all of the the projects in the system
    Then "<num_of_projects>" projects are returned
    And the status code "200" is returned
    Examples:
      | num_of_projects |
      | 2               |
      | 3               |

  # Alternate Flow 2: View tasks under a project
  Scenario Outline: View tasks assigned to a project
    Given a project item exists with identifier "<project_id>"
    When I request tasks assigned to project with identifier "<project_id>"
    Then the response should include a status code of "200"
    Examples:
      | project_id |
      | 1          |

  # Error Flow
  Scenario Outline: Get a project by ID that does not exist
    When a user gets the project with id "<attempted_project_id>"
    Then the status code "404" is returned
    Examples:
      | attempted_project_id |
      | -1                   |
      | 0                    |
      | 999                  |
