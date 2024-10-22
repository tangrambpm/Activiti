/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.rest.service.api.legacy;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.activiti.engine.ActivitiIllegalArgumentException;
import org.activiti.engine.TaskService;
import org.activiti.engine.identity.Group;
import org.activiti.engine.identity.GroupQuery;
import org.activiti.engine.task.TaskQuery;
import org.activiti.rest.common.api.ActivitiUtil;
import org.activiti.rest.common.api.SecuredResource;
import org.restlet.data.Form;
import org.restlet.resource.Get;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Tijs Rademakers
 */
public class TasksSummaryResource extends SecuredResource {
  
  @Get
  public ObjectNode getTasksSummary() {
    if(authenticate() == false) return null;
    
    String user = getQuery().getValues("user");
    if(user == null) {
      throw new ActivitiIllegalArgumentException("No user provided");
    }
    
    List<String> processDefKeys = null;
    TaskService ts = ActivitiUtil.getTaskService();
    Form formQuery = getQuery();
    Set<String> names = formQuery.getNames();

    GroupQuery query = ActivitiUtil.getIdentityService()
      .createGroupQuery()
      .groupMember(user)
      .groupType("assignment");

    if(names.contains("processDefinitionKeyIn")) {
      String[] processDefinitionKeys = getQueryParameter("processDefinitionKeyIn", formQuery).split(",");
      processDefKeys = new ArrayList<String>(processDefinitionKeys.length);
      for (String key : processDefinitionKeys) {
        processDefKeys.add(key);
      }
    }
    
    List<Group> groups = query.list();
    ObjectNode groupsJSON = new ObjectMapper().createObjectNode();
    for (Group group : groups) {
      TaskQuery taskQuery = ts.createTaskQuery();
      if(names.contains("processDefinitionKeyIn")) {
        taskQuery.processDefinitionKeyIn(processDefKeys);
      }
      taskQuery.active(); // only count not suspended tasks
      long tasksInGroup = taskQuery.taskCandidateGroup(group.getId()).count();
      groupsJSON.put(group.getName(), tasksInGroup);
    }
    
    ObjectNode summaryResponseJSON = new ObjectMapper().createObjectNode();
    
    ObjectNode totalAssignedJSON = new ObjectMapper().createObjectNode();
    totalAssignedJSON.put("total", ts.createTaskQuery().taskAssignee(user).active().count());
    summaryResponseJSON.put("assigned", totalAssignedJSON);
    
    ObjectNode totalUnassignedJSON = new ObjectMapper().createObjectNode();
    totalUnassignedJSON.put("total", ts.createTaskQuery().taskCandidateUser(user).count());
    totalUnassignedJSON.put("groups", groupsJSON);
    summaryResponseJSON.put("unassigned", totalUnassignedJSON);
    
    return summaryResponseJSON;
  }

}