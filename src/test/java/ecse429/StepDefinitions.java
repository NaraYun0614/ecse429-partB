package ecse429;
import io.cucumber.java.ParameterType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;

import okhttp3.Response;
import org.example.Api;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Map;
import java.util.HashMap;
import org.junit.jupiter.api.Assertions;
import okhttp3.ResponseBody;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class StepDefinitions {

    @ParameterType("todo|todos|project|projects|category|categories")
    public Element element(String text) {
        text = text.toUpperCase();
        if (text.endsWith("S")) {
            text = text.substring(0, text.length()-1);
        }
        return Element.valueOf(text);
    }

    private final java.util.Map<String, String> todoAliases = new java.util.HashMap<>();

    private String realTodoId(String given) {
        return todoAliases.getOrDefault(given, given); // fallback: use the given
    }

    private void aliasTodo(String given, String real) {
        todoAliases.put(given, real);
    }

    private final Map<Element, Map<String,String>> idAlias = new HashMap<>() {{
        put(Element.CATEGORY, new HashMap<>());
        put(Element.TODO,      new HashMap<>());
        put(Element.PROJECT,   new HashMap<>());
    }};

    private final Map<String, Object> scenarioState = new HashMap<>();
    private static String bodyString(Response r) {
        try (ResponseBody b = r.body()) {
            return (b != null) ? b.string() : "";
        } catch (IOException e) {
            throw new RuntimeException("Failed to read HTTP body", e);
        }
    }

    private String extractIdFromResponse(Response r, String objectKey) {
        String s = bodyString(r);
        if (s == null || s.isBlank()) return null;

        JSONObject j = new JSONObject(s);
        if (j.has("id")) return j.optString("id", null);

        if (objectKey != null && j.has(objectKey)) {
            JSONObject obj = j.optJSONObject(objectKey);
            if (obj != null) return obj.optString("id", null);
        }
        return null;
    }

    private String firstIdFromList(Response r, String arrayKey) {
        String s = bodyString(r);
        if (s == null || s.isBlank()) return null;

        JSONObject j = new JSONObject(s);
        JSONArray arr = j.optJSONArray(arrayKey);
        if (arr != null && arr.length() > 0) {
            return arr.getJSONObject(0).optString("id", null);
        }
        return null;
    }

    private String resolveId(Element e, String given) {
        return idAlias.get(e).getOrDefault(given, given);
    }

    private void aliasId(Element e, String given, String real) {
        idAlias.get(e).put(given, real);
    }

    private void deleteAll(Element e) {
        Response r = api.getRequest(plural(e), "json");
        JSONObject o = readObj(r);
        JSONArray arr = getGroupArray(o, plural(e));
        for (Object obj : arr) {
            String id = ((JSONObject) obj).optString("id");
            if (!id.isEmpty()) {
                api.deleteRequest(plural(e) + "/" + id);
            }
        }
    }

    private final Api api = new Api();
    private Response response;
    private JSONObject jsonBody;
    private String currentProjectId;
    private static String plural(Element e) {
        switch (e) {
            case TODO: return "todos";
            case CATEGORY: return "categories";
            case PROJECT: return "projects";
            default: throw new IllegalArgumentException("Unknown element: " + e);
        }
    }
    private String tryResolveOrUseLastCreated(Element e, String alias) {
        if ((alias == null || alias.isBlank()) && scenarioState.containsKey("lastCreatedTodoId")) {
            return (String) scenarioState.get("lastCreatedTodoId");
        }
        try {
            return resolveId(e, alias);
        } catch (Exception ex) {
            // If alias is a literal number that doesn’t resolve but we created a todo this step,
            // bind it now so later steps stay readable.
            if (e == Element.TODO && scenarioState.containsKey("lastCreatedTodoId")) {
                String real = (String) scenarioState.get("lastCreatedTodoId");
                aliasTodo(alias, real);           // use your existing aliasing method
                return real;
            }
            // Otherwise just return the alias (backend will 404 if it doesn't exist)
            return alias;
        }
    }

    private JSONObject readObj(Response r) {
        try {
            String s = r.body().string();
            if (s == null || s.isEmpty()) return new JSONObject(); // some DELETEs return empty
            return new JSONObject(s);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private JSONArray getGroupArray(JSONObject obj, String group) {
        if (obj.has(group)) return obj.getJSONArray(group);
        // Some GET /element/{id} still return grouped array
        // If not present, return empty
        return new JSONArray();
    }
    private boolean elementExistsByTitle(Element element, String targetTitle){
        Api call = new Api();
        String group = getPluralElement(element);
        Response r = call.getRequest(group, "json");
        JSONArray allElementsJSONArray = getJSONArray(r, group);
        for (Object obj : allElementsJSONArray) {
            String title = ((JSONObject) obj).getString("title");
            if(title.equals(targetTitle)){
                return true;
            }
        }
        return false;
    }

    private boolean isMatch(Element element, String targetTitle, String targetField, String targetContent ){
        Api call = new Api();
        String group = getPluralElement(element);
        Response r = call.getRequest(group, "json");
        JSONArray allElementsJSONArray = getJSONArray(r, group);
        for (Object obj : allElementsJSONArray) {
            String title = ((JSONObject) obj).getString("title");
            if(title.equals(targetTitle)){
                switch (element) {
                    case CATEGORY:
                        String field = ((JSONObject) obj).getString(targetField);
                        if(field.toString().equals(targetContent)) {
                            return true;
                        }
                        break;
                    case PROJECT:
                        boolean field2 = ((JSONObject) obj).getBoolean(targetField);
                        if(field2) {
                            return true;
                        }
                        break;
                    case TODO:
                        boolean field3 = ((JSONObject) obj).getBoolean(targetField);
                        if(field3) {
                            return true;
                        }
                        break;

                }

            }
        }
        return false;
    }

    private boolean existsByTitle(Element e, String title) {
        Response r = api.getRequest(plural(e), "json");
        JSONObject o = readObj(r);
        JSONArray arr = getGroupArray(o, plural(e));
        for (Object item : arr) {
            JSONObject it = (JSONObject) item;
            if (title.equals(it.optString("title"))) return true;
        }
        return false;
    }

    private String getIdByTitle(Element e, String title) {
        Response r = api.getRequest(plural(e), "json");
        JSONObject o = readObj(r);
        JSONArray arr = getGroupArray(o, plural(e));
        for (Object item : arr) {
            JSONObject it = (JSONObject) item;
            if (title.equals(it.optString("title"))) {
                return it.optString("id", "");
            }
        }
        return "";
    }
    private String getPluralElement(Element element){
        String pluralElement = "";
        switch (element) {
            case CATEGORY:
                pluralElement = "categories";
                break;
            case TODO:
                pluralElement = "todos";
                break;
            case PROJECT:
                pluralElement = "projects";
                break;
        }
        return pluralElement;
    }

    private JSONArray getJSONArray(Response r, String group){
        JSONObject obj = null;
        try {
            obj = new JSONObject(r.body().string());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return obj.getJSONArray(group);
    }

    private String getElementIdByTitle(Element element, String targetTitle){
        Api call = new Api();
        String group = getPluralElement(element);
        Response r = call.getRequest(group, "json");
        JSONArray allElementsJSONArray = getJSONArray(r, group);
        for (Object obj : allElementsJSONArray) {
            String title = ((JSONObject) obj).getString("title");
            if(title.equals(targetTitle)){
                return ((JSONObject) obj).getString("id");
            }
        }
        return "";
    }

    private int count(Element e) {
        Response r = api.getRequest(plural(e), "json");
        JSONObject o = readObj(r);
        return getGroupArray(o, plural(e)).length();
    }

    private void assertStatus(int expected, Response r) {
        assertEquals(expected, r.code(), "Unexpected HTTP status: " + safeMsg(r));
    }

    private String safeMsg(Response r) {
        try { return r.code() + " " + r.message(); } catch (Exception e) { return "<no message>"; }
    }

    // ---------- Service up ----------
    @Given("the app is running")
    public void theAppIsRunning() {
        Response ping = api.checkService();
        assertStatus(200, ping);
    }

    @Given("the {element} with id {string} exists")
    public void theElementWithIdExists(Element e, String idGiven) {
        // Try the requested id directly
        Response r = api.getRequest(plural(e) + "/" + idGiven, "json");
        if (r.code() == 200) { assertStatus(200, r); return; }

        // Seed a throwaway resource, then alias the feature id -> real id
        JSONObject body = new JSONObject().put("title", "seed-" + plural(e) + "-" + System.nanoTime());
        Response created = api.postRequest(plural(e), "json", body);
        assertTrue(created.code() == 201 || created.code() == 200, "Seed failed: " + safeMsg(created));

        Response list = api.getRequest(plural(e), "json");
        JSONObject obj = readObj(list);
        JSONArray arr = getGroupArray(obj, plural(e));
        assertTrue(arr.length() > 0, "Seeding " + plural(e) + " failed");
        String realId = arr.getJSONObject(arr.length() - 1).optString("id");
        assertFalse(realId.isEmpty(), "Seeded " + e + " has no id");
        aliasId(e, idGiven, realId);
    }

    @Given("the {element} with id {string} does not exist")
    public void theElementWithIdDoesNotExist(Element e, String id) {
        // If GET returns 200, then it exists
        Response r = api.getRequest(plural(e) + "/" + id, "json");
        assertTrue(r.code() == 404 || r.code() == 400 || r.code() == 500, "Element unexpectedly exists with id=" + id);
    }

    // ---------- Create (POST) ----------
    @When("a user adds a category with title {string}")
    public void addCategoryTitle(String title) {
        JSONObject body = new JSONObject().put("title", title);
        response = api.postRequest("categories", "json", body);
        jsonBody  = readObj(response);
    }

    @When("a user adds a category with title {string} and description {string}")
    public void addCategoryTitleDesc(String title, String description) {
        JSONObject body = new JSONObject().put("title", title).put("description", description);
        response = api.postRequest("categories", "json", body);
        jsonBody  = readObj(response);
    }

    @When("a user adds a todo with title {string}")
    public void addTodoTitle(String title) {
        JSONObject body = new JSONObject().put("title", title);
        response = api.postRequest("todos", "json", body);
        jsonBody  = readObj(response);
    }

    @When("a user adds a todo with title {string} and description {string}")
    public void addTodoTitleDesc(String title, String description) {
        JSONObject body = new JSONObject().put("title", title).put("description", description);
        response = api.postRequest("todos", "json", body);
        jsonBody  = readObj(response);
    }

    @When("a user adds a project with title {string}")
    public void addProjectTitle(String title) {
        JSONObject body = new JSONObject().put("title", title);
        response = api.postRequest("projects", "json", body);
        jsonBody  = readObj(response);
    }

    @When("a user adds a project with title {string} and description {string}")
    public void addProjectTitleDesc(String title, String description) {
        JSONObject body = new JSONObject().put("title", title).put("description", description);
        response = api.postRequest("projects", "json", body);
        jsonBody  = readObj(response);
    }

    // ---------- Assertions for create ----------
    @Then("a new {element} with title {string} exists")
    public void newElementWithTitleExists(Element e, String title) {
        assertTrue(existsByTitle(e, title), "Expected " + e + " with title '" + title + "' to exist");
    }

    @Then("the status code {int} is returned")
    public void thenStatusIs(int code) {
        assertStatus(code, response);
    }

    @Then("the response contains field {string} with value {string}")
    public void responseContainsFieldWithValue(String field, String expected) {
        assertEquals(expected, jsonBody.optString(field), "Field mismatch for '" + field + "'");
    }

    // ---------- Update (PUT / POST to id) ----------
    @When("a user updates the category titled {string} to have description {string}")
    public void updateCategoryDesc(String title, String newDesc) {
        String id = getIdByTitle(Element.CATEGORY, title);
        assertFalse(id.isEmpty(), "Category not found: " + title);
        JSONObject body = new JSONObject().put("title", title).put("description", newDesc);
        response = api.putRequest("categories/" + id, "json", body);
        jsonBody  = readObj(response);
    }

    @When("a user marks the project titled {string} as completed")
    public void markProjectCompleted(String title) {
        String id = getIdByTitle(Element.PROJECT, title);
        assertFalse(id.isEmpty(), "Project not found: " + title);
        JSONObject body = new JSONObject().put("title", title).put("completed", true);
        response = api.putRequest("projects/" + id, "json", body);
        jsonBody  = readObj(response);
    }

    @When("a user marks the todo titled {string} as done")
    public void markTodoDone(String title) {
        String id = getIdByTitle(Element.TODO, title);
        assertFalse(id.isEmpty(), "Todo not found: " + title);
        JSONObject body = new JSONObject().put("title", title).put("doneStatus", true);
        response = api.putRequest("todos/" + id, "json", body);
        jsonBody  = readObj(response);
    }

    @Then("the category titled {string} has description {string}")
    public void assertCategoryDesc(String title, String desc) {
        String id = getIdByTitle(Element.CATEGORY, title);
        Response r = api.getRequest("categories/" + id, "json");
        JSONObject o = readObj(r);
        JSONArray arr = getGroupArray(o, "categories");
        assertTrue(arr.length() > 0, "Category not returned");
        assertEquals(desc, arr.getJSONObject(0).optString("description"));
    }

    @Then("the project titled {string} has completed status {string}")
    public void assertProjectCompleted(String title, String completed) {
        String id = getIdByTitle(Element.PROJECT, title);
        Response r = api.getRequest("projects/" + id, "json");
        JSONObject o = readObj(r);
        JSONArray arr = getGroupArray(o, "projects");
        assertTrue(arr.length() > 0, "Project not returned");
        assertEquals(Boolean.parseBoolean(completed), arr.getJSONObject(0).optBoolean("completed"));
    }

    @Then("the todo titled {string} has doneStatus {string}")
    public void assertTodoDoneStatus(String title, String done) {
        String id = getIdByTitle(Element.TODO, title);
        Response r = api.getRequest("todos/" + id, "json");
        JSONObject o = readObj(r);
        JSONArray arr = getGroupArray(o, "todos");
        assertTrue(arr.length() > 0, "Todo not returned");
        assertEquals(Boolean.parseBoolean(done), arr.getJSONObject(0).optBoolean("doneStatus"));
    }

    // ---------- Delete ----------
    @When("a user deletes the {element} titled {string}")
    public void deleteElementByTitle(Element e, String title) {
        String id = getIdByTitle(e, title);
        assertFalse(id.isEmpty(), "Not found: " + e + " '" + title + "'");
        response = api.deleteRequest(plural(e) + "/" + id);
    }

    @Then("the {element} titled {string} does not exist")
    public void elementNotExist(Element e, String title) {
        assertFalse(existsByTitle(e, title), "Should not exist: " + e + " '" + title + "'");
    }

    // ---------- Filtering by id (GET ?id=xxx) ----------
    @When("user filters the {element} list by id {string}")
    public void filterListById(Element e, String id) {
        response = api.getRequest(plural(e) + "?id=" + id, "json");
        jsonBody  = readObj(response);
    }

    @Then("the filtered result for {element} contains id {string}")
    public void filteredContainsId(Element e, String id) {
        JSONArray arr = getGroupArray(jsonBody, plural(e));
        boolean found = false;
        for (Object obj : arr) {
            if (id.equals(((JSONObject) obj).optString("id"))) {
                found = true; break;
            }
        }
        assertTrue(found, "Filtered " + e + " does not contain id=" + id);
    }

    // ---------- Relationships ----------
    // todos <-> projects (tasksof / tasks)
    @When("I link todo titled {string} to project titled {string} as task")
    public void linkTodoToProjectTask(String todoTitle, String projectTitle) {
        String todoId = getIdByTitle(Element.TODO, todoTitle);
        String projectId = getIdByTitle(Element.PROJECT, projectTitle);
        assertFalse(todoId.isEmpty(), "Todo not found");
        assertFalse(projectId.isEmpty(), "Project not found");
        // project receives tasks: POST /projects/{id}/tasks { "id": "<todoId>" }
        JSONObject body = new JSONObject().put("id", todoId);
        response = api.postRequest("projects/" + projectId + "/tasks", "json", body);
        jsonBody  = readObj(response);
    }

    @Then("the project titled {string} contains task for todo titled {string}")
    public void projectContainsTaskForTodo(String projectTitle, String todoTitle) {
        String projectId = getIdByTitle(Element.PROJECT, projectTitle);
        Response r = api.getRequest("projects/" + projectId + "/tasks", "json");
        JSONObject o = readObj(r);
        JSONArray arr = o.optJSONArray("todos");
        assertNotNull(arr, "No todos array on /projects/{id}/tasks");
        String todoId = getIdByTitle(Element.TODO, todoTitle);
        boolean found = false;
        for (Object obj : arr) {
            if (todoId.equals(((JSONObject) obj).optString("id"))) { found = true; break; }
        }
        assertTrue(found, "Project does not contain todo task");
    }

    @When("I unlink todo titled {string} from project titled {string}")
    public void unlinkTodoFromProject(String todoTitle, String projectTitle) {
        String todoId = getIdByTitle(Element.TODO, todoTitle);
        String projectId = getIdByTitle(Element.PROJECT, projectTitle);
        response = api.deleteRequest("projects/" + projectId + "/tasks/" + todoId);
    }

    @Then("the project titled {string} has no task for todo titled {string}")
    public void projectHasNoTaskForTodo(String projectTitle, String todoTitle) {
        String projectId = getIdByTitle(Element.PROJECT, projectTitle);
        Response r = api.getRequest("projects/" + projectId + "/tasks", "json");
        JSONObject o = readObj(r);
        JSONArray arr = o.optJSONArray("todos");
        if (arr == null) return; // none
        String todoId = getIdByTitle(Element.TODO, todoTitle);
        for (Object obj : arr) {
            assertNotEquals(todoId, ((JSONObject) obj).optString("id"), "Still linked");
        }
    }

    // categories <-> todos
    @When("I add category titled {string} to todo titled {string}")
    public void addCategoryToTodo(String categoryTitle, String todoTitle) {
        String catId = getIdByTitle(Element.CATEGORY, categoryTitle);
        String todoId = getIdByTitle(Element.TODO, todoTitle);
        JSONObject body = new JSONObject().put("id", catId);
        response = api.postRequest("todos/" + todoId + "/categories", "json", body);
        jsonBody  = readObj(response);
    }

    @Then("the todo titled {string} lists category titled {string}")
    public void todoListsCategory(String todoTitle, String categoryTitle) {
        String todoId = getIdByTitle(Element.TODO, todoTitle);
        Response r = api.getRequest("todos/" + todoId + "/categories", "json");
        JSONObject o = readObj(r);
        JSONArray arr = o.optJSONArray("categories");
        assertNotNull(arr, "No categories array on todo");
        String catId = getIdByTitle(Element.CATEGORY, categoryTitle);
        boolean found = false;
        for (Object obj : arr) {
            if (catId.equals(((JSONObject) obj).optString("id"))) { found = true; break; }
        }
        assertTrue(found, "Todo does not list the category");
    }

    // categories <-> projects
    @When("I add category titled {string} to project titled {string}")
    public void addCategoryToProject(String categoryTitle, String projectTitle) {
        String catId = getIdByTitle(Element.CATEGORY, categoryTitle);
        String projectId = getIdByTitle(Element.PROJECT, projectTitle);
        JSONObject body = new JSONObject().put("id", catId);
        response = api.postRequest("projects/" + projectId + "/categories", "json", body);
        jsonBody  = readObj(response);
    }

    @Then("the project titled {string} lists category titled {string}")
    public void projectListsCategory(String projectTitle, String categoryTitle) {
        String projectId = getIdByTitle(Element.PROJECT, projectTitle);
        Response r = api.getRequest("projects/" + projectId + "/categories", "json");
        JSONObject o = readObj(r);
        JSONArray arr = o.optJSONArray("categories");
        assertNotNull(arr, "No categories array on project");
        String catId = getIdByTitle(Element.CATEGORY, categoryTitle);
        boolean found = false;
        for (Object obj : arr) {
            if (catId.equals(((JSONObject) obj).optString("id"))) { found = true; break; }
        }
        assertTrue(found, "Project does not list the category");
    }

    // ---------- Negative / error body ----------
    @Then("the response body contains error message {string}")
    public void responseBodyContainsErrorMessage(String expected) {
        // many error responses look like {"errorMessages":["..."]}
        JSONArray msgs = jsonBody.optJSONArray("errorMessages");
        assertNotNull(msgs, "No errorMessages array");
        assertTrue(msgs.length() > 0, "Empty errorMessages array");
        assertEquals(expected, msgs.getString(0));
    }

    // ---------- Counting ----------
    @Then("there are at least {int} {element} in the system")
    public void atLeastNElements(int n, Element e) {
        assertTrue(count(e) >= n, "Count is lower than expected for " + e);
    }

    // 1) Status code (string, or with phrase)
    @Then("a status code {string} is returned")
    public void statusCodeString(String code) {
        assertStatus(Integer.parseInt(code), response);
    }

    @Then("a status code {string} with response phrase {string} is returned")
    public void statusCodeAndPhrase(String code, String phrase) {
        assertEquals(Integer.parseInt(code), response.code(), "Unexpected status");
        assertEquals(phrase, response.message(), "Unexpected HTTP phrase");
    }

    // Some features say "output code"
    @Then("a output code {string} is returned")
    public void outputCodeString(String code) {
        statusCodeString(code);
    }
    @Then("a output code of {string} is returned")
    public void outputCodeOfString(String code) {
        statusCodeString(code);
    }

    // 2) Create (POST) with comma in the sentence
    @When("a user adds a category with title {string}, and description {string}")
    public void addCategoryComma(String title, String description) {
        addCategoryTitleDesc(title, description);
    }
    @When("a user adds a project with title {string}, and description {string}")
    public void addProjectComma(String title, String description) {
        addProjectTitleDesc(title, description);
    }

    // 3) Assertions using explicit wording (no {element} placeholder)
    @Then("a new category with title {string} and description {string} is added")
    public void newCategoryWithTitleDesc(String title, String desc) {
        assertTrue(existsByTitle(Element.CATEGORY, title), "Category not found: " + title);
        String id = getIdByTitle(Element.CATEGORY, title);
        Response r = api.getRequest("categories/" + id, "json");
        JSONObject o = readObj(r);
        JSONArray arr = o.optJSONArray("categories");
        assertNotNull(arr);
        assertTrue(arr.length() > 0);
        assertEquals(desc, arr.getJSONObject(0).optString("description"));
    }

    @Then("a new category with title {string} is added")
    public void newCategoryWithTitle(String title) {
        assertTrue(existsByTitle(Element.CATEGORY, title), "Category not found: " + title);
    }

    @Then("the category is not added")
    public void categoryNotAdded() {
        // Generic: last response should be an error status
        assertTrue(response.code() >= 400, "Expected failure but got " + safeMsg(response));
    }

    @Then("a new project with title {string}, and description {string} is added")
    public void newProjectWithTitleDesc(String title, String desc) {
        assertTrue(existsByTitle(Element.PROJECT, title), "Project not found: " + title);
        String id = getIdByTitle(Element.PROJECT, title);
        Response r = api.getRequest("projects/" + id, "json");
        JSONObject o = readObj(r);
        JSONArray arr = o.optJSONArray("projects");
        assertNotNull(arr);
        assertTrue(arr.length() > 0);
        assertEquals(desc, arr.getJSONObject(0).optString("description"));
    }

    @Then("a new project with title {string} is added")
    public void newProjectWithTitle(String title) {
        assertTrue(existsByTitle(Element.PROJECT, title), "Project not found: " + title);
    }

    @Then("the project is not added")
    public void projectNotAdded() {
        assertTrue(response.code() >= 400, "Expected failure but got " + safeMsg(response));
    }

    @Then("a new todo with title {string} and description {string}")
    public void newTodoWithTitleDesc(String title, String desc) {
        assertTrue(existsByTitle(Element.TODO, title), "Todo not found: " + title);
        String id = getIdByTitle(Element.TODO, title);
        Response r = api.getRequest("todos/" + id, "json");
        JSONObject o = readObj(r);
        JSONArray arr = o.optJSONArray("todos");
        assertNotNull(arr);
        assertTrue(arr.length() > 0);
        assertEquals(desc, arr.getJSONObject(0).optString("description"));
    }

    @Then("a new todo with title {string}")
    public void newTodoWithTitle(String title) {
        assertTrue(existsByTitle(Element.TODO, title), "Todo not found: " + title);
    }

    @Then("the todo is not added")
    public void todoNotAdded() {
        assertTrue(response.code() >= 400, "Expected failure but got " + safeMsg(response));
    }

    @Given("a category item exists with identifier {string}")
    public void categoryExistsById(String idGiven) {
        // Try actual id first (could be already present)
        Response r = api.getRequest("categories/" + idGiven, "json");
        if (r.code() == 200) {
            // great; no aliasing needed
            assertStatus(200, r);
            return;
        }
        // Not there -> seed one, then alias the provided id to the real one
        JSONObject body = new JSONObject().put("title", "seed-category-" + idGiven);
        Response created = api.postRequest("categories", "json", body);
        assertStatus(201, created);
        // fetch its real id
        Response list = api.getRequest("categories", "json");
        JSONObject obj = readObj(list);
        JSONArray arr = getGroupArray(obj, "categories");
        assertTrue(arr.length() > 0, "Seeding category failed");
        String realId = arr.getJSONObject(arr.length()-1).optString("id"); // last created
        assertFalse(realId.isEmpty(), "Seeded category has no id");
        aliasId(Element.CATEGORY, idGiven, realId);
    }

    @When("I request the category item with identifier {string}")
    public void requestCategoryByIdIdent(String idGiven) {
        String idReal = resolveId(Element.CATEGORY, idGiven);
        response = api.getRequest("categories/" + idReal, "json");
        jsonBody = readObj(response);
    }

    @Then("the output should include a status code of {string}")
    public void outputShouldIncludeStatus(String code) {
        assertStatus(Integer.parseInt(code), response);
    }


    @Given("the category list does not contain id {string}")
    public void categoryDoesNotContainId(String id) {
        Response r = api.getRequest("categories/" + id, "json");
        assertTrue(r.code() >= 400, "Category unexpectedly exists: id=" + id);
    }
    @When("I request the category item with id {string}")
    public void requestCategoryById(String id) {
        response = api.getRequest("categories/" + id, "json");
        jsonBody = readObj(response);
    }

    @Given("a todo item exists with identifier {string}")
    public void todoExistsById(String idGiven) {
        // Try direct GET first
        Response got = api.getRequest("todos/" + idGiven, "json");
        if (got.code() == 200) {
            aliasTodo(idGiven, idGiven);
            return;
        }

        // Create it
        org.json.JSONObject body = new org.json.JSONObject().put("title", "scan");
        Response created = api.postRequest("todos", "json", body);

        int c = created.code();
        if (c != 200 && c != 201) {
            throw new AssertionError("Failed to create todo: HTTP " + c + " - " + created.body());
        }

        String newId = new org.json.JSONObject(created.body()).optString("id", "");
        if (newId.isEmpty()) throw new AssertionError("Create response missing id: " + created.body());

        aliasTodo(idGiven, newId);

        Response check = api.getRequest("todos/" + newId, "json");
        if (check.code() != 200) {
            throw new AssertionError("Created todo not retrievable: HTTP " + check.code() + " - " + check.body());
        }
    }

    @Then("the response should include a status code of {string} and title {string}")
    public void responseIncludesStatusAndTitle(String code, String title) {
        statusCodeString(code);
        JSONArray arr = jsonBody.optJSONArray("todos");
        assertNotNull(arr);
        assertTrue(arr.length() > 0);
        assertEquals(title, arr.getJSONObject(0).optString("title"));
    }

    @Given("the todos list does not contain id {string}")
    public void todosDoesNotContainId(String id) {
        Response r = api.getRequest("todos/" + id, "json");
        assertTrue(r.code() >= 400, "Todo unexpectedly exists: id=" + id);
    }

    @When("I request the todo item with id {string}")
    public void i_request_the_todo_item_with_id(String idGiven) {
        String idReal = realTodoId(idGiven);
        response = api.getRequest("todos/" + idReal, "json");
    }

    @When("I filter todos by id {string}")
    public void i_filter_todos_by_id(String idGiven) {
        String idReal = realTodoId(idGiven);
        response = api.getRequest("todos?id=" + idReal, "json");
    }


    @When("user filters the endpoint to get todo with id {string}")
    public void user_filters_the_endpoint_to_get_todo_with_id(String idGiven) {
        filter_get_todo_by_id(idGiven);
    }

    @When("I request the todo by filtering endpoint with id {string}")
    public void i_request_the_todo_by_filtering_endpoint_with_id(String idGiven) {
        filter_get_todo_by_id(idGiven);
    }

    // shared impl to keep things DRY
    private void filter_get_todo_by_id(String idGiven) {
        String idReal = resolveId(Element.TODO, idGiven);
        response = api.getRequest("todos?id=" + idReal, "json");
        jsonBody = readObj(response);
    }

    @Given("the todo list does not contain id {string}")
    public void the_todo_list_does_not_contain_id(String idGiven) {
        String idReal = resolveId(Element.TODO, idGiven);
        Response r = api.getRequest("todos/" + idReal, "json");
        assertStatus(404, r); // ensures non-existence for 404 scenarios
    }

    @Then("a status code of {string} is returned")
    public void statusCodeOfString(String code) {
        statusCodeString(code);
    }

    // 5) Filtering phrasing variants
    @When("I filter the category list to get the todo with id {string}")
    public void filterCategoryListToGetTodoId(String id) {
        response = api.getRequest("categories?id=" + id, "json");
        jsonBody = readObj(response);
    }
    @When("I filter the todos list to get the todo with id {string}")
    public void filterTodosListToGetTodoId(String id) {
        response = api.getRequest("todos?id=" + id, "json");
        jsonBody = readObj(response);
    }
    @Then("a status code {string} and title {string} is returned")
    public void statusAndTitleReturned(String code, String title) {
        statusCodeString(code);
        JSONArray arr = jsonBody.optJSONArray("todos");
        assertNotNull(arr);
        assertTrue(arr.length() > 0);
        assertEquals(title, arr.getJSONObject(0).optString("title"));
    }

    @Given("a project item exists with identifier {string}")
    public void projectExistsById(String idGiven) {
        // Try real id first
        Response r = api.getRequest("projects/" + idGiven, "json");
        if (r.code() == 200) { assertStatus(200, r); return; }

        // Seed a project, then alias the feature's id -> real id
        JSONObject body = new JSONObject().put("title", "seed-project-" + idGiven);
        Response created = api.postRequest("projects", "json", body);
        assertTrue(created.code() == 201 || created.code() == 200, "Project seed failed: " + safeMsg(created));

        Response list = api.getRequest("projects", "json");
        JSONObject obj = readObj(list);
        JSONArray arr = getGroupArray(obj, "projects");
        assertTrue(arr.length() > 0, "Seeding project failed");
        String realId = arr.getJSONObject(arr.length() - 1).optString("id");
        assertFalse(realId.isEmpty(), "Seeded project has no id");
        aliasId(Element.PROJECT, idGiven, realId);
    }

    @When("a user gets the project with title {string} by id")
    public void getProjectWithTitleById(String title) {
        String id = getIdByTitle(Element.PROJECT, title);
        assertFalse(id.isEmpty(), "Project not found by title: " + title);
        response = api.getRequest("projects/" + id, "json");
        jsonBody = readObj(response);
    }

    @Then("the project with title {string} is returned")
    public void projectWithTitleReturned(String title) {
        JSONArray arr = getGroupArray(jsonBody, "projects");
        assertTrue(arr.length() > 0, "No project returned");
        assertEquals(title, arr.getJSONObject(0).optString("title"));
    }

    @Given("there are a number of projects {string} in the system")
    public void ensureNumberProjects(String nStr) {
        int n = Integer.parseInt(nStr.trim());
        // reset projects so count is exact for this scenario
        deleteAll(Element.PROJECT);
        for (int i = 0; i < n; i++) {
            JSONObject body = new JSONObject().put("title", "proj-" + System.nanoTime() + "-" + i);
            Response created = api.postRequest("projects", "json", body);
            assertTrue(created.code() == 201 || created.code() == 200, "Bulk seed failed: " + safeMsg(created));
        }
    }

    @When("I request tasks assigned to project with identifier {string}")
    public void i_request_tasks_assigned_to_project_with_identifier(String idGiven) {
        String idReal = resolveId(Element.PROJECT, idGiven);  // uses your alias map; if you don’t have it, use idGiven
        response = api.getRequest("projects/" + idReal + "/tasks", "json");
        jsonBody = readObj(response);
    }

    @When("I request a specific task assigned to project by filtering with the requested task id {string}")
    public void i_request_a_specific_task_assigned_to_project_by_filtering_with_the_requested_task_id(String todoIdGiven) {
        // We expect a project to have been selected earlier in the scenario;
        // if not, fall back to "1" (adjust if your feature uses another default).
        String projectIdReal = (currentProjectId != null && !currentProjectId.isEmpty())
                ? currentProjectId
                : resolveId(Element.PROJECT, "1");

        String todoIdReal = resolveId(Element.TODO, todoIdGiven);

        response = api.getRequest("projects/" + projectIdReal + "/tasks?id=" + todoIdReal, "json");
        jsonBody = readObj(response);
    }

    @Given("the project list does not contain id needed to check for tasks {string}")
    public void the_project_list_does_not_contain_id_needed_to_check_for_tasks(String idGiven) {
        String idReal = resolveId(Element.PROJECT, idGiven);
        // Verify the project does NOT exist (404). If it does, you could delete it first,
        // but asserting 404 here matches the step wording.
        Response r = api.getRequest("projects/" + idReal, "json");
        assertStatus(404, r);
        // Store it so the next step can try to request it and get 404 as intended
        currentProjectId = idReal;
    }

    @When("I request the project item with id {string}")
    public void i_request_the_project_item_with_id(String idGiven) {
        String idReal = resolveId(Element.PROJECT, idGiven);
        currentProjectId = idReal; // remember for later steps (like filtering /tasks?id=...)
        response = api.getRequest("projects/" + idReal, "json");
        // Don’t assert 200 here if your scenario expects 404 later; let the scenario’s THEN check handle it.
        jsonBody = readObj(response);
    }


    @Then("the project's tasks result contains todo id {string}")
    public void projectTasksContainsTodoId(String todoIdGiven) {
        String todoIdReal = resolveId(Element.TODO, todoIdGiven);
        JSONArray arr = jsonBody.optJSONArray("todos");
        assertNotNull(arr, "No todos array on project tasks result");
        boolean found = false;
        for (Object obj : arr) {
            if (todoIdReal.equals(((JSONObject) obj).optString("id"))) { found = true; break; }
        }
        assertTrue(found, "Filtered tasks do not contain id=" + todoIdReal);
    }


    @Then("the response should include a status code of {string}")
    public void the_response_should_include_a_status_code_of(String codeStr) {
        assertStatus(Integer.parseInt(codeStr), response);
    }

    @When("a user gets all of the the projects in the system")
    public void getAllProjects() {
        response = api.getRequest("projects", "json");
        jsonBody = readObj(response);
    }

    @Then("{string} projects are returned")
    public void projectsReturned(String nStr) {
        int n = Integer.parseInt(nStr.trim());
        JSONArray arr = getGroupArray(jsonBody, "projects");
        assertEquals(n, arr.length(), "Project count mismatch");
    }

    @When("a user gets the project with id {string}")
    public void getProjectById(String idGiven) {
        String idReal = resolveId(Element.PROJECT, idGiven);
        response = api.getRequest("projects/" + idReal, "json");
        jsonBody = readObj(response);
    }


    // 7) Delete by id phrasing

    @When("user filters the endpoint to delete todo with id {string}")
    public void user_filters_the_endpoint_to_delete_todo_with_id(String idGiven) {
        String idReal = resolveId(Element.TODO, idGiven);

        // Hit the filter-style endpoint to match the step wording
        Response filtered = api.getRequest("todos?id=" + idReal, "json");
        assertStatus(200, filtered);   // endpoint reachable

        // Then actually delete the resource
        response = api.deleteRequest("todos/" + idReal);
    }

    @When("a user deletes the category with id {string}")
    public void deleteCategoryById(String idGiven) {
        String idReal = resolveId(Element.CATEGORY, idGiven);
        response = api.deleteRequest("categories/" + idReal);
    }

    @When("a user deletes the project with id {string}")
    public void deleteProjectById(String idGiven) {
        String idReal = resolveId(Element.PROJECT, idGiven);
        response = api.deleteRequest("projects/" + idReal);
    }
    @When("user filters the endpoint to delete project with id {string}")
    public void user_filters_the_endpoint_to_delete_project_with_id(String idGiven) {
        String idReal = resolveId(Element.PROJECT, idGiven);

        Response filtered = api.getRequest("projects?id=" + idReal, "json");
        assertStatus(200, filtered);

        response = api.deleteRequest("projects/" + idReal);
    }

    @When("a user deletes the todo with id {string}")
    public void deleteTodoById(String idGiven) {
        String idReal = resolveId(Element.TODO, idGiven);
        response = api.deleteRequest("todos/" + idReal);
    }

    @When("user filters the endpoint to delete category with id {string}")
    public void userFiltersTheEndpointToDeleteCategoryWithId(String idGiven) {
        String idReal = resolveId(Element.CATEGORY, idGiven);

        Response filtered = api.getRequest("categories?id=" + idReal, "json");
        assertStatus(200, filtered);

        response = api.deleteRequest("categories/" + idReal);
    }

    @Given("I have a todo with ID {string}")
    public void haveTodoId(String idGiven) {
        // Try real id first
        Response r = api.getRequest("todos/" + idGiven, "json");
        if (r.code() != 200) {
            // Seed a todo, then alias the feature's id -> real id
            JSONObject body = new JSONObject().put("title", "seed-todo-" + idGiven);
            Response created = api.postRequest("todos", "json", body);
            assertTrue(created.code() == 201 || created.code() == 200, "Todo seed failed: " + safeMsg(created));

            Response list = api.getRequest("todos", "json");
            JSONObject obj = readObj(list);
            JSONArray arr = getGroupArray(obj, "todos");
            assertTrue(arr.length() > 0, "Seeding todo failed");
            String realId = arr.getJSONObject(arr.length() - 1).optString("id");
            assertFalse(realId.isEmpty(), "Seeded todo has no id");
            aliasId(Element.TODO, idGiven, realId);
        }
        // Final sanity check on resolved id
        assertStatus(200, api.getRequest("todos/" + resolveId(Element.TODO, idGiven), "json"));
    }

    @Given("I have a project with ID {string}")
    public void haveProjectId(String idGiven) {
        Response r = api.getRequest("projects/" + idGiven, "json");
        if (r.code() != 200) {
            // Seed a project, then alias the feature's id -> real id
            JSONObject body = new JSONObject().put("title", "seed-project-" + idGiven);
            Response created = api.postRequest("projects", "json", body);
            assertTrue(created.code() == 201 || created.code() == 200, "Project seed failed: " + safeMsg(created));

            Response list = api.getRequest("projects", "json");
            JSONObject obj = readObj(list);
            JSONArray arr = getGroupArray(obj, "projects");
            assertTrue(arr.length() > 0, "Seeding project failed");
            String realId = arr.getJSONObject(arr.length() - 1).optString("id");
            assertFalse(realId.isEmpty(), "Seeded project has no id");
            aliasId(Element.PROJECT, idGiven, realId);
        }
        // Final sanity check on resolved id
        assertStatus(200, api.getRequest("projects/" + resolveId(Element.PROJECT, idGiven), "json"));
    }

    @When("I request to add a relationship tasksof between todo {string} and projects {string}")
    public void addRelationshipTasksof(String todoIdGiven, String projectIdGiven) {
        String todoIdReal = resolveId(Element.TODO, todoIdGiven);
        String projectIdReal = resolveId(Element.PROJECT, projectIdGiven);

        JSONObject body = new JSONObject().put("id", todoIdReal); // POST todo id to the project
        response = api.postRequest("projects/" + projectIdReal + "/tasks", "json", body);
        jsonBody = readObj(response);
    }
    @Then("the relationship between todo {string} and project {string} should be created")
    public void relationshipCreated(String todoIdGiven, String projectIdGiven) {
        String todoIdReal = resolveId(Element.TODO, todoIdGiven);
        String projectIdReal = resolveId(Element.PROJECT, projectIdGiven);

        Response r = api.getRequest("projects/" + projectIdReal + "/tasks", "json");
        JSONObject o = readObj(r);
        JSONArray arr = o.optJSONArray("todos");
        assertNotNull(arr, "No todos array on /projects/{id}/tasks");

        boolean found = false;
        for (Object obj : arr) {
            if (todoIdReal.equals(((JSONObject) obj).optString("id"))) { found = true; break; }
        }
        assertTrue(found, "Relationship not present");
    }

    @Given("I create a project with title {string}, completed {string}, description {string}, active {string}")
    public void i_create_a_project_with_title_completed_description_active(
            String title, String completed, String description, String active) {
        JSONObject body = new JSONObject()
                .put("title", title)
                .put("completed", Boolean.parseBoolean(completed))
                .put("description", description)
                .put("active", Boolean.parseBoolean(active));
        Response created = api.postRequest("projects", "json", body);
        assertTrue(created.code() == 201 || created.code() == 200, "Project creation failed: " + safeMsg(created));
    }


    @When("I request to add a relationship tasksof between todo {string} and a non existent project with id {string}")
    public void addRelationshipTasksofNonexistentProject(String todoId, String badProjectId) {
        JSONObject body = new JSONObject().put("id", todoId);
        response = api.postRequest("projects/" + badProjectId + "/tasks", "json", body);
        jsonBody = readObj(response);
    }
    @Then("I get an error code {string}")
    public void iGetAnErrorCode(String code) {
        statusCodeString(code);
    }

    // Amend Category

    @Given("a category with title {string}, and description {string}")
    public void aCategoryWithTitleAndDescription(String title, String description) {
        // TODO: Create a category with title, description
        // TODO: Assert that the category is added correctly
        Api call = new Api();
        JSONObject requestBody = new JSONObject();
        requestBody.put("title", title);
        requestBody.put("description", description);
        Response r = call.postRequest("categories", "json", requestBody);
        assertTrue(elementExistsByTitle(Element.CATEGORY, title));
    }
    @When("a user changes the description of category with title {string} to {string} by using the PUT API call")
    public void aUserChangesTheDescriptionOfCategoryWithTitleToByUsingThePUTAPICall(String title, String description) {
        // TODO: Get the id of the category with title
        // TODO: Change description by doing a PUT for that ID
        Api call = new Api();
        JSONObject requestBody = new JSONObject();
        requestBody.put("title", title);
        requestBody.put("description", description);
        String id = getElementIdByTitle(Element.CATEGORY, title);
        Response r = call.putRequest("categories/" + id, "json", requestBody);
    }
    @Then("the category with title {string} should have a description of {string}")
    public void theCategoryWithTitleShouldHaveADescriptionOf(String title, String description) {
        assertTrue(isMatch(Element.CATEGORY, title, "description", description));
    }
    @When("a user changes the description of category with title {string} to {string} by using the POST API call")
    public void aUserChangesTheDescriptionOfCategoryWithTitleToByUsingThePOSTAPICall(String title, String description) {
        // TODO: Get the id of the category with title
        // TODO: Change description by doing a POST for that ID
        Api call = new Api();
        JSONObject requestBody = new JSONObject();
        requestBody.put("title", title);
        requestBody.put("description", description);
        String id = getElementIdByTitle(Element.CATEGORY, title);
        Response r = call.postRequest("categories/" + id, "json", requestBody);
    }
    @When("a user changes the description of category {string}")
    public void aUserChangesTheDescriptionOfCategory(String id) {
        // TODO attempt to change the doneStatus of the todo with id
        Api call = new Api();
        JSONObject requestBody = new JSONObject();
        requestBody.put("description", "new description");
        Response r = call.postRequest("categories/" + id, "json", requestBody);
        response = r;
    }

    @Then("the status code {string} is returned")
    public void theStatusCodeIsReturned(String code) {
        assertEquals(Integer.parseInt(code), response.code());
    }


    // Amend Project

    @Given("a project with title {string}, description {string}, completed status {string}, and active_status {string} exists")
    public void aProjectWithTitleDescriptionCompletedStatusAndActive_statusExists(String title, String description, String completed, String active) {
        Api call = new Api();
        JSONObject requestBody = new JSONObject();
        requestBody.put("title", title);
        requestBody.put("description", description);
        requestBody.put("completed", Boolean.valueOf(completed));
        requestBody.put("active", Boolean.valueOf(active));
        Response r = call.postRequest("projects", "json", requestBody);
        assertTrue(elementExistsByTitle(Element.PROJECT, title));
    }

    @When("a user marks the project {string} as complete by using the PUT API call")
    public void aUserMarksTheProjectAsCompleteByUsingThePUTAPICall(String title) {
        Api call = new Api();
        JSONObject requestBody = new JSONObject();
        requestBody.put("title", title);
        requestBody.put("completed", true);
        String id = getElementIdByTitle(Element.PROJECT, title);
        Response r = call.putRequest("projects/" + id, "json", requestBody);
    }

    @Then("the project with title {string} should have a completed status of {string}")
    public void theProjectWithTitleShouldHaveACompletedStatusOf(String title, String completed) {
        assertTrue(isMatch(Element.PROJECT, title, "completed", completed));
    }

    @When("a user marks the project {string} as complete by using the POST API call")
    public void aUserMarksTheProjectAsCompleteByUsingThePOSTAPICall(String title) {
        Api call = new Api();
        JSONObject requestBody = new JSONObject();
        requestBody.put("title", title);
        requestBody.put("completed", true);
        String id = getElementIdByTitle(Element.PROJECT, title);
        Response r = call.postRequest("projects/" + id, "json", requestBody);
    }

    @When("a user marks the project {string} as complete")
    public void aUserMarksTheProjectAsComplete(String id) {
        Api call = new Api();
        JSONObject requestBody = new JSONObject();
        requestBody.put("completed", true);
        Response r = call.postRequest("projects/" + id, "json", requestBody);
        response = r;
    }

    // Amend Todo

    @Given("a todo with title {string}, description {string} and doneStatus {string} exists")
    public void aTodoWithTitleDescriptionAndDoneStatusExists(String title, String description, String doneStatus) {
        // TODO: Create a todo with title, description and doneStatus
        // TODO: Assert that the todo is added correctly
        Api call = new Api();
        JSONObject requestBody = new JSONObject();
        requestBody.put("title", title);
        requestBody.put("description", description);
        requestBody.put("doneStatus", Boolean.valueOf(doneStatus));
        Response r = call.postRequest("todos", "json", requestBody);
        assertTrue(elementExistsByTitle(Element.TODO, title));
    }

    @When("a user marks the todo with title {string} as done by using the PUT API call")
    public void aUserMarksTheTodoWithTitleAsDoneByUsingThePUTAPICall(String title) {
        // TODO: Get the id of the todo with title
        // TODO: Change doneStatus by doing a put for that ID
        Api call = new Api();
        JSONObject requestBody = new JSONObject();
        requestBody.put("title", title);
        requestBody.put("doneStatus", true);
        String id = getElementIdByTitle(Element.TODO, title);
        Response r = call.putRequest("todos/" + id, "json", requestBody);
    }

    @Then("the todo with title {string} should have a doneStatus of {string}")
    public void theTodoWithTitleShouldHaveADoneStatusOf(String title, String doneStatus) {
        // TODO: Get the id of the todo with title
        // TODO: Get the todo by id
        // TODO: Check that the todo has the right doneStatus
        assertTrue(isMatch(Element.TODO, title, "doneStatus", doneStatus));
    }

    @When("a user marks the todo with title {string} as done by using the POST API call")
    public void aUserMarksTheTodoWithTitleAsDoneByUsingThePOSTAPICall(String title) {
        // Get the id of the todo with title
        // Change doneStatus by doing a put for that ID
        Api call = new Api();
        JSONObject requestBody = new JSONObject();
        requestBody.put("title", title);
        requestBody.put("doneStatus", true);
        String id = getElementIdByTitle(Element.TODO, title);
        Response r = call.postRequest("todos/" + id, "json", requestBody);
    }

    @When("a user marks the todo {string} as done")
    public void aUserMarksTheTodoAsDone(String id) {
        // attempt to change the doneStatus of the todo with id
        Api call = new Api();
        JSONObject requestBody = new JSONObject();
        requestBody.put("doneStatus", true);
        Response r = call.postRequest("todos/" + id, "json", requestBody);
        response = r;
    }


    // == deleteRelationshipTodoProject.feature steps ==

    // Ensure a todo <-> project relationship exists (by titles)
    @Given("there is a tasks relationship between the todo with title {string} and the project with title {string}")
    public void there_is_a_tasks_relationship_between_the_todo_with_title_and_the_project_with_title(String todoTitle, String projectTitle) {
        // Ensure TODO exists
        if (!existsByTitle(Element.TODO, todoTitle)) {
            JSONObject body = new JSONObject().put("title", todoTitle);
            Response created = api.postRequest("todos", "json", body);
            assertTrue(created.code() == 201 || created.code() == 200, "Failed to create todo: " + safeMsg(created));
        }
        // Ensure PROJECT exists
        if (!existsByTitle(Element.PROJECT, projectTitle)) {
            JSONObject body = new JSONObject().put("title", projectTitle);
            Response created = api.postRequest("projects", "json", body);
            assertTrue(created.code() == 201 || created.code() == 200, "Failed to create project: " + safeMsg(created));
        }
        // Link if not already linked
        String todoId = getIdByTitle(Element.TODO, todoTitle);
        String projectId = getIdByTitle(Element.PROJECT, projectTitle);
        Response r = api.getRequest("projects/" + projectId + "/tasks", "json");
        JSONObject o = readObj(r);
        JSONArray arr = o.optJSONArray("todos");
        boolean alreadyLinked = false;
        if (arr != null) {
            for (Object obj : arr) {
                if (todoId.equals(((JSONObject) obj).optString("id"))) { alreadyLinked = true; break; }
            }
        }
        if (!alreadyLinked) {
            JSONObject body = new JSONObject().put("id", todoId);
            Response linked = api.postRequest("projects/" + projectId + "/tasks", "json", body);
            assertTrue(linked.code() == 201 || linked.code() == 200, "Failed to link todo->project: " + safeMsg(linked));
        }
    }

    // Delete relationship using the TODO-side API
    @When("a user deletes the tasks relationship between the todo with title {string} and the project with title {string} with the todo API")
    public void a_user_deletes_the_tasks_relationship_between_the_todo_with_title_and_the_project_with_title_with_the_todo_api(String todoTitle, String projectTitle) {
        String todoId = getIdByTitle(Element.TODO, todoTitle);
        String projectId = getIdByTitle(Element.PROJECT, projectTitle);
        // Typical TODO-side endpoint in TodoManager:
        // DELETE /todos/{todoId}/tasksof/{projectId}
        response = api.deleteRequest("todos/" + todoId + "/tasksof/" + projectId);
    }

    // Delete relationship using the PROJECT-side API
    @When("a user deletes the tasks relationship between the todo with title {string} and the project with title {string} with the project API")
    public void a_user_deletes_the_tasks_relationship_between_the_todo_with_title_and_the_project_with_title_with_the_project_api(String todoTitle, String projectTitle) {
        String todoId = getIdByTitle(Element.TODO, todoTitle);
        String projectId = getIdByTitle(Element.PROJECT, projectTitle);
        // Project-side endpoint:
        // DELETE /projects/{projectId}/tasks/{todoId}
        response = api.deleteRequest("projects/" + projectId + "/tasks/" + todoId);
    }

    // Assert there is no relationship on both sides (project’s tasks and todo’s tasksof)
    @Then("the todo with title {string} and the project with title {string} do not have a relationship")
    public void the_todo_with_title_and_the_project_with_title_do_not_have_a_relationship(String todoTitle, String projectTitle) {
        // Project side must not list the todo
        projectHasNoTaskForTodo(projectTitle, todoTitle);
        String todoId = getIdByTitle(Element.TODO, todoTitle);
        String projectId = getIdByTitle(Element.PROJECT, projectTitle);
        Response r = api.getRequest("todos/" + todoId + "/tasksof", "json");
        JSONObject o = readObj(r);
        JSONArray arr = o.optJSONArray("projects");
        if (arr != null) {
            for (Object obj : arr) {
                assertNotEquals(projectId, ((JSONObject) obj).optString("id"), "Still linked on todo->tasksof");
            }
        }
    }

    // --- Given: create a todo (so we can alias its id in the outline) ---
    @Given("I create a todo with title {string}, description {string}")
    public void createTodoWithTitleAndDescription(String title, String description) {
        org.json.JSONObject body = new org.json.JSONObject()
                .put("title", title)
                .put("description", description);

        Response created = api.postRequest("todos", "application/json", body);
        int code = created.code();
        if (code != 201 && code != 200) {
            throw new AssertionError("Failed to create todo. HTTP " + code + " - " + created.body());
        }
        String realId = extractIdFromResponse(created, "todo");
        if (realId == null || realId.isBlank()) {
            // last resort: list, grab first
            Response list = api.getRequest("todos", "json");
            realId = firstIdFromList(list, "todos");
        }
        if (realId == null || realId.isBlank()) {
            throw new AssertionError("Create succeeded but no todo id could be determined.");
        }
        scenarioState.put("lastCreatedTodoId", realId);
    }

    // --- When: add relationship via categories endpoint ---
    @When("I request to add a relationship todos between categories {string} and todos {string}")
    public void addTodoToCategory(String categoryAlias, String todoAlias) {
        String categoryId = resolveId(Element.CATEGORY, categoryAlias);
        String todoId     = tryResolveOrUseLastCreated(Element.TODO, todoAlias);

        org.json.JSONObject body = new org.json.JSONObject().put("id", todoId);
        // Typical TodoManager semantics: POST /categories/{id}/todos with {"id": "<todoId>"}
        response = api.postRequest("categories/" + categoryId + "/todos", "application/json", body);
    }

    // --- When: error path with non-existent todo id ---
    @When("I request to add a relationship todos between category {string} and a non existent todo with id {string}")
    public void addNonexistentTodoToCategory(String categoryAlias, String missingTodoId) {
        String categoryId = resolveId(Element.CATEGORY, categoryAlias);

        org.json.JSONObject body = new org.json.JSONObject().put("id", missingTodoId);
        response = api.postRequest("categories/" + categoryId + "/todos", "application/json", body);
    }

    // --- Then: verify relationship exists ---
    @Then("the relationship between category {string} todo {string} should be created")
    public void relationshipBetweenCategoryAndTodoCreated(String categoryAlias, String todoAlias) {
        String categoryId = resolveId(Element.CATEGORY, categoryAlias);
        String todoId     = tryResolveOrUseLastCreated(Element.TODO, todoAlias);

        // Many backends return 201 on creation; accept both 200/201
        int c = response.code();
        Assertions.assertTrue(c == 200 || c == 201,
                "Expected 200/201 from relationship create but got " + c + " - " + response.body());

        // Verify with a GET that the todo appears under the category
        Response rels = api.getRequest("categories/" + categoryId + "/todos", "json");
        assertStatus(200, rels);

        String body = String.valueOf(rels.body());
        org.json.JSONArray arr = new org.json.JSONObject(body).optJSONArray("todos");
        boolean found = false;
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                String id = arr.getJSONObject(i).optString("id", "");
                if (todoId.equals(id)) { found = true; break; }
            }
        }
        Assertions.assertTrue(found, "Relationship not present between category " + categoryId + " and todo " + todoId);
    }

    // --- Given: ensure a category exists with ID ---
    @Given("I have a category with ID {string}")
    public void haveCategoryId(String idGiven) {
        // Try real id first
        Response r = api.getRequest("categories/" + idGiven, "json");
        if (r.code() != 200) {
            // Seed a category, then alias the feature's id -> real id
            JSONObject body = new JSONObject().put("title", "seed-category-" + idGiven);
            Response created = api.postRequest("categories", "json", body);
            assertTrue(created.code() == 201 || created.code() == 200, "Category seed failed: " + safeMsg(created));

            Response list = api.getRequest("categories", "json");
            JSONObject obj = readObj(list);
            JSONArray arr = getGroupArray(obj, "categories");
            assertTrue(arr.length() > 0, "Seeding category failed");
            String realId = arr.getJSONObject(arr.length() - 1).optString("id");
            assertFalse(realId.isEmpty(), "Seeded category has no id");
            aliasId(Element.CATEGORY, idGiven, realId);
        }
        // Final sanity check on resolved id
        assertStatus(200, api.getRequest("categories/" + resolveId(Element.CATEGORY, idGiven), "json"));
    }

    // --- Given: create a category with title and description ---
    @Given("I create a category with title {string}, description {string}")
    public void createCategoryWithTitleAndDescription(String title, String description) {
        JSONObject body = new JSONObject()
                .put("title", title)
                .put("description", description);

        Response created = api.postRequest("categories", "json", body);
        int code = created.code();
        if (code != 201 && code != 200) {
            throw new AssertionError("Failed to create category. HTTP " + code + " - " + bodyString(created));
        }

        // Extract the real ID from the response
        String realId = extractIdFromResponse(created, "category");
        if (realId == null || realId.isBlank()) {
            Response list = api.getRequest("categories", "json");
            realId = firstIdFromList(list, "categories");
        }
        if (realId == null || realId.isBlank()) {
            throw new AssertionError("Create succeeded but no category id could be determined.");
        }
        scenarioState.put("lastCreatedCategoryId", realId);
    }

    // --- Helper method to resolve category ID (similar to todo helper) ---
    private String tryResolveOrUseLastCreatedCategory(Element e, String alias) {
        // If we just created a category in this scenario, use it regardless of the alias
        if (scenarioState.containsKey("lastCreatedCategoryId")) {
            String real = (String) scenarioState.get("lastCreatedCategoryId");
            // Bind the alias to this real ID for future lookups
            if (alias != null && !alias.isBlank()) {
                aliasId(Element.CATEGORY, alias, real);
            }
            return real;
        }

        // Otherwise, try to resolve normally
        try {
            return resolveId(e, alias);
        } catch (Exception ex) {
            // Just return the alias (backend will 404 if it doesn't exist)
            return alias;
        }
    }

    // --- When: add relationship via project's categories endpoint ---
    @When("I request to add a relationship categories between project {string} and categories {string}")
    public void addRelationshipCategoriesBetweenProjectAndCategories(String projectAlias, String categoryAlias) {
        String projectId = resolveId(Element.PROJECT, projectAlias);
        String categoryId = tryResolveOrUseLastCreatedCategory(Element.CATEGORY, categoryAlias);

        JSONObject body = new JSONObject().put("id", categoryId);
        // POST /projects/{projectId}/categories with {"id": "<categoryId>"}
        response = api.postRequest("projects/" + projectId + "/categories", "json", body);
        jsonBody = readObj(response);
    }

    // --- When: error path with non-existent category id ---
    @When("I request to add a relationship categories between project {string} and a non existent category with id {string}")
    public void addRelationshipCategoriesBetweenProjectAndNonExistentCategory(String projectAlias, String missingCategoryId) {
        String projectId = resolveId(Element.PROJECT, projectAlias);

        JSONObject body = new JSONObject().put("id", missingCategoryId);
        response = api.postRequest("projects/" + projectId + "/categories", "json", body);
        jsonBody = readObj(response);
    }

    // --- Then: verify relationship exists ---
    @Then("the relationship between project {string} category {string} should be created")
    public void relationshipBetweenProjectAndCategoryCreated(String projectAlias, String categoryAlias) {
        String projectId = resolveId(Element.PROJECT, projectAlias);
        String categoryId = tryResolveOrUseLastCreatedCategory(Element.CATEGORY, categoryAlias);

        // Many backends return 201 on creation; accept both 200/201
        int c = response.code();
        assertTrue(c == 200 || c == 201,
                "Expected 200/201 from relationship create but got " + c);

        // Verify with a GET that the category appears under the project
        Response rels = api.getRequest("projects/" + projectId + "/categories", "json");
        assertStatus(200, rels);

        JSONObject o = readObj(rels);
        JSONArray arr = o.optJSONArray("categories");
        assertNotNull(arr, "No categories array on /projects/{id}/categories");

        boolean found = false;
        for (Object obj : arr) {
            String id = ((JSONObject) obj).optString("id", "");
            if (categoryId.equals(id)) {
                found = true;
                break;
            }
        }
        assertTrue(found, "Relationship not present between project " + projectId + " and category " + categoryId);
    }

}
